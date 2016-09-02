package br.com.cmabreu.zodiac.gemini.core;

import java.util.List;

import br.com.cmabreu.zodiac.gemini.entity.Activity;
import br.com.cmabreu.zodiac.gemini.entity.Fragment;
import br.com.cmabreu.zodiac.gemini.entity.Instance;

public interface IInstanceGenerator {
	List<Instance> generateInstances( Activity activity, Fragment frag) throws Exception;
}
