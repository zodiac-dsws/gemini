package br.com.cmabreu.zodiac.gemini.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

import br.com.cmabreu.zodiac.gemini.entity.Activity;
import br.com.cmabreu.zodiac.gemini.entity.Fragment;
import br.com.cmabreu.zodiac.gemini.misc.FragmentComparator;

public class FragmentIndexer {
	private Set<Fragment> fragments = new TreeSet<Fragment>();
	private Queue<Fragment> fragmentQueue = new LinkedList<Fragment>();
	private List<Fragment> orderedFragments = new ArrayList<Fragment>();
	

	public List<Fragment> getIndexedFragments( Set<Fragment> fragments ) {
		if ( ( fragments != null ) && ( fragments.size() > 0) ) {
			this.fragments = fragments;
			Fragment frag = getFirst();
			frag.setIndexOrder(1);
			orderedFragments.add(frag);
			visitFragment( frag );
			sort();
		}
		return orderedFragments;
	}

	private Fragment getFirst() {
		for ( Fragment frag : fragments ) {
			return frag;
		}
		return null;
	}

	private void addToQueue( Fragment frag,  int order  ) {
		if ( !orderedFragments.contains( frag ) ) {
			frag.setIndexOrder( order );
			fragmentQueue.add(frag);
		}
	}

	private Fragment getFragmentFromActivity ( Fragment owner, Activity activity ) {
		for ( Fragment frag : fragments ) {
			for ( Activity act : frag.getActivities() ) {
				if ( ( act.equals(activity) ) && ( !owner.equals(frag)	) ) {
					return frag;
				}
			}
		}
		return null;
	}
	
	
	private void visitFragment( Fragment fragment ) {
		int index = fragment.getIndexOrder();
		for ( Activity actInFragment : fragment.getActivities() ) {
			
			int offSet = 1;
			for ( Activity inputActivity : actInFragment.getPreviousActivities() ) {
				Fragment inputFrag = getFragmentFromActivity(fragment, inputActivity); 
				if ( inputFrag != null ) {
					addToQueue(inputFrag, index - offSet );
					offSet++;
				}
			}
			
			offSet = 1;
			for ( Activity inputActivity : actInFragment.getNextActivities() ) {
				Fragment inputFrag = getFragmentFromActivity(fragment, inputActivity); 
				if ( inputFrag != null ) {
					addToQueue(inputFrag, index + offSet );
					offSet++;
				}
			}
			
		}
		
		if ( !orderedFragments.contains( fragment ) ) {
			orderedFragments.add( fragment );
		}

		Fragment nextFragToVisit = fragmentQueue.poll();
		if ( nextFragToVisit != null ) {
			visitFragment(nextFragToVisit);
		}
		
	}
	
	private void sort() {
		int index = 0;
		FragmentComparator fc = new FragmentComparator();
		Collections.sort(orderedFragments, fc);
		for ( Fragment frag : orderedFragments ) {
			frag.setIndexOrder(index);
			index++;
		}
	}
}
