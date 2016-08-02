package br.cefetrj.sagitarii.nunki;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import br.cefetrj.sagitarii.nunki.comm.Communicator;
import br.cefetrj.sagitarii.nunki.comm.Uploader;
import br.cefetrj.sagitarii.nunki.Activation;

public class TaskManager {
	private SystemProperties tm;
	private Communicator comm;
	private Configurator configurator;
	private XMLParser parser;
	private List<Activation> executionQueue;
	private List<Activation> jobPool;
	private Logger logger = LogManager.getLogger( this.getClass().getName() ); 
	private List<Task> tasks = new ArrayList<Task>();
	private Task currentTask = null;
	private Activation currentActivation;
	private List<String> execLog = new ArrayList<String>();
	
	private void debug( String s ) {
		if ( !s.equals("")) {
			logger.debug( s );
			execLog.add( s );
			notifySagitarii( s );
		}
	}
	
	private void error( String s ) {
		logger.error( s );
		execLog.add( "[ERROR] " +  s );
		notifySagitarii( s );
	}
	
	public Task getCurrentTask() {
		return currentTask;
	}
	
	public Activation getCurrentActivation() {
		return this.currentActivation;
	}

	public List<Activation> getJobPool() {
		return new ArrayList<Activation>( jobPool );
	}
	
	public List<Task> getTasks() {
		return new ArrayList<Task>( tasks );
	}
	
	public TaskManager(Communicator comm, Configurator gf) {
		this.tm = gf.getSystemProperties();
		this.comm = comm;
		this.configurator = gf;
		this.parser = new XMLParser();
		this.executionQueue = new ArrayList<Activation>();
		this.jobPool = new ArrayList<Activation>();
	}
	

	public void notifySagitarii( String message ) {
		Notifier.getInstance(comm, configurator).notifySagitarii( message, currentActivation);
	}
	
	private String generateCommand( Activation activation ) {
		return activation.getCommand();
	}
	
	/**
	 * Run a wrapper task
	 * BLOCKING
	 */
	private void runTask( Activation activation ) {
		String instanceId = activation.getInstanceSerial();
		int order = activation.getOrder();
		debug("starting executor " + activation.getExecutor() );

		debug("start task " + activation.getTaskId() + "(" + activation.getType() + ") " + activation.getExecutor() + " ("+ instanceId + " :: " + order + ")");
        
		activation.setStatus( TaskStatus.RUNNING );
		
		Task task = new Task( activation, execLog, this );
		task.setSourceData( activation.getSourceData() );
		task.setRealStartTime( Calendar.getInstance().getTime() );
		
		try {
			comm.send("activityManagerReceiver", "instanceId=" + activation.getInstanceSerial() + "&response=RUNNING&node=" + tm.getMacAddress() + "&executor" + activation.getExecutor() );
	        tasks.add(task);
	        currentTask = task;
	        
	        // Will Block Until Finished ...
	        task.run( configurator );
	        
	        // When finished...
	        finishAndClose( task );
	        
		} catch ( Exception e ) {
			error("Sagitarii not received task RUNNING response. Maybe offline.");
		}
	}
	
	/**
	* Implementation of ITaskObserver.notify()
	*/
	public synchronized void finishAndClose( Task task ) {
		debug("task " + task.getTaskId() + "("+ currentTask.getActivation().getExecutor() + ") finished. (" + task.getExitCode() + ")" );
		try {
			Activation act = task.getActivation();
			act.setStatus( TaskStatus.FINISHED );
			task.setRealFinishTime( Calendar.getInstance().getTime() );
			
			try {
				new Uploader(configurator).uploadCSV("sagi_output.txt", act.getTargetTable(), act.getExperiment(), 
						act.getNamespace(), task, tm );
			} catch ( Exception e) {
				error("Error sending files to Sagitarii: " + e.getMessage() );
				e.printStackTrace();
			}			
			
			debug("all done! leaving execution thread.");
		} catch ( Exception e ) {
			error("error finishing task " + task.getApplicationName() + " at " + currentTask.getActivation().getNamespace() + " : " + e.getMessage() );
		}
		
	}
	
	public void process( String hexResp ) throws Exception {
		String instanceSerial = "";
		try {
			
			byte[] compressedResp = ZipUtil.toByteArray( hexResp );
			String response = ZipUtil.decompress(compressedResp);

			List<Activation> acts = parser.parseActivations( response );
			executionQueue.addAll( acts );
			jobPool.addAll( acts );
			debug("starting instance with " + acts.size() + " activities.");
			boolean found = false;
			for ( Activation act : acts ) {
				if( act.getOrder() == 0 ) {
					currentActivation = act;
					found = true;
					debug("execute first task in instance " + act.getInstanceSerial() );
					instanceSerial = act.getInstanceSerial();
					executionQueue.remove(act);
				
					String newCommand = generateCommand( act );
					act.setCommand( newCommand );
					createWorkFolder(act);
					saveXmlData(act);
					runTask( act );
					break;
				}
			}
			if ( !found ) {
				error("no activities found in instance ");
			}
		} catch (Exception e) {
			error( "error starting process: " + e.getMessage() );
			comm.send("activityManagerReceiver", "instanceId=" + instanceSerial + "&response=CANNOT_EXEC&node=" + tm.getMacAddress() );
		}
	}
	
	private void saveXmlData( Activation act ) throws Exception {
		FileWriter writer = new FileWriter( act.getNamespace() + "/" + "sagi_source_data.xml"); 
		String xml = act.getXmlOriginalData();
		xml = xml.replaceAll("><", ">\n<");
		writer.write( xml );
		writer.close();		
		debug("XML source data file saved");
	}	

	private void createWorkFolder( Activation act ) throws Exception {
		File outputFolder = new File( act.getNamespace() + "/" + "outbox" );
		outputFolder.mkdirs();

		File inputFolder = new File( act.getNamespace() + "/" + "inbox" );
		inputFolder.mkdirs();
	}	
}
