package models.explanation;

import explanation.analysis.PolicyInfo;
import explanation.analysis.Tradeoff;

public class HPolicyExplanation {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private HPolicyTag mHPolicyTag;
	private Tradeoff mTradeoff;

	public HPolicyExplanation(HPolicyTag hPolicyTag, Tradeoff tradeoff) {
		mHPolicyTag = hPolicyTag;
		mTradeoff = tradeoff;
	}

	public HPolicyTag getHPolicyTag() {
		return mHPolicyTag;
	}

	public PolicyInfo getQueryPolicyInfo() {
		return mTradeoff.getSolutionPolicyInfo();
	}

	public PolicyInfo getHPolicyInfo() {
		return mTradeoff.getAlternativePolicyInfo();
	}

	public Tradeoff getTradeoff() {
		return mTradeoff;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof HPolicyExplanation)) {
			return false;
		}
		HPolicyExplanation explanation = (HPolicyExplanation) obj;
		return explanation.mHPolicyTag == mHPolicyTag && explanation.mTradeoff.equals(mTradeoff);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mHPolicyTag.hashCode();
			result = 31 * result + mTradeoff.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
