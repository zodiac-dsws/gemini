package br.com.cmabreu.zodiac.gemini.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Entity
@Table(name="queries", indexes = {
        @Index(columnList = "id_query", name = "qry_id_hndx")
})
public class CustomQuery {

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="id_query")
	private int idCustomQuery;
	
	@Column(length=250)
	private String name;

	@ManyToOne
	@JoinColumn(name="id_experiment", foreignKey = @ForeignKey(name = "fk_customq_experiment"))
	@Fetch(FetchMode.JOIN)
	private Experiment experiment;    
	
	@Column(columnDefinition = "TEXT", name="query")
	private String query;

	public int getIdCustomQuery() {
		return idCustomQuery;
	}

	public void setIdCustomQuery(int idCustomQuery) {
		this.idCustomQuery = idCustomQuery;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Experiment getExperiment() {
		return experiment;
	}

	public void setExperiment(Experiment experiment) {
		this.experiment = experiment;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	
	
}
