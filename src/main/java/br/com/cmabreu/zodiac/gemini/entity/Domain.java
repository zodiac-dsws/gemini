package br.com.cmabreu.zodiac.gemini.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Entity
@Table(name="domains") 
public class Domain {

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="id_domain")
	private int idDomain;
	
	@ManyToOne
	@JoinColumn(name="id_table")
	@Fetch(FetchMode.JOIN)
	private Relation table;
	
	@Column(length=250)
	private String domainName;
	
	@Transient
	public String getColumnName() {
		if ( domainName == null || domainName.equals("") ) return null;
		String[] spt = domainName.split(".");
		return spt[1];
	}
	
	@Transient
	public String getTableName() {
		if ( domainName == null || domainName.equals("") ) return null;
		String[] spt = domainName.split(".");
		return spt[0];
	}

	public int getIdDomain() {
		return idDomain;
	}

	public void setIdDomain(int idDomain) {
		this.idDomain = idDomain;
	}

	public Relation getTable() {
		return table;
	}

	public void setTable(Relation table) {
		this.table = table;
	}

	public String getDomainName() {
		return domainName;
	}

	public void setDomainName(String domainName) {
		this.domainName = domainName;
	}

	
}
