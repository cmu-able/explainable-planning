package explanation.analysis;

import java.util.Set;

import language.mdp.QSpace;

public class Explanation {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private PolicyInfo mSolnPolicyInfo;
	private QSpace mQSpace;
	private Set<Tradeoff> mTradeoffs;

	public Explanation(PolicyInfo solnPolicyInfo, QSpace qSpace, Set<Tradeoff> tradeoffs) {
		mSolnPolicyInfo = solnPolicyInfo;
		mQSpace = qSpace;
		mTradeoffs = tradeoffs;
	}

	public PolicyInfo getSolutionPolicyInfo() {
		return mSolnPolicyInfo;
	}

	public QSpace getQSpace() {
		return mQSpace;
	}

	public Set<Tradeoff> getTradeoffs() {
		return mTradeoffs;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof Explanation)) {
			return false;
		}
		Explanation explanation = (Explanation) obj;
		return explanation.mSolnPolicyInfo.equals(mSolnPolicyInfo) && explanation.mQSpace.equals(mQSpace)
				&& explanation.mTradeoffs.equals(mTradeoffs);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mSolnPolicyInfo.hashCode();
			result = 31 * result + mQSpace.hashCode();
			result = 31 * result + mTradeoffs.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
