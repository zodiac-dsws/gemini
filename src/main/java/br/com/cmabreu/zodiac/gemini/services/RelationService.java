package br.com.cmabreu.zodiac.gemini.services;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import br.com.cmabreu.zodiac.gemini.core.Logger;
import br.com.cmabreu.zodiac.gemini.core.UserTableEntity;
import br.com.cmabreu.zodiac.gemini.entity.Relation;
import br.com.cmabreu.zodiac.gemini.exceptions.DatabaseConnectException;
import br.com.cmabreu.zodiac.gemini.exceptions.NotFoundException;
import br.com.cmabreu.zodiac.gemini.repository.RelationRepository;


public class RelationService { 
	private RelationRepository rep;
	
	public RelationService() throws DatabaseConnectException {
		this.rep = new RelationRepository();
	}
	
	@SuppressWarnings("rawtypes")
	public Set<UserTableEntity> genericFetchList(String query) throws Exception {
		debug("generic fetch " + query );
		if ( !rep.isOpen() ) {
			rep.newTransaction();
		}
		Set<UserTableEntity> result = new LinkedHashSet<UserTableEntity>();
		for ( Object obj : rep.genericFetchList(query) ) {
			UserTableEntity ut = new UserTableEntity( (Map)obj );
			result.add(ut);
		}
		rep.closeSession();
		return result;
	}	
	
	public List<Relation> getList() throws NotFoundException {
		return rep.getList();	
	}	
	
	private void debug( String s ) {
		Logger.getInstance().debug(this.getClass().getName(), s );
	}	


}
