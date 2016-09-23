package br.com.cmabreu.zodiac.gemini.core;

import br.com.cmabreu.zodiac.gemini.federation.federates.GeminiFederate;

public class TestTimedRequest implements Runnable {
	
    @Override
    public void run() {
    	
    	try {
    		System.out.println("********* TEST TIMED REQUEST : WAIT   : ********");
    		Thread.sleep( 5000 );
    		System.out.println("********* TEST TIMED REQUEST : START  : ********");
    		
    		//GeminiFederate.getInstance().startExperiment( 354 );
    		
    		GeminiFederate.getInstance().startExperiment( 374 );
    		
    		System.out.println("********* TEST TIMED REQUEST : FINISH : ********");    		
    	} catch ( Exception e ) {
    		
    	}
    }
	

}
