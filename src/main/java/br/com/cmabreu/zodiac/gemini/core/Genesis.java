package br.com.cmabreu.zodiac.gemini.core;

import java.util.Set;
import java.util.TreeSet;

import br.com.cmabreu.zodiac.gemini.entity.Activity;
import br.com.cmabreu.zodiac.gemini.entity.Experiment;
import br.com.cmabreu.zodiac.gemini.entity.Fragment;
import br.com.cmabreu.zodiac.gemini.entity.Relation;
import br.com.cmabreu.zodiac.gemini.misc.json.JsonElementConversor;
import br.com.cmabreu.zodiac.gemini.services.RelationService;
import br.com.cmabreu.zodiac.gemini.types.ActivityType;

public class Genesis {
	private Set<Activity> activities = new TreeSet<Activity>();
	private Activity root = null;

	private JsonElementConversor conversor  = new JsonElementConversor();
	private Experiment experiment;

	public void setExperiment( Experiment experiment ) {
		this.experiment = experiment;
	}
	
	private void convert() throws Exception {
		setActivities( conversor.convert(experiment) );
	}
	
	/**
	 * Verifica se as tabelas de entrada das atividades de entrada possuem dados.
	 */
	private void checkEntrancePointsAvailability() {
		boolean canRun = true;
		try {
			RelationService rs = new RelationService();
			for ( Activity act : activities  ) {
				// Se for uma atividade de início de workflow (não há atividades anteriores)...
				if ( act.getPreviousActivities().size() == 0 ) {
					// To fix issue #125
					// We cannot guarantee the data before the experiment runs
					// Assume the data is always available 
					if( act.getType() == ActivityType.SELECT ) {
						canRun = true;
					} else {
						for ( Relation rel : act.getInputRelations() ) {
							String inputTable = rel.getName();
							int count = rs.getCount( inputTable, "where id_experiment = " + experiment.getIdExperiment() );
							rel.setSize( count );
							if ( count == 0 ) {
								canRun = false;
							}
						}
					}
				}
			}
		} catch ( Exception e ) {
			e.printStackTrace();
			canRun = false;
		}
		experiment.setAvailability( canRun );
	}

	
	public Experiment generate( Experiment experiment ) throws Exception {
		if ( experiment != null ) {
			this.experiment = experiment;
			convert();
		}
		checkEntrancePointsAvailability();
		return this.experiment;
	}


	public Experiment checkTables( Experiment experiment ) throws Exception {
		this.experiment = experiment;
		for ( Fragment frag : experiment.getFragments()  ) {
			activities.addAll( frag.getActivities() );
		}
		checkEntrancePointsAvailability();
		return this.experiment;
	}
	
	
	public Activity getRoot() {
		return root;
	}

	public Set<Activity> getActivities() {
		return activities;
	}
	
	private void setActivities( Set<Activity> activities ) {
		root = null;
		this.activities = activities;
		for ( Activity act : activities  ) {
			if ( act.getPreviousActivities().size() == 0 ) {
				root = act;
				break;
			}
		}
		getFragments();
	}
	
	private void getFragments() {
		if ( activities.size() > 0 ) {
			FragmentCreator fc = new FragmentCreator();
			this.experiment.setFragments( fc.getFragments( activities ) );
		}
	}
	
}
