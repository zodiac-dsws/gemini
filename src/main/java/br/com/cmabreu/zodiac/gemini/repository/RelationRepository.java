package br.com.cmabreu.zodiac.gemini.repository;

import java.util.List;

import br.com.cmabreu.zodiac.gemini.core.DomainStorage;
import br.com.cmabreu.zodiac.gemini.core.Logger;
import br.com.cmabreu.zodiac.gemini.entity.Domain;
import br.com.cmabreu.zodiac.gemini.entity.Relation;
import br.com.cmabreu.zodiac.gemini.exceptions.DatabaseConnectException;
import br.com.cmabreu.zodiac.gemini.exceptions.DeleteException;
import br.com.cmabreu.zodiac.gemini.exceptions.InsertException;
import br.com.cmabreu.zodiac.gemini.exceptions.NotFoundException;
import br.com.cmabreu.zodiac.gemini.exceptions.UpdateException;
import br.com.cmabreu.zodiac.gemini.infra.DaoFactory;
import br.com.cmabreu.zodiac.gemini.infra.IDao;
import br.com.cmabreu.zodiac.gemini.misc.TableAttribute;
import br.com.cmabreu.zodiac.gemini.misc.TableAttribute.AttributeType;

public class RelationRepository extends BasicRepository {

	public RelationRepository() throws DatabaseConnectException {
		super();
		debug("init");
	}

	
	public List<Relation> getList() throws NotFoundException {
		debug("get list" );
		DaoFactory<Relation> df = new DaoFactory<Relation>();
		IDao<Relation> fm = df.getDao(this.session, Relation.class);
		List<Relation> relacoes = null;
		try {
			relacoes = fm.getList("select * from tables");
		} catch (Exception e) {
			closeSession();
			throw e;
		}
		closeSession();
		debug("done: " + relacoes.size() + " tables.");
		return relacoes;
	}

	public void updateTable( Relation table ) throws UpdateException {
		debug("update");
		DaoFactory<Relation> df = new DaoFactory<Relation>();
		IDao<Relation> fm = df.getDao(this.session, Relation.class);
		try {
			fm.updateDO(table);
			commit();
		} catch (UpdateException e) {
			rollBack();
			closeSession();
			error( e.getMessage() );
			throw e;
		}
		closeSession();
		debug("done");
	}
	

	public List<?> genericFetchList( String query ) throws Exception {
		debug("generic query");
		debug( query );
		DaoFactory<Relation> df = new DaoFactory<Relation>();
		IDao<Relation> fm = df.getDao(this.session, Relation.class);
		List<?> retorno = null;
		try {
			retorno = fm.genericAccess( query );
		} catch (Exception e) {
			error( e.getMessage() );
			rollBack();
			closeSession();
			throw e;
		}
		closeSession();
		debug("done");
		return retorno;
	}

	
	public int getCount( String tableName, String criteria ) throws Exception {
		debug("get count " + tableName );
		DaoFactory<Relation> df = new DaoFactory<Relation>();
		IDao<Relation> fm = df.getDao(this.session, Relation.class);
		int retorno = fm.getCount(tableName,criteria);
		closeSession();
		debug("done");
		return retorno;
	}
	
	
	/**
	 * Executa um acesso generico no banco de dados
	 * Não deve ser utilizado para SELECT.
	 * 
	 */
	public void executeQuery( String query ) throws Exception {
		debug("execute query");
		debug( query );
		DaoFactory<Relation> df = new DaoFactory<Relation>();
		IDao<Relation> fm = df.getDao(this.session, Relation.class);
		try {
			fm.executeQuery( query, false );
			commit();
		} catch (Exception e) {
			error( e.getMessage() );
			error( " > " + query );
			rollBack();
			closeSession();
			throw e;
		}
		closeSession();
		debug("done");
	}
	
	public void executeQueryAndKeepOpen( String query ) throws Exception {
		debug("execute query keeping session opened");
		debug( query );
		DaoFactory<Relation> df = new DaoFactory<Relation>();
		IDao<Relation> fm = df.getDao(this.session, Relation.class);
		try {
			fm.executeQuery( query, false );
		} catch (Exception e) {
			error( e.getMessage() );
			error( " > " + query );
			rollBack();
			closeSession();
			throw e;
		}
		debug("done");
	}

	public int insertDomain( Domain domain ) throws InsertException {
		debug("insert domain " + domain.getDomainName() );
		DaoFactory<Domain> factory = new DaoFactory<Domain>();
		IDao<Domain> domainDao = factory.getDao(this.session, Domain.class);
		int ret =  domainDao.insertDO( domain );
		commit();
		DomainStorage.getInstance().addDomain( domain );
		return ret;
	}
	

