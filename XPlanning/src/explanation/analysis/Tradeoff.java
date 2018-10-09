package explanation.analysis;

import java.util.HashMap;
import java.util.Map;

import language.domain.models.IAction;
import language.mdp.QSpace;
import language.metrics.IQFunction;
import language.metrics.ITransitionStructure;
import language.objectives.AttributeCostFunction;
import language.objectives.CostFunction;

public class Tradeoff {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private PolicyInfo mSolnPolicyInfo;
	private PolicyInfo mAltPolicyInfo;
	private Map<IQFunction<IAction, ITransitionStructure<IAction>>, Double> mQAGains = new HashMap<>();
	private Map<IQFunction<IAction, ITransitionStructure<IAction>>, Double> mQALosses = new HashMap<>();

	public Tradeoff(PolicyInfo solnPolicyInfo, PolicyInfo altPolicyInfo, QSpace qSpace, CostFunction costFunction) {
		mSolnPolicyInfo = solnPolicyInfo;
		mAltPolicyInfo = altPolicyInfo;
		computeTradeoff(qSpace, costFunction);
	}

	private void computeTradeoff(QSpace qSpace, CostFunction costFunction) {
		for (IQFunction<IAction, ITransitionStructure<IAction>> qFunction : qSpace) {
			double solnQAValue = mSolnPolicyInfo.getQAValue(qFunction);
			double altQAValue = mAltPolicyInfo.getQAValue(qFunction);
			double diffQAValue = altQAValue - solnQAValue;
			AttributeCostFunction<IQFunction<IAction, ITransitionStructure<IAction>>> attrCostFunc = costFunction
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

	public Map<IQFunction<IAction, ITransitionStructure<IAction>>, Double> getQAGains() {
		return mQAGains;
	}

	public Map<IQFunction<IAction, ITransitionStructure<IAction>>, Double> getQALosses() {
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
