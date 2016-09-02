package br.com.cmabreu.zodiac.gemini.services;

import java.util.List;
import java.util.Set;

import br.com.cmabreu.zodiac.gemini.core.Logger;
import br.com.cmabreu.zodiac.gemini.entity.Experiment;
import br.com.cmabreu.zodiac.gemini.entity.User;
import br.com.cmabreu.zodiac.gemini.exceptions.DatabaseConnectException;
import br.com.cmabreu.zodiac.gemini.exceptions.InsertException;
import br.com.cmabreu.zodiac.gemini.exceptions.NotFoundException;
import br.com.cmabreu.zodiac.gemini.repository.ExperimentRepository;

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


}
