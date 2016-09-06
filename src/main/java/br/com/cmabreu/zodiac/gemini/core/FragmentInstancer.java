package br.com.cmabreu.zodiac.gemini.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import br.com.cmabreu.zodiac.gemini.entity.Activity;
import br.com.cmabreu.zodiac.gemini.entity.Experiment;
import br.com.cmabreu.zodiac.gemini.entity.Fragment;
import br.com.cmabreu.zodiac.gemini.entity.Instance;
import br.com.cmabreu.zodiac.gemini.entity.Relation;
import br.com.cmabreu.zodiac.gemini.misc.FragmentComparator;
import br.com.cmabreu.zodiac.gemini.services.FragmentService;
import br.com.cmabreu.zodiac.gemini.services.InstanceService;
import br.com.cmabreu.zodiac.gemini.services.RelationService;
import br.com.cmabreu.zodiac.gemini.types.ActivityType;
import br.com.cmabreu.zodiac.gemini.types.FragmentStatus;

public class FragmentInstancer {
	private List<Fragment> fragments;
	private Experiment experiment;
	List<Instance> instances = new ArrayList<Instance>();
	
	/**
	 * Check if a Fragment contains an Activity
	 */
	private boolean contains( Fragment fragment, Activity act ) {
		for ( Activity activity : fragment.getActivities() ) {
			if ( activity.equals( act ) ) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Localiza qual atividade é a inicial do fragmento analizando se sua(s) atividade(s)
	 * de entrada estão fora de seu próprio fragmento.
	 */
	private Activity getEntrancePoint( Fragment fragment ) {
		for ( Activity act : fragment.getActivities() ) {
			if ( act.getPreviousActivities().size() == 0 ) {
				return act;
			} else {
				for( Activity input : act.getPreviousActivities() ) {
					if ( !contains(fragment, input ) ) {
						return act;
					}
				}
			}
		}
		return null;
	}
	
	
	private void sort() {
		FragmentComparator fc = new FragmentComparator();
		Collections.sort(this.fragments, fc);
	}	
	
	/**
	 * Construtor.
	 * 
	 */
	public FragmentInstancer( Experiment experiment ) {
		this.experiment = experiment;
		this.fragments = experiment.getFragments();
		sort();
	}
	
	/**
	 * Verifica se a tabela tableName possui algum registro.
	 */
	private boolean haveTableSomeData( String tableName ) {
		debug("verify table " + tableName + " data...");
		boolean result = false;
		try {
			RelationService ts = new RelationService();
			Set<UserTableEntity> ute = ts.genericFetchList("select * from " + tableName + " where id_experiment = " + experiment.getIdExperiment() );
			result = ( ute.size() > 0 );
		} catch ( Exception e ) { /** Any error = have no data **/ }
		if ( !result ) {
			debug("table " + tableName + " is empty.");
		}
		return result;
	}

	
	private boolean checkExperimentStartPoint( Activity activity ) {
		if ( activity.getPreviousActivities().size() == 0 ) {
			
			// To fix issue #125
			// We cannot guarantee the data before the experiment runs
			// Assume the data is always available 
			if( activity.getType() == ActivityType.SELECT ) {
				return true;
			}
			// ============================
			
			// Is the Experiment start point. Check data availability.
			debug(activity.getTag() +  " is EXPERIMENT entrance point");
			boolean finalResult = activity.getInputRelations().size() > 0;
			int count = 0;
			for ( Relation rel : activity.getInputRelations() ) {
				if ( !haveTableSomeData( rel.getName() ) ) {
					count++;
					debug( " > needed source table " + rel.getName() + " produced no data.");
				} else {
					debug( " > needed source table " + rel.getName() + " have data.");
				}
			}
			int totalAvailability = activity.getInputRelations().size() - count;
			finalResult = finalResult && ( totalAvailability > 0  );
			return finalResult; 
		} else {
			debug(activity.getTag() +  " is NOT an EXPERIMENT entrance point.");
			return false;
		}
	}
	
	private boolean checkSourceDataAvailability( Activity activity ) {
		debug("checking data availability");
		boolean canRun = true;  
		if ( activity.getPreviousActivities().size() > 0 ) {
			// Is the Fragment start point. Check all previous activities for produced data. 
			debug(activity.getTag() +  " is FRAGMENT entrance point");
			int count = 0;
			for ( Activity act : activity.getPreviousActivities() ) {
				if ( !haveTableSomeData( act.getOutputRelation().getName() ) ) {
					count++;
					debug( " > needed activity " + act.getTag() + " (" + act.getSerial() + ") produced no data. Table " + act.getOutputRelation().getName() );
				} else {
					debug( " > needed activity " + act.getTag() + " (" + act.getSerial() + ") produced data in table " + act.getOutputRelation().getName() );
				}
			}
			int totalAvailability = activity.getPreviousActivities().size() - count;
			if ( totalAvailability == 0  ) {
				canRun = false;
			}
		} else {
			debug(activity.getTag() +  " is not FRAGMENT entrance point. trying as EXPERIMENT E.P.");
			return checkExperimentStartPoint( activity );
		}
		// canRun == false means: Its a Experiment entrance point ( previous activities = 0 ) or we have no data anywhere
		return canRun;
	}
	
	
	/**
	 *	Para cada fragmento da lista verifica se sua atividade de entrada
	 *	pode ser executada. Gera os instances, salva no banco e atualiza o 
	 *	status do fragmento para PIPELINED
	 */
	public void generate() throws Exception {
		try {
			for ( Fragment frag : fragments ) {
				debug("Checking fragment " + frag.getSerial() + " (" + frag.getStatus() + "):");
				if ( ( frag.getStatus() == FragmentStatus.PREVIEW ) || ( frag.getStatus() == FragmentStatus.READY ) ) {
					debug("will create instances for fragment " + frag.getSerial() );
					Activity act = getEntrancePoint( frag );
					if ( act != null ) {
						debug("entrance point: activity " + act.getTag() + " (" + act.getExecutorAlias() + ")" );
						// Check if any of source tables have any data...
						if ( checkSourceDataAvailability( act ) ) {
							instances = InstanceGeneratorFactory.getGenerator( act.getType() ).generateInstances(act, frag);
							if ( instances.size() > 0 ) {
								debug("done. " + instances.size() + " instances generated. will store...");
								new InstanceService().insertInstanceList(instances);
								debug("done storing instances to database. updating fragment status to PIPELINED...");
								
								frag.setStatus( FragmentStatus.PIPELINED );
								frag.setRemainingInstances( instances.size() );
								frag.setTotalInstances( instances.size() );
								FragmentService fs = new FragmentService();
								fs.updateFragment( frag );
								
							} else {
								error("no instances were created.");
							}
							debug("done");
						} else {
							debug("no data available. is it an experiment E.P. ? ");
							if ( checkExperimentStartPoint(act) ) {
								warn("no data found in any source tables. cannot run this Experiment.");
								// Its an Experiment entrance point with no data. Cannot Run!
							} else {
								// Its a Fragment entrance point with no data.
								warn("no data found in any source tables. Its READY to go but not now.");
								frag.setStatus( FragmentStatus.READY );
								frag.setRemainingInstances( 0 );
								frag.setTotalInstances( 0 );
								FragmentService fs = new FragmentService();
								fs.updateFragment( frag );
							}
						}
						
					} else {
						debug("no entrance point. aborting...");
					}
				}
			}
		} catch ( Exception e ) {
			e.printStackTrace();
			throw e;
		}
	}

	
	public List<Instance> getInstances() {
		return instances;
	}
	
	private void debug( String s ) {
		Logger.getInstance().debug(this.getClass().getName(), s );
	}	

	private void error( String s ) {
		Logger.getInstance().error(this.getClass().getName(), s );
	}		
	
	private void warn( String s ) {
		Logger.getInstance().warn(this.getClass().getName(), s );
	}		
}
