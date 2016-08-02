package br.cefetrj.sagitarii.nunki;

import java.net.URLEncoder;

import br.cefetrj.sagitarii.nunki.comm.Communicator;

public class Notifier {
	private static Notifier instance;
	private Communicator comm;
	private Configurator configurator;
	
	public static Notifier getInstance( Communicator comm, Configurator config ) {
		if ( instance == null ) {
			instance = new Notifier( comm, config );
		}
		return instance;
	}

	private Notifier( Communicator comm, Configurator config ) {
		this.comm = comm;
		this.configurator = config;
	}

	private Notifier() {
		
	}
	
	public synchronized void notifySagitarii( String message, Activation act ) {
		String owner = "STARTING";
		String activitySerial = "";
		if ( act != null ) {
			owner = act.getExecutor();
			activitySerial = act.getActivitySerial();
		}
		message = "[" + owner + "] " + message;
		try {
			String parameters = "activitySerial=" + activitySerial + "&macAddress=" + configurator.getSystemProperties().getMacAddress() + "&errorLog=" + URLEncoder.encode( message, "UTF-8");
			comm.send("receiveNodeLog", parameters);
		} catch ( Exception e ) {
			e.printStackTrace();
		}
	}
	
	
}
