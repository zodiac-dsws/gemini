package br.com.cmabreu.zodiac.gemini.federation;

import hla.rti1516e.CallbackModel;
import hla.rti1516e.RTIambassador;
import hla.rti1516e.RtiFactory;
import hla.rti1516e.RtiFactoryFactory;

public class RTIAmbassadorProvider {
	private static RTIAmbassadorProvider instance;
	private RTIambassador rtiamb;
	private GeminiAmbassador myAmb;

	private RTIAmbassadorProvider() throws Exception {
		RtiFactory factory = RtiFactoryFactory.getRtiFactory();
		rtiamb = factory.getRtiAmbassador();
		myAmb = new GeminiAmbassador();
		rtiamb.connect(myAmb, CallbackModel.HLA_IMMEDIATE);
	}
	
	public static RTIAmbassadorProvider getInstance() throws Exception {
		if ( instance == null ) {
			instance = new RTIAmbassadorProvider();
		}
		return instance;
	}
	
	public GeminiAmbassador getGeminiAmbassador() {
		return myAmb;
	}
	
	public RTIambassador getRTIAmbassador() {
		return rtiamb;
	}
	
	
}
