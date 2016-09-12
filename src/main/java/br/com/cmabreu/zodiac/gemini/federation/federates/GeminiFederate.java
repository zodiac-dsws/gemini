package br.com.cmabreu.zodiac.gemini.federation.federates;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import br.com.cmabreu.zodiac.gemini.core.FragmentInstancer;
import br.com.cmabreu.zodiac.gemini.core.Logger;
import br.com.cmabreu.zodiac.gemini.entity.Experiment;
import br.com.cmabreu.zodiac.gemini.exceptions.NotFoundException;
import br.com.cmabreu.zodiac.gemini.federation.Environment;
import br.com.cmabreu.zodiac.gemini.federation.RTIAmbassadorProvider;
import br.com.cmabreu.zodiac.gemini.federation.classes.CoreClass;
import br.com.cmabreu.zodiac.gemini.federation.classes.ExperimentStartedInteractionClass;
import br.com.cmabreu.zodiac.gemini.federation.classes.GeminiClass;
import br.com.cmabreu.zodiac.gemini.federation.classes.GenerateInstancesInteractionClass;
import br.com.cmabreu.zodiac.gemini.misc.PathFinder;
import br.com.cmabreu.zodiac.gemini.services.ExperimentService;
import br.com.cmabreu.zodiac.gemini.services.FragmentService;
import hla.rti1516e.AttributeHandleValueMap;
import hla.rti1516e.InteractionClassHandle;
import hla.rti1516e.ObjectInstanceHandle;
import hla.rti1516e.ParameterHandleValueMap;
import hla.rti1516e.RTIambassador;
import hla.rti1516e.ResignAction;
import hla.rti1516e.exceptions.FederatesCurrentlyJoined;
import hla.rti1516e.exceptions.FederationExecutionAlreadyExists;
import hla.rti1516e.exceptions.FederationExecutionDoesNotExist;

public class GeminiFederate {
	private static GeminiFederate instance;
	private String rootPath;
	private GeminiClass geminiClass;
	private GenerateInstancesInteractionClass generateInstancesInteractionClass;
	private ExperimentStartedInteractionClass experimentStartedInteractionClass;
	private CoreClass coreClass;

	public static GeminiFederate getInstance() throws Exception {
		if ( instance == null ) {
			instance = new GeminiFederate();
		}
		return instance;
	}
	
	public Experiment startExperiment( int idExperiment ) throws Exception {
		debug("Starting Experiment ID " + idExperiment + "...");
		Experiment experiment = new ExperimentService().runExperiment( idExperiment );
		debug("Experiment " + experiment.getTagExec() + " is now running.");
		
		// Notify the Federation
		experimentStartedInteractionClass.send( experiment.getTagExec() );
		
		return experiment;
	}	
	
	public void finishFederationExecution() throws Exception {
		debug( "Will try to finish Federation execution" );
		RTIambassador rtiamb = RTIAmbassadorProvider.getInstance().getRTIAmbassador();

		rtiamb.resignFederationExecution( ResignAction.DELETE_OBJECTS );
		try	{
			rtiamb.destroyFederationExecution( "Zodiac" );
			debug( "Destroyed Federation" );
		} catch( FederationExecutionDoesNotExist dne ) {
			debug( "No need to destroy federation, it doesn't exist" );
		} catch( FederatesCurrentlyJoined fcj ){
			debug( "Didn't destroy federation, federates still joined" );
		}		
	}
	
	private GeminiFederate( ) throws Exception {
		rootPath = PathFinder.getInstance().getPath();
	}
	
