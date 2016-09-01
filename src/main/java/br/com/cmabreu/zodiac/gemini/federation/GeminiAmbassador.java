package br.com.cmabreu.zodiac.gemini.federation;

import br.com.cmabreu.zodiac.gemini.core.Logger;
import br.com.cmabreu.zodiac.gemini.federation.federates.GeminiFederate;
import hla.rti1516e.AttributeHandleSet;
import hla.rti1516e.AttributeHandleValueMap;
import hla.rti1516e.InteractionClassHandle;
import hla.rti1516e.NullFederateAmbassador;
import hla.rti1516e.ObjectClassHandle;
import hla.rti1516e.ObjectInstanceHandle;
import hla.rti1516e.OrderType;
import hla.rti1516e.ParameterHandleValueMap;
import hla.rti1516e.TransportationTypeHandle;
import hla.rti1516e.exceptions.FederateInternalError;


public class GeminiAmbassador extends NullFederateAmbassador {
	
	@Override
	public void provideAttributeValueUpdate(ObjectInstanceHandle theObject,	AttributeHandleSet theAttributes, byte[] userSuppliedTag)
			throws FederateInternalError {

		try {
			GeminiFederate.getInstance().getGeminiClass().provideAttributeValueUpdate(theObject, theAttributes);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}	
	
	@Override
	public void reflectAttributeValues( ObjectInstanceHandle theObject, AttributeHandleValueMap theAttributes,
	                                    byte[] tag, OrderType sentOrder, TransportationTypeHandle transport,
	                                    SupplementalReflectInfo reflectInfo ) throws FederateInternalError {
		
		try {
			GeminiFederate.getInstance().reflectAttributeUpdate( theObject, theAttributes );
		} catch ( Exception e ) {
			e.printStackTrace();
		}
		
	}

	
	@Override
    public void requestAttributeOwnershipRelease( ObjectInstanceHandle theObject, AttributeHandleSet candidateAttributes, byte[] userSuppliedTag) throws FederateInternalError {
		//
    }		
	
	@Override
	public void discoverObjectInstance( ObjectInstanceHandle theObject, ObjectClassHandle theObjectClass, String objectName ) throws FederateInternalError {
		debug("New object discovered (" + objectName + ")");
	}
	
	@Override
	public void attributeOwnershipAcquisitionNotification(	ObjectInstanceHandle theObject,	AttributeHandleSet securedAttributes, byte[] userSuppliedTag) throws FederateInternalError {
		//
	}	
	
	
	@Override
	public void removeObjectInstance(ObjectInstanceHandle theObject, byte[] userSuppliedTag, OrderType sentOrdering, SupplementalRemoveInfo removeInfo)	{
		debug("Remove object " + new String(userSuppliedTag) );
	}	
	
	@Override
	public void receiveInteraction(InteractionClassHandle interactionClass,	ParameterHandleValueMap theParameters,
			byte[] userSuppliedTag, OrderType sentOrdering,	TransportationTypeHandle theTransport, 
			SupplementalReceiveInfo receiveInfo) throws FederateInternalError {

		try {
			GeminiFederate.getInstance().generateInstances( theParameters );
		} catch ( Exception e ) {
			
		}
		
	}
	
	private void debug( String s ) {
		Logger.getInstance().debug(this.getClass().getName(), s );
	}	

	private void error( String s ) {
		Logger.getInstance().error(this.getClass().getName(), s );
	}		
	
}
