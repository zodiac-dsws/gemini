package br.com.cmabreu.zodiac.gemini.repository;

import java.util.List;

import br.com.cmabreu.zodiac.gemini.core.Logger;
import br.com.cmabreu.zodiac.gemini.entity.ActivationExecutor;
import br.com.cmabreu.zodiac.gemini.exceptions.DatabaseConnectException;
import br.com.cmabreu.zodiac.gemini.exceptions.DeleteException;
import br.com.cmabreu.zodiac.gemini.exceptions.InsertException;
import br.com.cmabreu.zodiac.gemini.exceptions.NotFoundException;
import br.com.cmabreu.zodiac.gemini.exceptions.UpdateException;
import br.com.cmabreu.zodiac.gemini.infra.DaoFactory;
import br.com.cmabreu.zodiac.gemini.infra.IDao;

public class ExecutorRepository extends BasicRepository {

	public ExecutorRepository() throws DatabaseConnectException {
		super();
		debug("init");
	}

	public List<ActivationExecutor> getList() throws NotFoundException {
		debug("get list" );
		DaoFactory<ActivationExecutor> df = new DaoFactory<ActivationExecutor>();
		IDao<ActivationExecutor> fm = df.getDao(this.session, ActivationExecutor.class);
		List<ActivationExecutor> executors = null;
		try {
			executors = fm.getList("select * from executors") ;
		} catch ( Exception e ) {
			closeSession();
			throw e;
		}
		closeSession();
		debug("done: " + executors.size() + " executors.");
		return executors;
	}

	public void updateActivationExecutor( ActivationExecutor executor ) throws UpdateException {
		debug("update");
		DaoFactory<ActivationExecutor> df = new DaoFactory<ActivationExecutor>();
		IDao<ActivationExecutor> fm = df.getDao(this.session, ActivationExecutor.class);
		try {
			fm.updateDO(executor);
			commit();
		} catch (UpdateException e) {
			error( e.getMessage() );
			rollBack();
			closeSession();
			throw e;
		}
		closeSession();
		debug("done");
	}
	
	public ActivationExecutor insereActivationExecutor(ActivationExecutor executor) throws InsertException {
		debug("insert");
		DaoFactory<ActivationExecutor> df = new DaoFactory<ActivationExecutor>();
		IDao<ActivationExecutor> fm = df.getDao(this.session, ActivationExecutor.class);
		
		try {
			fm.insertDO(executor);
			commit();
		} catch (InsertException e) {
			rollBack();
			closeSession();
			error( e.getMessage() );
			throw e;
		}
		closeSession();
		debug("done");
		return executor;
	}
	

	public ActivationExecutor getActivationExecutor(String executorAlias) throws NotFoundException {
		debug("retrieving executor by alias " + executorAlias + "..." );
		DaoFactory<ActivationExecutor> df = new DaoFactory<ActivationExecutor>();
		IDao<ActivationExecutor> fm = df.getDao(this.session, ActivationExecutor.class);
		ActivationExecutor executor = null;
		try {
			executor = fm.getList("select * from executors where executoralias = '" + executorAlias + "'").get(0);
		} catch ( Exception e ) {
			closeSession();		
			throw new NotFoundException("Cannot find executor "+executorAlias+". Please check the name.");
		} 
		debug("done");
		closeSession();
		return executor;
	}

	
	public ActivationExecutor getActivationExecutor(int idActivationExecutor) throws NotFoundException {
		debug("get " + idActivationExecutor + "...");
		DaoFactory<ActivationExecutor> df = new DaoFactory<ActivationExecutor>();
		IDao<ActivationExecutor> fm = df.getDao(this.session, ActivationExecutor.class);
		ActivationExecutor executor = null;
		try {
			executor = fm.getDO(idActivationExecutor);
		} catch ( Exception e ) {
			closeSession();		
			throw e;
		} 
		closeSession();		
		debug("done: " + executor.getExecutorAlias() );
		return executor;
	}
	

	public void excluiActivationExecutor(ActivationExecutor executor) throws DeleteException {
		debug("delete" );
		DaoFactory<ActivationExecutor> df = new DaoFactory<ActivationExecutor>();
		IDao<ActivationExecutor> fm = df.getDao(this.session, ActivationExecutor.class);
		try {
			fm.deleteDO(executor);
			commit();
		} catch (DeleteException e) {
			rollBack();
			closeSession();
			error( e.getMessage() );
			throw e;			
		}
		debug("done");
		closeSession();
	}	
	
	private void debug( String s ) {
		Logger.getInstance().debug(this.getClass().getName(), s );
	}	
	
	private void error( String s ) {
		Logger.getInstance().error(this.getClass().getName(), s );
	}		
	
}