	private void startFederate() {
		debug("Starting Zodiac Gemini Instance Generator");
		try {
			Map<String, String> newenv = new HashMap<String, String>();
			newenv.put("RTI_HOME", "");
			
			Environment.setEnv( newenv );
			
			RTIambassador rtiamb = RTIAmbassadorProvider.getInstance().getRTIAmbassador();
			
			try	{
				URL[] modules = new URL[]{
					(new File( rootPath + "/foms/HLAstandardMIM.xml" ) ).toURI().toURL()
				};
				rtiamb.createFederationExecution("Zodiac", modules );
			} catch( FederationExecutionAlreadyExists exists ) {
				debug("Federation already exists. Bypassing...");
			}
			
			try {
				join();
			} catch ( Exception e ) {
				error("Error when joing the Federation: " + e.getMessage() );
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}

	
	public ObjectInstanceHandle getGeminiObjectHandle() {
		return getGeminiClass().getGeminiObjectHandle();
	}
	
	public void reflectAttributeUpdate( ObjectInstanceHandle theObject,  AttributeHandleValueMap theAttributes ) {
		//
	}
	

	public boolean isGenerateInstanceInteraction( InteractionClassHandle interactionClassHandle ) {
		return generateInstancesInteractionClass.getInteractionClassHandle().equals( interactionClassHandle ); 
	}
	
	public void startServer() throws Exception {
		startFederate();
		if ( geminiClass == null ) {
			geminiClass = new GeminiClass();
			geminiClass.publish();
			geminiClass.subscribe(); // Will help redundancy.
			geminiClass.createNew();

			coreClass = new CoreClass();
			coreClass.subscribe();		
			
			// Listen to generate instances commands from Sagittarius
			generateInstancesInteractionClass = new GenerateInstancesInteractionClass();
			generateInstancesInteractionClass.subscribe();	
			
			// Notify Federation when a Experiment is running
			experimentStartedInteractionClass = new ExperimentStartedInteractionClass();
			experimentStartedInteractionClass.publish();
			
			debug("done.");
			
		} else {
			warn("server is already running an instance");
		}
	}
	
	public GeminiClass getGeminiClass() {
		return geminiClass;
	}
	
	private void join() throws Exception {
		RTIambassador rtiamb = RTIAmbassadorProvider.getInstance().getRTIAmbassador();

		debug("joing Federation Execution ...");
		URL[] joinModules = new URL[]{
			(new File(rootPath +  "/foms/zodiac.xml")).toURI().toURL(),	
			(new File(rootPath +  "/foms/sagittarius.xml")).toURI().toURL(),
			(new File(rootPath +  "/foms/scorpio.xml")).toURI().toURL(),
			(new File(rootPath +  "/foms/core.xml")).toURI().toURL(),
		    (new File(rootPath +  "/foms/gemini.xml")).toURI().toURL()
		};
		rtiamb.joinFederationExecution( "Gemini", "GeminiType", "Zodiac", joinModules );           
	}
	
	private void debug( String s ) {
		Logger.getInstance().debug(this.getClass().getName(), s );
	}	

	private void warn( String s ) {
		Logger.getInstance().warn(this.getClass().getName(), s );
	}	

	private void error( String s ) {
		Logger.getInstance().error(this.getClass().getName(), s );
	}

	// *************************************************************
	// *** TODO: MUST BE THREADED OR WILL BLOCK THE FEDERATE!!!! ***
	// *************************************************************
	public void generateInstances(ParameterHandleValueMap theParameters) {
		String experimentSerial = generateInstancesInteractionClass.getExperimentSerial( theParameters );
		debug("Generate instances for experiment " + experimentSerial );

		try {
			
			ExperimentService es = new ExperimentService();
			Experiment exp = es.getExperiment(experimentSerial);

			try {
				FragmentService fs = new FragmentService();
				exp.setFragments( fs.getList( exp.getIdExperiment() ) );
			
				debug("Experiment " + exp.getTagExec() + " found.");
				
				try {
					FragmentInstancer fp = new FragmentInstancer( exp );
					fp.generate();
					int pips = fp.getInstances().size();
					if ( pips == 0) {
						debug("experiment " + experimentSerial + " generate empty instance list. Will finish it" );
					} else {
						debug("done generating " + pips + "instances for Experiment " + experimentSerial );
					}
					
				} catch (Exception e) {
					error("cannot generate instances for experiment " + exp.getTagExec() + ": " + e.getMessage() );
				}
			
			} catch ( NotFoundException nfe ) {
				error("Experiment " + experimentSerial + " have no Fragments.");
			}
			
		} catch ( NotFoundException e) {
			error("Experiment " + experimentSerial + " not found.");
		} catch ( Exception e ) {
			error("Error generating instances fot Experiment " + experimentSerial );
		}
		
		/*
			select exp.id_experiment as experiment, act.executoralias, fr.id_fragment as fragment, fr.status as fragstatus, ins.serial as instance, ins.type from instances ins 
				join fragments fr on fr.id_fragment = ins.id_fragment
				join activities act on act.id_fragment = fr.id_fragment
				join experiments exp on exp.id_experiment = fr.id_experiment
			order by fr.serial
			
			update fragments set status = 'RUNNING' where id_fragment = 3154
			update instances set status = 'PIPELINED' where id_fragment = 3153
			update experiments set status = 'RUNNING' where id_experiment = 355
			select * from instances where status = 'PIPELINED'
			
			select distinct (status), count(status) from instances group by status order by status
			select id_fragment, status, count(status) from instances group by id_fragment,status order by status	
			
		*/
	}

	
}
