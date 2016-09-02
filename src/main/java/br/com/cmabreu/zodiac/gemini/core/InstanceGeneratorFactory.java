package br.com.cmabreu.zodiac.gemini.core;

import br.com.cmabreu.zodiac.gemini.types.ActivityType;

public class InstanceGeneratorFactory {
	
	
	public static IInstanceGenerator getGenerator( ActivityType type ) {
		
		switch (type) {
		case MAP:
			return new MapInstanceGenerator();
		case SELECT:
			return new SelectInstanceGenerator();
		case REDUCE:
			return new ReduceInstanceGenerator();
		case SPLIT_MAP:
			return new SplitInstanceGenerator();
		default:
			return null;
		}
	}

	
	
}
