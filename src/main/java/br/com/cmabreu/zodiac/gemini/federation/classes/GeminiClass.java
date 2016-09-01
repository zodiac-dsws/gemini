package br.com.cmabreu.zodiac.gemini.federation.classes;


import br.com.cmabreu.zodiac.gemini.core.Logger;
import br.com.cmabreu.zodiac.gemini.federation.EncoderDecoder;
import br.com.cmabreu.zodiac.gemini.federation.RTIAmbassadorProvider;
import br.com.cmabreu.zodiac.gemini.federation.objects.GeminiObject;
import hla.rti1516e.AttributeHandle;
import hla.rti1516e.AttributeHandleSet;
import hla.rti1516e.AttributeHandleValueMap;
import hla.rti1516e.ObjectClassHandle;
import hla.rti1516e.ObjectInstanceHandle;
import hla.rti1516e.RTIambassador;
import hla.rti1516e.encoding.HLAunicodeString;
import hla.rti1516e.exceptions.RTIexception;

public class GeminiClass {
	private static ObjectClassHandle classHandle;
	private AttributeHandle macAddressAttributeHandle;
	private AttributeHandleSet attributes;
	private RTIambassador rtiamb;
	private EncoderDecoder encodec;
	private GeminiObject gemini;
	
	public ObjectInstanceHandle getGeminiObjectHandle() {
		return gemini.getHandle();
	}
	
	public void createNew() throws RTIexception {
		debug("Gemini is online");
		ObjectInstanceHandle handle = rtiamb.registerObjectInstance( classHandle, "Gemini" );
		gemini = new GeminiObject( handle );
	}	
	
	public GeminiClass() {
		try {
			
			rtiamb = RTIAmbassadorProvider.getInstance().getRTIAmbassador();
			classHandle = rtiamb.getObjectClassHandle( "HLAobjectRoot.Gemini" );
			attributes = rtiamb.getAttributeHandleSetFactory().create();
			macAddressAttributeHandle = rtiamb.getAttributeHandle( classHandle, "MACAddress" );
			attributes.add( macAddressAttributeHandle );
			
			encodec = new EncoderDecoder();
		} catch ( Exception e ) {
			e.printStackTrace();
		}
	}
	
	public void updateAttributeValues() throws Exception {
		HLAunicodeString currentInstanceHandleValue = encodec.createHLAunicodeString( gemini.getMacAddress() );
		AttributeHandleValueMap attributes = rtiamb.getAttributeHandleValueMapFactory().create(1);
		attributes.put( macAddressAttributeHandle, currentInstanceHandleValue.toByteArray() );
		rtiamb.updateAttributeValues( gemini.getHandle(), attributes, "Gemini Attributes".getBytes() );		
	}
	
	public ObjectClassHandle getClassHandle() {
		return classHandle;
	}
	
	public boolean isSameOf( ObjectClassHandle theObjectClass ) {
		return theObjectClass.equals( classHandle );
	}
	
	public void publish() throws RTIexception {
		debug("publish");
		rtiamb.publishObjectClassAttributes( classHandle, attributes );
	}
	
	public void subscribe() throws RTIexception {
		debug("subscribe");
		rtiamb.subscribeObjectClassAttributes( classHandle, attributes );
	}	
	
	private void debug( String s ) {
		Logger.getInstance().debug(this.getClass().getName(), s );
	}

	private void error( String s ) {
		Logger.getInstance().error(this.getClass().getName(), s );
	}

	private void warn( String s ) {
		Logger.getInstance().warn(this.getClass().getName(), s );
	}

	public void provideAttributeValueUpdate(ObjectInstanceHandle theObject, AttributeHandleSet theAttributes) {
		try {
			updateAttributeValues();
		} catch ( Exception e ) {
			error("Error sending attributes to RTI under request.");
		}
	}

	public void remove(ObjectInstanceHandle theObject) {
		warn("Gemini is offline");
		gemini = null;
	}

	public boolean objectExists(ObjectInstanceHandle theObject) {
		return gemini != null;
	}


}
