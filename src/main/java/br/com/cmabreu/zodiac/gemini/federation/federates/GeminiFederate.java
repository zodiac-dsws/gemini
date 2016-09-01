package br.com.cmabreu.zodiac.gemini.federation.federates;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import br.com.cmabreu.zodiac.gemini.core.Logger;
import br.com.cmabreu.zodiac.gemini.federation.Environment;
import br.com.cmabreu.zodiac.gemini.federation.RTIAmbassadorProvider;
import br.com.cmabreu.zodiac.gemini.federation.classes.CoreClass;
import br.com.cmabreu.zodiac.gemini.federation.classes.GeminiClass;
import br.com.cmabreu.zodiac.gemini.federation.classes.GenerateInstancesInteractionClass;
import br.com.cmabreu.zodiac.gemini.misc.PathFinder;
import hla.rti1516e.AttributeHandleValueMap;
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
	private CoreClass coreClass;

	public static GeminiFederate getInstance() throws Exception {
		if ( instance == null ) {
			instance = new GeminiFederate();
		}
		return instance;
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

	public void generateInstances(ParameterHandleValueMap theParameters) {
		String experimentSerial = generateInstancesInteractionClass.getExperimentSerial( theParameters );
		debug("Federate: Generate instances to experiment " + experimentSerial );
		
		/*
		try {
			FragmentInstancer fp = new FragmentInstancer( exp );
			fp.generate();
			int pips = fp.getInstances().size();
			if ( pips == 0) {
				logger.debug("experiment " + exp.getTagExec() + " generate empty instance list. Will finish it" );
				haveReady = false;
			} 
		} catch (Exception e) {
			logger.error("cannot generate instances for experiment " + exp.getTagExec() + ": " + e.getMessage() );
			haveReady = false;
		}
		logger.debug("done generating instances (" + exp.getTagExec() + ")");
		*/		
		
	}

	
}
