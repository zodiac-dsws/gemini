package br.com.cmabreu.zodiac.gemini.federation.federates;

import java.util.Calendar;

import br.com.cmabreu.zodiac.gemini.core.FragmentInstancer;
import br.com.cmabreu.zodiac.gemini.core.Logger;
import br.com.cmabreu.zodiac.gemini.entity.Experiment;
import br.com.cmabreu.zodiac.gemini.services.ExperimentService;
import br.com.cmabreu.zodiac.gemini.services.FragmentService;
import br.com.cmabreu.zodiac.gemini.types.ExperimentStatus;
import hla.rti1516e.ParameterHandleValueMap;

public class InstanceGeneratorThread implements Runnable {
	private ParameterHandleValueMap theParameters;
	
	public InstanceGeneratorThread( ParameterHandleValueMap theParameters ) {
		this.theParameters = theParameters;
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
			String experimentSerial = federate.getGenerateInstancesInteractionClass().getExperimentSerial( theParameters );
	
			try {
				debug("Generate instances for Experiment " + experimentSerial + " under external request.");
		
				ExperimentService es = new ExperimentService();
				Experiment exp = es.getExperiment(experimentSerial);
		
				FragmentService fs = new FragmentService();
				exp.setFragments( fs.getList( exp.getIdExperiment() ) );
		
				debug("Experiment " + exp.getTagExec() + " found. Generating Instances...");
		
				FragmentInstancer fp = new FragmentInstancer( exp );
				fp.generate();
				int pips = fp.getInstances().size();
				if ( pips == 0) {
					debug("Experiment " + experimentSerial + " generate no Instances. Will finish it." );
					es.newTransaction();
					exp.setStatus( ExperimentStatus.FINISHED );
					exp.setFinishDateTime( Calendar.getInstance().getTime() );
					es.updateExperiment(exp);
					debug("Broadcast to the Federation Experiment " + experimentSerial + " is finished." );
					federate.getExperimentFinishedInteractionClass().send( experimentSerial );
				} else {
					debug("done generating " + pips + " Instances for Experiment " + experimentSerial );
					federate.getInstancesCreatedInteractionClass().send( experimentSerial, pips);
				}
			} catch ( Exception e ) {
				federate.getInstanceCreationErrorInteractionClass().send(experimentSerial, "Error creating instances: " + e.getMessage() );
			}
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
