package br.com.cmabreu.zodiac.gemini.core;

import java.util.ArrayList;
import java.util.List;

import br.com.cmabreu.zodiac.gemini.entity.Domain;

public class DomainStorage {
	private static List<Domain> domains;
	private static DomainStorage instance;
	
	public static DomainStorage getInstance() {
		if ( instance == null ) {
			instance = new DomainStorage();
		}
		return instance;
	}
	
	public void addDomain( Domain domain ) {
		domains.add( domain );
	}
	
	private DomainStorage() {
		domains = new ArrayList<Domain>();
	}

	
	public synchronized void setDomains(List<Domain> newDomains) {
		domains = newDomains;
	}

	public synchronized Domain getDomain( String domainName ) {
		for ( Domain domain : domains  ) {
			if ( domain.getDomainName().equals( domainName ) ) {
				return domain;
			}
		}
		return null;
	}

	public boolean domainExists( String domainName ) {
		for ( Domain domain : domains  ) {
			if ( domain.getDomainName().equals( domainName ) ) {
				return true;
			}
		}
		return false;
	}

	public boolean isColumnADomain( String columnName ) {
		for ( Domain domain : domains  ) {
			if ( domain.getDomainName().contains( "." + columnName ) ) {
				return true;
			}
		}
		return false;
	}
	
	
}
