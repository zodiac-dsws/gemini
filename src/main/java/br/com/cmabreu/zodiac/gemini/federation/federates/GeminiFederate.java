package br.com.cmabreu.zodiac.gemini.federation.federates;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.com.cmabreu.zodiac.gemini.core.Logger;
import br.com.cmabreu.zodiac.gemini.federation.Environment;
import br.com.cmabreu.zodiac.gemini.federation.RTIAmbassadorProvider;
import br.com.cmabreu.zodiac.gemini.federation.classes.CoreClass;
import br.com.cmabreu.zodiac.gemini.federation.classes.ExperimentFinishedInteractionClass;
import br.com.cmabreu.zodiac.gemini.federation.classes.ExperimentStartedInteractionClass;
import br.com.cmabreu.zodiac.gemini.federation.classes.GeminiClass;
import br.com.cmabreu.zodiac.gemini.federation.classes.GenerateInstancesInteractionClass;
import br.com.cmabreu.zodiac.gemini.federation.classes.InstanceCreationErrorInteractionClass;
import br.com.cmabreu.zodiac.gemini.federation.classes.InstancesCreatedInteractionClass;
import br.com.cmabreu.zodiac.gemini.misc.PathFinder;
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
	private ExperimentFinishedInteractionClass experimentFinishedInteractionClass;
	private InstancesCreatedInteractionClass instancesCreatedInteractionClass;
	private InstanceCreationErrorInteractionClass instanceCreationErrorInteractionClass;
	private List<String> processingExperiments;
	private CoreClass coreClass;
	
	public static GeminiFederate getInstance() throws Exception {
		if ( instance == null ) {
			instance = new GeminiFederate();
		}
		return instance;
	}

	// *************************************************************
	// ***    MUST BE THREADED OR WILL BLOCK THE FEDERATE!!!!    ***
	// *************************************************************
	public void startExperiment( int idExperiment ) throws Exception {
		StartExperimentThread set = new StartExperimentThread(idExperiment);
		set.run();
	}
	
	// *************************************************************
	// ***    MUST BE THREADED OR WILL BLOCK THE FEDERATE!!!!    ***
	// *************************************************************
	public void generateInstances(ParameterHandleValueMap theParameters) throws Exception {
		String experimentSerial = generateInstancesInteractionClass.getExperimentSerial( theParameters );
		if ( processingExperiments.contains( experimentSerial ) ) {
			warn("Already generating Instances for Experiment " + experimentSerial);
			return;
		}
		processingExperiments.add( experimentSerial );
		debug("Will generate Instances for Experiment " + experimentSerial + ". Processing " + processingExperiments.size() + " Experiments." );
		InstanceGeneratorThread igt = new InstanceGeneratorThread( experimentSerial );
		igt.run();
	}
	
	public synchronized void removeProcessingExperiments( String experimentSerial ) {
		processingExperiments.remove( experimentSerial );
		/*
		for ( String s : processingExperiments ) {
			if ( s.equals( experimentSerial ) ) {
				processingExperiments.remove(s);
				break;
			}
		}
		*/
	}
	
	public ExperimentStartedInteractionClass getExperimentStartedInteractionClass() {
		return experimentStartedInteractionClass;
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
		processingExperiments = new ArrayList<String>(); 
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

			// Notify Federation when a Experiment is finished.
			experimentFinishedInteractionClass = new ExperimentFinishedInteractionClass();
			experimentFinishedInteractionClass.publish();
			
			// Notify Federation when create instances
			instancesCreatedInteractionClass = new InstancesCreatedInteractionClass();
			instancesCreatedInteractionClass.publish();
			
			// Notify when fail to create instances
			instanceCreationErrorInteractionClass = new InstanceCreationErrorInteractionClass();
			instanceCreationErrorInteractionClass.publish();

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
	
	public GenerateInstancesInteractionClass getGenerateInstancesInteractionClass() {
		return generateInstancesInteractionClass;
	}
	
	public ExperimentFinishedInteractionClass getExperimentFinishedInteractionClass() {
		return experimentFinishedInteractionClass;
	}
	
	public InstancesCreatedInteractionClass getInstancesCreatedInteractionClass() {
		return instancesCreatedInteractionClass;
	}

	public InstanceCreationErrorInteractionClass getInstanceCreationErrorInteractionClass() {
		return instanceCreationErrorInteractionClass;
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

	public boolean isExperimentFinishedInteraction(InteractionClassHandle interactionClassHandle) {
		return experimentFinishedInteractionClass.getInteractionClassHandle().equals( interactionClassHandle );
	}


}
