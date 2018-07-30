package analysis;

import java.util.HashMap;
import java.util.Map;

import factors.IAction;
import language.metrics.IQFunction;
import language.metrics.IQFunctionDomain;
import language.objectives.AttributeCostFunction;
import language.objectives.CostFunction;
import mdp.QSpace;

public class Tradeoff {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private PolicyInfo mSolnPolicyInfo;
	private PolicyInfo mAltPolicyInfo;
	private Map<IQFunction<IAction, IQFunctionDomain<IAction>>, Double> mQAGains = new HashMap<>();
	private Map<IQFunction<IAction, IQFunctionDomain<IAction>>, Double> mQALosses = new HashMap<>();

	public Tradeoff(PolicyInfo solnPolicyInfo, PolicyInfo altPolicyInfo, QSpace qSpace, CostFunction costFunction) {
		mSolnPolicyInfo = solnPolicyInfo;
		mAltPolicyInfo = altPolicyInfo;
		computeTradeoff(qSpace, costFunction);
	}

	private void computeTradeoff(QSpace qSpace, CostFunction costFunction) {
		for (IQFunction<IAction, IQFunctionDomain<IAction>> qFunction : qSpace) {
			double solnQAValue = mSolnPolicyInfo.getQAValue(qFunction);
			double altQAValue = mAltPolicyInfo.getQAValue(qFunction);
			double diffQAValue = altQAValue - solnQAValue;
			AttributeCostFunction<IQFunction<IAction, IQFunctionDomain<IAction>>> attrCostFunc = costFunction
					.getAttributeCostFunction(qFunction);
			double solnQACost = attrCostFunc.getCost(solnQAValue);
			double altQACost = attrCostFunc.getCost(altQAValue);
			if (altQACost < solnQACost) {
				mQAGains.put(qFunction, diffQAValue);
			} else if (altQACost > solnQACost) {
				mQALosses.put(qFunction, diffQAValue);
			}
		}
	}

	public PolicyInfo getSolutionPolicyInfo() {
		return mSolnPolicyInfo;
	}

	public PolicyInfo getAlternativePolicyInfo() {
		return mAltPolicyInfo;
	}

	public Map<IQFunction<IAction, IQFunctionDomain<IAction>>, Double> getQAGains() {
		return mQAGains;
	}

	public Map<IQFunction<IAction, IQFunctionDomain<IAction>>, Double> getQALosses() {
		return mQALosses;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof Tradeoff)) {
			return false;
		}
		Tradeoff tradeoff = (Tradeoff) obj;
		return tradeoff.mSolnPolicyInfo.equals(mSolnPolicyInfo) && tradeoff.mAltPolicyInfo.equals(mAltPolicyInfo)
				&& tradeoff.mQAGains.equals(mQAGains) && tradeoff.mQALosses.equals(mQALosses);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mSolnPolicyInfo.hashCode();
			result = 31 * result + mAltPolicyInfo.hashCode();
			result = 31 * result + mQAGains.hashCode();
			result = 31 * result + mQALosses.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