	public List<Domain> getDomains() throws NotFoundException {
		debug("get domains list" );
		DaoFactory<Domain> factory = new DaoFactory<Domain>();
		IDao<Domain> domainDao = factory.getDao(this.session, Domain.class);
		List<Domain> domains = null;
		try {
			domains = domainDao.getList("select * from domains");
		} catch (Exception e) {
			closeSession();
			throw e;
		}
		closeSession();
		debug("done: " + domains.size() + " domains.");
		return domains;
	}

	
	public Relation insertTable(Relation table, List<TableAttribute> attributes) throws InsertException {
		debug("insert table " + table.getName() );
		DaoFactory<Relation> df = new DaoFactory<Relation>();
		IDao<Relation> fm = df.getDao(this.session, Relation.class);
		
		DaoFactory<Domain> factory = new DaoFactory<Domain>();
		IDao<Domain> domainDao = factory.getDao(this.session, Domain.class);

		try {
			fm.insertDO(table);
			
			for( TableAttribute attr : attributes ) {
				if ( attr.getType() == AttributeType.FILE ) {
					debug("file type attribute " + attr.getName() + " detected. creating domain...");
					Domain dom = new Domain();
					dom.setDomainName( table.getName() + "." + attr.getName() );
					dom.setTable(table);
					domainDao.insertDO( dom );
					debug("done creating domain for " + attr.getName() );
				}
			}
			commit();
			try {
				DomainStorage.getInstance().setDomains( getDomains() );
			} catch ( NotFoundException nfe ) { 
				debug("no domains found in database");
			}
		} catch (Exception e) {
			error( e.getMessage() );
			rollBack();
			closeSession();
			throw new InsertException(e.getMessage());
		}
		debug("done");
		closeSession();
		return table;
	}
	
	
	public void createInternalIndex( String tableName ) throws InsertException {
		debug("create internal index for table " + tableName);

		String sql = "CREATE INDEX " + tableName + "_indx ON "+tableName + 
				" (index_id, id_experiment, id_activity, id_instance);";

		sql = sql + "CREATE INDEX " + tableName + "_expindx ON "+tableName + 
				" (id_experiment);";

		DaoFactory<Relation> df = new DaoFactory<Relation>();
		IDao<Relation> fm = df.getDao(this.session, Relation.class);
		try {
			fm.executeQuery( sql, false );
			commit();
		} catch (Exception e) {
			error( e.getMessage() );
			rollBack();
			closeSession();
			throw new InsertException(e.getMessage());
		}
		closeSession();
		debug("done");
	}
	
	public void createDatabaseTable(String schema) throws InsertException {
		debug("create custom table");
		DaoFactory<Relation> df = new DaoFactory<Relation>();
		IDao<Relation> fm = df.getDao(this.session, Relation.class);
		try {
			fm.executeQuery( schema, false );
			commit();
		} catch (Exception e) {
			error( e.getMessage() );
			rollBack();
			closeSession();
			throw new InsertException(e.getMessage());
		}
		closeSession();
		debug("done");
	}
	
	public Relation getTable(String name) throws NotFoundException {
		debug("retrieve table by name " + name + "..." );
		DaoFactory<Relation> df = new DaoFactory<Relation>();
		IDao<Relation> fm = df.getDao(this.session, Relation.class);
		Relation table = null;
		try {
			table = fm.getList("select * from tables where name = '" + name + "'").get(0);
		} catch ( Exception e ) {
			closeSession();
			error( e.getMessage() );
			throw e;
		}
		closeSession();
		debug("done");
		return table;
	}

	
	public Relation getTable(int idRelacao) throws NotFoundException {
		debug("retrieve");
		DaoFactory<Relation> df = new DaoFactory<Relation>();
		IDao<Relation> fm = df.getDao(this.session, Relation.class);
		Relation table = null;
		try {
			table = fm.getDO(idRelacao);
		} catch ( Exception e ) {
			closeSession();
			error( e.getMessage() );
			throw e;
		}
		closeSession();
		debug("done: " + table.getName() );
		return table;
	}
	
	public void deleteTable(Relation table) throws DeleteException {
		debug("delete" );
		DaoFactory<Relation> df = new DaoFactory<Relation>();
		IDao<Relation> fm = df.getDao(this.session, Relation.class);
		try {
			fm.executeQuery("drop table " + table.getName(), false );
			fm.deleteDO(table);
			commit();
		} catch (Exception e) {
			error( e.getMessage() );
			rollBack();
			closeSession();
			throw new DeleteException( e.getMessage() );			
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
