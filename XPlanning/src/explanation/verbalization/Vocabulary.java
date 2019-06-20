package explanation.verbalization;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import language.domain.metrics.IEvent;
import language.domain.metrics.IQFunction;
import language.domain.metrics.NonStandardMetricQFunction;

public class Vocabulary {

	private Map<IQFunction<?, ?>, String> mNouns = new HashMap<>();
	private Map<NonStandardMetricQFunction<?, ?, ?>, Map<IEvent<?, ?>, String>> mCategoricalValues = new HashMap<>();
	private Map<IQFunction<?, ?>, String> mVerbs = new HashMap<>();
	private Map<IQFunction<?, ?>, String> mPrepositions = new HashMap<>();
	private Map<IQFunction<?, ?>, String> mSingularUnits = new HashMap<>();
	private Map<IQFunction<?, ?>, String> mPluralUnits = new HashMap<>();
	private Set<IQFunction<?, ?>> mOmitUnits = new HashSet<>();
	private String mPeriodUnit;

	public void putNoun(IQFunction<?, ?> qFunction, String noun) {
		mNouns.put(qFunction, noun);
	}

	public void putVerb(IQFunction<?, ?> qFunction, String verb) {
		mVerbs.put(qFunction, verb);
	}

	public void putPreposition(IQFunction<?, ?> qFunction, String preposition) {
		mPrepositions.put(qFunction, preposition);
	}

	public void putUnit(IQFunction<?, ?> qFunction, String singularUnit, String pluralUnit) {
		mSingularUnits.put(qFunction, singularUnit);
		mPluralUnits.put(qFunction, pluralUnit);
	}

	public void setOmitUnitWhenNounPresent(IQFunction<?, ?> qFunction) {
		mOmitUnits.add(qFunction);
	}

	public void putCategoricalValue(NonStandardMetricQFunction<?, ?, ?> qFunction, IEvent<?, ?> event,
			String categoricalValue) {
		if (!mCategoricalValues.containsKey(qFunction)) {
			Map<IEvent<?, ?>, String> catValues = new HashMap<>();
			catValues.put(event, categoricalValue);
			mCategoricalValues.put(qFunction, catValues);
		} else {
			mCategoricalValues.get(qFunction).put(event, categoricalValue);
		}
	}

	public void setPeriodUnit(String periodUnit) {
		mPeriodUnit = periodUnit;
	}

	public String getNoun(IQFunction<?, ?> qFunction) {
		return mNouns.get(qFunction);
	}

	public String getVerb(IQFunction<?, ?> qFunction) {
		return mVerbs.get(qFunction);
	}

	public String getPreposition(IQFunction<?, ?> qFunction) {
		return mPrepositions.get(qFunction);
	}

	public String getSingularUnit(IQFunction<?, ?> qFunction) {
		return mSingularUnits.get(qFunction);
	}

	public String getPluralUnit(IQFunction<?, ?> qFunction) {
		return mPluralUnits.get(qFunction);
	}

	public boolean omitUnitWhenNounPresent(IQFunction<?, ?> qFunction) {
		return mOmitUnits.contains(qFunction);
	}

	public String getCategoricalValue(NonStandardMetricQFunction<?, ?, ?> qFunction, IEvent<?, ?> event) {
		return mCategoricalValues.get(qFunction).get(event);
	}

	public String getPeriodUnit() {
		return mPeriodUnit;
	}
}
