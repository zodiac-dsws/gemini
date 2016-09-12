package br.com.cmabreu.zodiac.gemini.services;

import java.util.Calendar;
import java.util.List;
import java.util.Set;

import br.com.cmabreu.zodiac.gemini.core.FragmentInstancer;
import br.com.cmabreu.zodiac.gemini.core.Genesis;
import br.com.cmabreu.zodiac.gemini.core.Logger;
import br.com.cmabreu.zodiac.gemini.entity.Experiment;
import br.com.cmabreu.zodiac.gemini.entity.User;
import br.com.cmabreu.zodiac.gemini.exceptions.DatabaseConnectException;
import br.com.cmabreu.zodiac.gemini.exceptions.InsertException;
import br.com.cmabreu.zodiac.gemini.exceptions.NotFoundException;
import br.com.cmabreu.zodiac.gemini.exceptions.UpdateException;
import br.com.cmabreu.zodiac.gemini.repository.ExperimentRepository;
import br.com.cmabreu.zodiac.gemini.types.ExperimentStatus;

public class ExperimentService {
	private ExperimentRepository rep;

	public ExperimentService() throws DatabaseConnectException {
		this.rep = new ExperimentRepository();
	}

	public void close() {
		rep.closeSession();
	}

	public void newTransaction() {
		rep.newTransaction();
	}
	
	private Experiment insertExperiment(Experiment experiment) throws InsertException {
		Experiment expRet = rep.insertExperiment( experiment );
		return expRet ;
	}	
	
	public Experiment generateExperiment( Experiment source, User owner ) throws InsertException {
		Experiment ex = new Experiment();
		try {
			ex.setWorkflow( source.getWorkflow() );
			ex.setActivitiesSpecs( source.getActivitiesSpecs() );
			ex.setImagePreviewData( source.getImagePreviewData() );
			ex.setOwner( owner );
			ex = insertExperiment(ex);
		} catch ( Exception e ) {
			throw new InsertException( e.getMessage() );
		}
		return ex;
	}
	
	public List<Experiment> getRunning() throws NotFoundException {
		debug("retrieve running experiments");
		List<Experiment> running = rep.getRunning();
		try {
			FragmentService fs = new FragmentService();
			for ( Experiment exp : running ) {
				exp.setFragments( fs.getList( exp.getIdExperiment() ) );
			}
		} catch (DatabaseConnectException e) {
			throw new NotFoundException( e.getMessage() );
		}
		debug("done");
		
		return running;
	}

	public Set<Experiment> getList() throws NotFoundException {
		debug("get list");
		Set<Experiment> preList = rep.getList();
		return preList;	
	}

	public Experiment getExperiment( String experimentSerial ) throws NotFoundException {
		return rep.getExperiment( experimentSerial );
	}
	
	public Set<Experiment> getList( User user ) throws NotFoundException {
		debug("get list : user " + user.getLoginName() );
		Set<Experiment> preList = rep.getList( user );
		return preList;	
	}

	private void debug( String s ) {
		Logger.getInstance().debug(this.getClass().getName(), s );
	}	
	
	private void error( String s ) {
		Logger.getInstance().error(this.getClass().getName(), s );
	}		

	
	
	public Experiment runExperiment( int idExperiment ) throws Exception {
		
		debug( "Generating Activities to run Experiment " + idExperiment );  
		Experiment experiment = null;
		try {
			experiment = rep.getExperiment( idExperiment );
		} catch (NotFoundException e) {
			throw new UpdateException( e.getMessage() );
		}
		
		if ( experiment.getStatus() == ExperimentStatus.FINISHED ) {
			throw new Exception("This experiment is finished.");
		}

		if ( experiment.getStatus() == ExperimentStatus.RUNNING ) {
			throw new Exception("This experiment is already running.");
		}
		
		if ( experiment.getStatus() == ExperimentStatus.STARTING ) {
			throw new Exception("This experiment is already starting. Be patient.");
		}
		
		debug("setting experiment status to STARTING");
		experiment.setStatus( ExperimentStatus.STARTING );
		rep.newTransaction();
		rep.updateExperiment(experiment);

		
		try {
		
			debug("Genesis is converting JSON specifications and fragmenting...");
			// Gerar atividades basado na especificação JSON
			Genesis ag = new Genesis();
			ag.generate(experiment);
	
			// Fragmentar baseado nos tipos de atividades e dados disponíveis.
			int acts = ag.getActivities().size();
			int frgs = experiment.getFragments().size();
	
			debug("fragmenting is done. storing now.");
			new FragmentService().insertFragmentList( experiment.getFragments() );
			
			experiment.setStatus( ExperimentStatus.RUNNING );
			experiment.setLastExecutionDate( Calendar.getInstance().getTime() );
	
			// Gerar instances do primeiro fragmento que pode ser executado.
			debug("creating instances");
			FragmentInstancer fp = new FragmentInstancer( experiment );
			fp.generate();
			
			int pips = fp.getInstances().size();
			
			debug( acts + " activities generated." );
			debug( frgs + " fragments generated." );
			debug( pips + " instances generated." );
	
			debug("saving experiment");
			rep.newTransaction();
			rep.updateExperiment(experiment);
	
			//Sagitarii.getInstance().addRunningExperiment(experiment);
			
			debug( "Experiment " + experiment.getTagExec() + " is now running with " + acts + " activities, " + frgs + " fragments and " + pips + " instances.");
			
		} catch ( Exception e ) {
			error( e.getMessage() );

			debug("setting experiment status to STOPPED due to starting error");
			experiment.setStatus( ExperimentStatus.STOPPED );
			rep.newTransaction();
			rep.updateExperiment(experiment);
			
			throw e;
		}
		return experiment;
	}
	


}
