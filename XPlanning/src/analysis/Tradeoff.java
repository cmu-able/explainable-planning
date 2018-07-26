package analysis;

import java.util.HashMap;
import java.util.Map;

import exceptions.ResultParsingException;
import exceptions.XMDPException;
import factors.IAction;
import metrics.IQFunction;
import metrics.IQFunctionDomain;
import objectives.AttributeCostFunction;
import objectives.CostFunction;
import policy.Policy;
import prism.PrismException;
import prismconnector.PrismConnector;

public class Tradeoff {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private PrismConnector mConnector;
	private Policy mSolnPolicy;
	private Policy mAltPolicy;
	private Map<IQFunction<IAction, IQFunctionDomain<IAction>>, Double> mQAGains = new HashMap<>();
	private Map<IQFunction<IAction, IQFunctionDomain<IAction>>, Double> mQALosses = new HashMap<>();

	public Tradeoff(PrismConnector prismConnector, Policy solnPolicy, Policy altPolicy)
			throws ResultParsingException, XMDPException, PrismException {
		mConnector = prismConnector;
		mSolnPolicy = solnPolicy;
		mAltPolicy = altPolicy;
		computeTradeoff();
	}

	private void computeTradeoff() throws ResultParsingException, XMDPException, PrismException {
		CostFunction costFunction = mConnector.getXMDP().getCostFunction();
		for (IQFunction<IAction, IQFunctionDomain<IAction>> qFunction : mConnector.getXMDP().getQSpace()) {
			double solnQAValue = mConnector.getQAValue(mSolnPolicy, qFunction);
			double altQAValue = mConnector.getQAValue(mAltPolicy, qFunction);
			double diffQAValue = Math.abs(solnQAValue - altQAValue);
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

	public Policy getSolutionPolicy() {
		return mSolnPolicy;
	}

	public Policy getAlternativePolicy() {
		return mAltPolicy;
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
		return tradeoff.mSolnPolicy.equals(mSolnPolicy) && tradeoff.mAltPolicy.equals(mAltPolicy)
				&& tradeoff.mQAGains.equals(mQAGains) && tradeoff.mQALosses.equals(mQALosses);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mSolnPolicy.hashCode();
			result = 31 * result + mAltPolicy.hashCode();
			result = 31 * result + mQAGains.hashCode();
			result = 31 * result + mQALosses.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
