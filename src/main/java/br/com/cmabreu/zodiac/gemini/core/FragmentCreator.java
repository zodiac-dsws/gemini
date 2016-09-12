package br.com.cmabreu.zodiac.gemini.core;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import br.com.cmabreu.zodiac.gemini.entity.Activity;
import br.com.cmabreu.zodiac.gemini.entity.Fragment;
import br.com.cmabreu.zodiac.gemini.types.ExecutionModel;

public class FragmentCreator {
	private Set<Fragment> fragments = new HashSet<Fragment>(); 
	private Fragment fragmentCache; 
	private Set<String> visitedList = new HashSet<String>();
	private Set<Activity> cache = new TreeSet<Activity>();
	private Set<Activity> activities = new TreeSet<Activity>();

	public List<Fragment> getFragments( Set<Activity> activities ) {
		this.activities = activities;
		generateFragments();
		return new FragmentIndexer().getIndexedFragments(fragments) ;
	}

	private void getJoinActivities() {
		for( Activity act : activities ) {
			if ( act.getType().isBlocking() ) {
				cache.add(act);
			}
		}
	}

	
	
	// Uma atividade inicia um fragmento quando ela nao possui atividades antes dela
	// ou quando existe uma unica atividade tipo SPLIT antes dela.
	private void getStartActivities() {
		for ( Activity act : activities	) {
			if ( (act.getPreviousActivities().size() == 0) || ( act.previousIsSplit() )  ) {
				cache.add(act);
			}
		}
	}
	
	private void getJoinChildrenNotJoin() {
		for( Activity act : activities ) {
			if ( act.getType().isBlocking() ) {
				for( Activity actChild : act.getNextActivities() ) {
					if ( !actChild.getType().isBlocking() ) {
						cache.add(actChild);
					} 
				}
			}
		}
	}

	private void generateFragments() {
		fragments.clear();
		getStartActivities();
		getJoinActivities();
		getJoinChildrenNotJoin();

		for ( Activity act : cache ) {
			calcGraphWeigh(act);
		}
		visitedList.clear();

		for ( Activity act : cache ) {
			openFragment(act);
			visitNode(act);
			closeFragment();
		}

	}

	private void calcGraphWeigh( Activity activity ) {
		for ( Activity act : activity.getNextActivities() ) {
			if ( !isVisited( act.getSerial() ) ) {
				setVisited( act.getSerial() );
				calcGraphWeigh( act );
			}
		}
	}

	private void openFragment( Activity activity ) {
		ExecutionModel type = ExecutionModel.DYN_FTF;
		if ( activity.getType().isBlocking() ) {
			type = ExecutionModel.DYN_FAF;
		}
		fragmentCache = new Fragment();
		addToFragment( activity );
		fragmentCache.setType(type);
	}

	private void closeFragment() {
		fragments.add(fragmentCache);
	}

	private void addToFragment( Activity activity ) {
		fragmentCache.addActivity(activity);
	}

	private void visitNode( Activity activity ) {
		if ( !activity.getType().isBlocking() ) {
			for ( Activity act : activity.getNextActivities() ) {
				if ( (!isVisited( act.getSerial() ) ) && ( !act.getType().isBlocking() ) && ( !act.previousIsSplit() )  ) {
					setVisited( act.getSerial() );
					addToFragment(act);
					visitNode( act );
				}
			}
		} 
	}

	private boolean isVisited( String serial ) {
		for ( String vis : visitedList ) {
			if ( vis.equalsIgnoreCase( serial ) ) {
				return true;
			}
		}
		return false;
	}
	
	private void setVisited( String serial ) {
		visitedList.add( serial );
	}
	
}
