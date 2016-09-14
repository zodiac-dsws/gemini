package br.com.cmabreu.zodiac.gemini.federation.classes;


import br.com.cmabreu.zodiac.gemini.core.Logger;
import br.com.cmabreu.zodiac.gemini.federation.EncoderDecoder;
import br.com.cmabreu.zodiac.gemini.federation.RTIAmbassadorProvider;
import hla.rti1516e.InteractionClassHandle;
import hla.rti1516e.ParameterHandle;
import hla.rti1516e.ParameterHandleValueMap;
import hla.rti1516e.RTIambassador;
import hla.rti1516e.encoding.HLAunicodeString;

public class InstanceCreationErrorInteractionClass {
	private InteractionClassHandle instanceCreationErrorInteractionHandle;
	private ParameterHandle experimentSerialParameterHandle;
	private ParameterHandle reasonParameterHandle;
	private EncoderDecoder encodec;
	
	private RTIambassador rtiamb;

	public InteractionClassHandle getInteractionClassHandle() {
		return instanceCreationErrorInteractionHandle;
	}
	
	public InstanceCreationErrorInteractionClass() throws Exception {
		rtiamb = RTIAmbassadorProvider.getInstance().getRTIAmbassador();
		instanceCreationErrorInteractionHandle = rtiamb.getInteractionClassHandle( "HLAinteractionRoot.InstanceCreationError" );
		experimentSerialParameterHandle = rtiamb.getParameterHandle( instanceCreationErrorInteractionHandle, "ExperimentSerial" );
		reasonParameterHandle = rtiamb.getParameterHandle( instanceCreationErrorInteractionHandle, "Reason" );
		encodec = new EncoderDecoder();
	}
	
	public String getExperimentSerial( ParameterHandleValueMap parameters ) {
		String experimentSerial = encodec.toString( parameters.get( experimentSerialParameterHandle ) );
		return experimentSerial;
	}
	
	public String getReason( ParameterHandleValueMap parameters ) {
		String reason = encodec.toString( parameters.get( reasonParameterHandle ) );
		return reason;
	}	
	
	public void send( String experimentSerial, String reason ) {
		try {
			HLAunicodeString experimentSerialValue = encodec.createHLAunicodeString( experimentSerial );
			HLAunicodeString reasonValue = encodec.createHLAunicodeString( reason );
			
			ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(2);
			parameters.put( experimentSerialParameterHandle, experimentSerialValue.toByteArray() );
			parameters.put( reasonParameterHandle, reasonValue.toByteArray() );
			
			rtiamb.sendInteraction( instanceCreationErrorInteractionHandle, parameters, "Error Creating Instances".getBytes() );
		} catch ( Exception e ) {
			error("Error sending notification to the Federation: " + e.getMessage() );
		}
	}

	private void error( String s ) {
		Logger.getInstance().error(this.getClass().getName(), s );
	}
	
	public void publish() throws Exception {
		rtiamb.publishInteractionClass( instanceCreationErrorInteractionHandle );
	}
	
	public void subscribe() throws Exception {
		rtiamb.subscribeInteractionClass( instanceCreationErrorInteractionHandle );		
	}
	
}
