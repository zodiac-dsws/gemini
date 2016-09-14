package br.com.cmabreu.zodiac.gemini.federation.federates;

import br.com.cmabreu.zodiac.gemini.core.Logger;
import br.com.cmabreu.zodiac.gemini.entity.Experiment;
import br.com.cmabreu.zodiac.gemini.services.ExperimentService;

public class StartExperimentThread implements Runnable {
	private int idExperiment;
	
	public StartExperimentThread( int idExperiment ) {
		this.idExperiment = idExperiment;
	}
	
	private void debug( String s ) {
		Logger.getInstance().debug(this.getClass().getName(), s );
	}	

	private void error( String s ) {
		Logger.getInstance().error(this.getClass().getName(), s );
	}
	
    @Override
    public void run() {
    	debug("start thread");
    	try {
	    	GeminiFederate federate = GeminiFederate.getInstance();
			debug("Starting Experiment ID " + idExperiment + "...");
			Experiment experiment = new ExperimentService().runExperiment( idExperiment );
			debug("Experiment " + experiment.getTagExec() + " is now running.");

			// Notify the Federation
			federate.getExperimentStartedInteractionClass().send( experiment.getTagExec() );
    	} catch ( Exception e ) {
    		error("Fatal error starting thread: " + e.getMessage() );
    	}
    	debug("end thread");

		/*
			select exp.id_experiment as experiment, act.executoralias, fr.id_fragment as fragment, fr.status as fragstatus, ins.serial as instance, ins.type from instances ins 
				join fragments fr on fr.id_fragment = ins.id_fragment
				join activities act on act.id_fragment = fr.id_fragment
				join experiments exp on exp.id_experiment = fr.id_experiment
			order by fr.serial

			update fragments set status = 'RUNNING' where id_fragment = 3154
			update instances set status = 'PIPELINED' where id_fragment = 3153
			update experiments set status = 'RUNNING' where id_experiment = 355
			select * from instances where status = 'PIPELINED'

			select distinct (status), count(status) from instances group by status order by status
			select id_fragment, status, count(status) from instances group by id_fragment,status order by status	

		 */
    }
	

}
