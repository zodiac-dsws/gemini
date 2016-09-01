package br.com.cmabreu.zodiac.gemini.federation.classes;


import br.com.cmabreu.zodiac.gemini.federation.EncoderDecoder;
import br.com.cmabreu.zodiac.gemini.federation.RTIAmbassadorProvider;
import hla.rti1516e.InteractionClassHandle;
import hla.rti1516e.ParameterHandle;
import hla.rti1516e.ParameterHandleValueMap;
import hla.rti1516e.RTIambassador;
import hla.rti1516e.encoding.HLAunicodeString;

public class GenerateInstancesInteractionClass {
	private InteractionClassHandle generateInstancesInteractionHandle;
	private ParameterHandle experimentSerialParameterHandle;
	private EncoderDecoder encodec;
	
	private RTIambassador rtiamb;

	public GenerateInstancesInteractionClass() throws Exception {
		rtiamb = RTIAmbassadorProvider.getInstance().getRTIAmbassador();
		generateInstancesInteractionHandle = rtiamb.getInteractionClassHandle( "HLAinteractionRoot.GenerateInstances" );
		experimentSerialParameterHandle = rtiamb.getParameterHandle( generateInstancesInteractionHandle, "ExperimentSerial" );
		encodec = new EncoderDecoder();
	}
	
	public String getExperimentSerial( ParameterHandleValueMap parameters ) {
		String experimentSerial = encodec.toString( parameters.get( experimentSerialParameterHandle ) );
		return experimentSerial;
	}	
	
	public void send( String experimentSerial ) throws Exception {
		HLAunicodeString experimentSerialValue = encodec.createHLAunicodeString( experimentSerial );
		
		ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(2);
		parameters.put( experimentSerialParameterHandle, experimentSerialValue.toByteArray() );
		
		rtiamb.sendInteraction( generateInstancesInteractionHandle, parameters, "Generate Instances".getBytes() );		
	}

	public void publish() throws Exception {
		rtiamb.publishInteractionClass( generateInstancesInteractionHandle );
	}
	
	public void subscribe() throws Exception {
		rtiamb.subscribeInteractionClass( generateInstancesInteractionHandle );		
	}
	
}
