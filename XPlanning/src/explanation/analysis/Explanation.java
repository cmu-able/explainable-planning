package explanation.analysis;

import java.util.Set;

import language.mdp.QSpace;
import language.objectives.CostFunction;

public class Explanation {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private PolicyInfo mSolnPolicyInfo;
	private Set<Tradeoff> mTradeoffs;

	public Explanation(PolicyInfo solnPolicyInfo, Set<Tradeoff> tradeoffs) {
		mSolnPolicyInfo = solnPolicyInfo;
		mTradeoffs = tradeoffs; // this is be an empty set when there is no tradeoff (i.e., the solution policy is an
								// absolute optimal one)
	}

	public PolicyInfo getSolutionPolicyInfo() {
		return mSolnPolicyInfo;
	}

	public QSpace getQSpace() {
		return mSolnPolicyInfo.getXMDP().getQSpace();
	}

	public CostFunction getCostFunction() {
		return mSolnPolicyInfo.getXMDP().getCostFunction();
	}

	/**
	 * This is be an empty set when there is no tradeoff (i.e., the solution policy is an absolute optimal one).
	 * 
	 * @return A set of tradeoffs
	 */
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
		return explanation.mSolnPolicyInfo.equals(mSolnPolicyInfo) && explanation.mTradeoffs.equals(mTradeoffs);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mSolnPolicyInfo.hashCode();
			result = 31 * result + mTradeoffs.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
