package br.com.cmabreu.zodiac.gemini.core;

import java.util.ArrayList;
import java.util.List;

import br.com.cmabreu.zodiac.gemini.entity.Activity;
import br.com.cmabreu.zodiac.gemini.entity.Fragment;
import br.com.cmabreu.zodiac.gemini.entity.Instance;


public class SelectInstanceGenerator implements IInstanceGenerator {
	
	@Override
	public List<Instance> generateInstances(Activity activity, Fragment frag) throws Exception {
		debug( "Activity '" + activity.getTag() + "' allowed to run." );
		debug("generating instances...");		
		debug("'SELECT' type detected: single instance will be created. No need to fetch data.");

		InstanceCreator pc = new InstanceCreator();
		List<Instance> pipes = new ArrayList<Instance>();

		Instance pipe = pc.createInstance( activity, frag, null );
		pipes.add(pipe);
		
		return pipes;
	}
	
	private void debug( String s ) {
		Logger.getInstance().debug(this.getClass().getName(), s );
	}		

}
