package explanation.verbalization;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Vocabulary {

	private Map<String, String> mNouns = new HashMap<>();
	private Map<String, Map<String, String>> mCategoricalValues = new HashMap<>();
	private Map<String, String> mVerbs = new HashMap<>();
	private Map<String, String> mPrepositions = new HashMap<>();
	private Map<String, String> mSingularUnits = new HashMap<>();
	private Map<String, String> mPluralUnits = new HashMap<>();
	private Set<String> mOmitUnits = new HashSet<>();
	private String mPeriodUnit;

	public void putNoun(String qFunctionName, String noun) {
		mNouns.put(qFunctionName, noun);
	}

	public void putVerb(String qFunctionName, String verb) {
		mVerbs.put(qFunctionName, verb);
	}

	public void putPreposition(String qFunctionName, String preposition) {
		mPrepositions.put(qFunctionName, preposition);
	}

	public void putUnit(String qFunctionName, String singularUnit, String pluralUnit) {
		mSingularUnits.put(qFunctionName, singularUnit);
		mPluralUnits.put(qFunctionName, pluralUnit);
	}

	public void setOmitUnitWhenNounPresent(String qFunctionName) {
		mOmitUnits.add(qFunctionName);
	}

	public void putCategoricalValue(String qFunctionName, String eventName, String categoricalValue) {
		if (!mCategoricalValues.containsKey(qFunctionName)) {
			Map<String, String> catValues = new HashMap<>();
			catValues.put(eventName, categoricalValue);
			mCategoricalValues.put(qFunctionName, catValues);
		} else {
			mCategoricalValues.get(qFunctionName).put(eventName, categoricalValue);
		}
	}

	public void setPeriodUnit(String periodUnit) {
		mPeriodUnit = periodUnit;
	}

	public String getNoun(String qFunctionName) {
		return mNouns.get(qFunctionName);
	}

	public String getVerb(String qFunctionName) {
		return mVerbs.get(qFunctionName);
	}

	public String getPreposition(String qFunctionName) {
		return mPrepositions.get(qFunctionName);
	}

	public String getSingularUnit(String qFunctionName) {
		return mSingularUnits.get(qFunctionName);
	}

	public String getPluralUnit(String qFunctionName) {
		return mPluralUnits.get(qFunctionName);
	}

	public boolean omitUnitWhenNounPresent(String qFunctionName) {
		return mOmitUnits.contains(qFunctionName);
	}

	public String getCategoricalValue(String qFunctionName, String eventName) {
		return mCategoricalValues.get(qFunctionName).get(eventName);
	}

	public String getPeriodUnit() {
		return mPeriodUnit;
	}
}
