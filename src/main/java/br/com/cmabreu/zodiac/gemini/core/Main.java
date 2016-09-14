package br.com.cmabreu.zodiac.gemini.core;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import br.com.cmabreu.zodiac.gemini.core.config.Configurator;
import br.com.cmabreu.zodiac.gemini.federation.federates.GeminiFederate;
import br.com.cmabreu.zodiac.gemini.infra.ConnFactory;

public class Main {
	private ScheduledExecutorService scheduler;
	
	private void debug( String s ) {
		Logger.getInstance().debug(this.getClass().getName(), s );
	}	

	private void error( String s ) {
		Logger.getInstance().error(this.getClass().getName(), s );
	}		

    public static void main( String[] args ) {
    	System.out.println("Starting Gemini...");
    	new Main().initialize();
    }
    
    public void initialize() {

    	try {
    		Logger.getInstance().enable();
    		Logger.getInstance().canOutputToFile( true );
       
    		int interval = 5;
			Configurator config = Configurator.getInstance("config.xml");
			config.loadMainConfig();
			interval = config.getPoolIntervalSeconds();

			String user = config.getUserName();
			String passwd = config.getPassword();
			String database = config.getDatabaseName();

			debug("Credentials: " + user + " | " + database);
			
    		ConnFactory.setCredentials(user, passwd, database);

			scheduler = Executors.newSingleThreadScheduledExecutor();
	        MainHeartBeat as = new MainHeartBeat();
	        scheduler.scheduleAtFixedRate(as, interval, interval , TimeUnit.SECONDS);
	        GeminiFederate.getInstance().startServer();

	        
	        TestTimedRequest tmr = new TestTimedRequest();
	        tmr.run();
	        
		} catch (Exception e) { 
			System.out.println( e.getMessage() );
			error( e.getMessage() );
			//e.printStackTrace(); 
		}
        
        
	}
	

}
