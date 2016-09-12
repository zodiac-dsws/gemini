package br.com.cmabreu.zodiac.gemini.core;

import br.com.cmabreu.zodiac.gemini.entity.Activity;
import br.com.cmabreu.zodiac.gemini.types.ActivityType;

public class ActivityFactory {
	
	public static Activity getActivity( ActivityType type ) {
		Activity act = new Activity( );
		act.setType(type);
		return act;
	}

}
