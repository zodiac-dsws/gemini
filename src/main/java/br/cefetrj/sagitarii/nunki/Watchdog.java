package br.cefetrj.sagitarii.nunki;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import br.cefetrj.sagitarii.nunki.comm.Communicator;

public class Watchdog {
	private Logger logger = LogManager.getLogger( this.getClass().getName() ); 
	private List<String> processes = new ArrayList<String>();
	private Communicator communicator;
	private Configurator configurator;
	
	public Watchdog(Communicator communicator, Configurator configurator ) {
		this.communicator = communicator;
		this.configurator = configurator;
	}
	
	private Double getCpuFromProcess( String process ) {
		//PID USER PR NI VIRT RES SHR S %CPU %MEM TIME+ COMMAND
		Double result = 0.0;
		String[] items = process.replace("  "," ").replace("  "," ").split(" ");
		result = Double.valueOf( items[8].replace(",", ".") );
		return result;
	}
	
	private String getByPid( int pid ) {
		for ( String line : processes ) {
			if ( line.startsWith( String.valueOf( pid ) ) ) {
				return line;
			}
		}
		return "";
	}
	
	private void getRunningProcesses() throws Exception {
		Process process = Runtime.getRuntime().exec("top -b -n1 -c");
		InputStream is = process.getInputStream();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		String line;
		while ( (line = br.readLine()) != null) {
			if( line.contains("java") ) processes.add( line );	
		}	
	}

	private void kill( int pid ) throws Exception {
		// Don't do this for now. Will improve later...
		// Runtime.getRuntime().exec("kill -9 " + pid);
	}
	
	
	public void protect( List<TaskRunner> runners, Double cpuLoad ) {
		
		if ( cpuLoad < 99 ) return;  
		logger.debug("CPU load too high (" + cpuLoad + "%). Will check this...");
		
		if( runners.size() == 0 ) {
			logger.debug("is not our fault. No Teapot tasks running.");
			return;
		}
		
		try { 
			getRunningProcesses();
			logger.debug("found " + processes.size() + " java processes.");
			for ( TaskRunner tr : runners ) {
				Task task = tr.getCurrentTask();
				if ( task != null ) {
					String line = getByPid( task.getPID() );
					if ( !line.equals("") ) {
						Double cpu = getCpuFromProcess( line );
						logger.debug("found task pid " + task.getPID() + ": " + cpu + "%" );
						if ( cpu >= 99 ) {
							logger.debug(" > You bastard! I'll kill you...");
							kill( task.getPID() );
						}
					}
				}
			}
			
		} catch ( Exception e ) {
			logger.error( e.getMessage() );
		}
		processes.clear();
	}

}
