package br.com.cmabreu.zodiac.gemini.misc.json;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import br.com.cmabreu.zodiac.gemini.core.ActivityFactory;
import br.com.cmabreu.zodiac.gemini.entity.Activity;
import br.com.cmabreu.zodiac.gemini.entity.Experiment;
import br.com.cmabreu.zodiac.gemini.entity.Relation;
import br.com.cmabreu.zodiac.gemini.types.ActivityType;

public class JsonElementConversor {
	private Set<Activity> activities = new TreeSet<Activity>();
	
	private Activity getActivity(String tag) {
		for ( Activity act : activities ) {
			if ( act.getTag().equals( tag ) ) {
				return act;
			}
		}
		return null;
	}

	public boolean validadeGraphStructure( String jsonString ) {
		boolean retorno = true;
		/*
		List<JsonElement> elements = convertToElements( jsonString );
		for( JsonElement ele : elements ) {
			if ( ele.getGroup().equals("nodes") ) {
				if ( !isConnected( ele.getData().getId(), elements ) ) {
					retorno = false;
				}
			}
		}
		*/
		return retorno;
	}

	public List<JsonElement> convertToElements( String jsonString ) {
		Gson gson = new Gson();
		Type listType = new TypeToken<ArrayList<JsonElement>>() {  }.getType();
		List<JsonElement> elements = gson.fromJson(jsonString, listType);
		return elements;
	}
	
	
	private boolean validNode( String nodeType ) {
		boolean result = ( !nodeType.equals("TRGTABLE") && !nodeType.equals("SRCTABLE") );
		return result;
	}
	
	public Set<Activity> convert (Experiment experiment) throws Exception {
		String jsonString = experiment.getActivitiesSpecs();
		Set<Activity> list = new TreeSet<Activity>();
		
		if ( !validadeGraphStructure( jsonString ) ){
			throw new Exception("The Workflow Graph Definition is invalid.");
		}
		
		List<JsonElement> elements = convertToElements( jsonString );

		if ( elements != null ) {
		
			for ( JsonElement element : elements ) {
				if ( element.getGroup().equals("nodes") && validNode( element.getData().getName() ) ) {
					Activity activity = ActivityFactory.getActivity( ActivityType.valueOf( element.getData().getName() ) );
					
					activity.setCommand( element.getData().getActivation() );
					activity.setExecutorAlias( element.getData().getActivation() );
					activity.setDescription( element.getData().getDescription() );
					activity.setTag( element.getData().getId() );
					
					if ( element.getData().getInputId() != null ) {
						
						String[] inputRelations = element.getData().getInputId().split(",");
						String[] inputRelationNames = element.getData().getInput().split(" ");
						
						if ( inputRelations.length > 0 ) {
							for ( int cc = 0; cc < inputRelations.length; cc++ ) {
								String inputTableId = inputRelations[cc];
								Relation tbl = new Relation();
								tbl.setIdTable( Integer.valueOf( inputTableId ) );
								tbl.setName( inputRelationNames[cc] );
								activity.addInputRelation( tbl );
							}
						}
						
						
					}
					if ( element.getData().getOutputId() != null ) {
						Relation tbl = new Relation();
						tbl.setIdTable( Integer.valueOf( element.getData().getOutputId() ) );
						tbl.setName( element.getData().getOutput() );
						activity.setOutputRelation( tbl );
					}
					activities.add(activity);
					
				}
			}
			
			for ( JsonElement element : elements ) {
				if ( element.getGroup().equals("edges") ) {
					String source = element.getData().getSource();
					String target = element.getData().getTarget();
					Activity actSource = getActivity(source);
					Activity actTarget = getActivity(target);
					if ( (actTarget!=null) && (actSource!=null) ) {
						actSource.addNextActivity( actTarget );
						actTarget.addPreviousActivity( actSource );
					}
					
				}
			}

			list.addAll(activities);
		} else {
			throw new Exception("No activities found");
		}
		
		return list;
	}

}
