package verbalization;

import java.util.HashMap;
import java.util.Map;

import language.metrics.IQFunction;

public class Vocabulary {

	private Map<IQFunction<?, ?>, String> mNouns = new HashMap<>();
	private Map<IQFunction<?, ?>, String> mVerbs = new HashMap<>();
	private Map<IQFunction<?, ?>, String> mSingularUnits = new HashMap<>();
	private Map<IQFunction<?, ?>, String> mPluralUnits = new HashMap<>();

	public void putNoun(IQFunction<?, ?> qFunction, String noun) {
		mNouns.put(qFunction, noun);
	}

	public void putVerb(IQFunction<?, ?> qFunction, String verb) {
		mVerbs.put(qFunction, verb);
	}

	public void putUnit(IQFunction<?, ?> qFunction, String singularUnit, String pluralUnit) {
		mSingularUnits.put(qFunction, singularUnit);
		mPluralUnits.put(qFunction, pluralUnit);
	}

	public String getNoun(IQFunction<?, ?> qFunction) {
		return mNouns.get(qFunction);
	}

	public String getVerb(IQFunction<?, ?> qFunction) {
		return mVerbs.get(qFunction);
	}

	public String getSingularUnit(IQFunction<?, ?> qFunction) {
		return mSingularUnits.get(qFunction);
	}

	public String getPluralUnit(IQFunction<?, ?> qFunction) {
		return mPluralUnits.get(qFunction);
	}
}
