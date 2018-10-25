package explanation.analysis;

import java.util.HashMap;
import java.util.Map;

import language.domain.metrics.IQFunction;
import language.domain.metrics.ITransitionStructure;
import language.domain.models.IAction;
import language.mdp.QSpace;

public class Tradeoff {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private PolicyInfo mSolnPolicyInfo;
	private PolicyInfo mAltPolicyInfo;
	private Map<IQFunction<IAction, ITransitionStructure<IAction>>, Double> mQAValueGains = new HashMap<>();
	private Map<IQFunction<IAction, ITransitionStructure<IAction>>, Double> mQAValueLosses = new HashMap<>();
	private Map<IQFunction<IAction, ITransitionStructure<IAction>>, Double> mQACostGains = new HashMap<>();
	private Map<IQFunction<IAction, ITransitionStructure<IAction>>, Double> mQACostLosses = new HashMap<>();

	public Tradeoff(PolicyInfo solnPolicyInfo, PolicyInfo altPolicyInfo, QSpace qSpace) {
		mSolnPolicyInfo = solnPolicyInfo;
		mAltPolicyInfo = altPolicyInfo;
		computeTradeoff(qSpace);
	}

	private void computeTradeoff(QSpace qSpace) {
		for (IQFunction<IAction, ITransitionStructure<IAction>> qFunction : qSpace) {
			// QA values
			double solnQAValue = mSolnPolicyInfo.getQAValue(qFunction);
			double altQAValue = mAltPolicyInfo.getQAValue(qFunction);
			double diffQAValue = altQAValue - solnQAValue;

			// Scaled QA costs
			double solnScaledQACost = mSolnPolicyInfo.getScaledQACost(qFunction);
			double altScaledQACost = mAltPolicyInfo.getScaledQACost(qFunction);
			double diffScaledQACost = altScaledQACost - solnScaledQACost;

			if (altScaledQACost < solnScaledQACost) {
				mQAValueGains.put(qFunction, diffQAValue);
				mQACostGains.put(qFunction, diffScaledQACost);
			} else if (altScaledQACost > solnScaledQACost) {
				mQAValueLosses.put(qFunction, diffQAValue);
				mQACostLosses.put(qFunction, diffScaledQACost);
			}
		}
	}

	public PolicyInfo getSolutionPolicyInfo() {
		return mSolnPolicyInfo;
	}

	public PolicyInfo getAlternativePolicyInfo() {
		return mAltPolicyInfo;
	}

	public Map<IQFunction<IAction, ITransitionStructure<IAction>>, Double> getQAValueGains() {
		return mQAValueGains;
	}

	public Map<IQFunction<IAction, ITransitionStructure<IAction>>, Double> getQAValueLosses() {
		return mQAValueLosses;
	}

	public Map<IQFunction<IAction, ITransitionStructure<IAction>>, Double> getQACostGains() {
		return mQACostGains;
	}

	public Map<IQFunction<IAction, ITransitionStructure<IAction>>, Double> getQACostLosses() {
		return mQACostLosses;
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
				&& tradeoff.mQAValueGains.equals(mQAValueGains) && tradeoff.mQAValueLosses.equals(mQAValueLosses)
				&& tradeoff.mQACostGains.equals(mQACostGains) && tradeoff.mQACostLosses.equals(mQACostLosses);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mSolnPolicyInfo.hashCode();
			result = 31 * result + mAltPolicyInfo.hashCode();
			result = 31 * result + mQAValueGains.hashCode();
			result = 31 * result + mQAValueLosses.hashCode();
			result = 31 * result + mQACostGains.hashCode();
			result = 31 * result + mQACostLosses.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
