package examples.clinicscheduling.metrics;

import examples.clinicscheduling.models.ABP;
import examples.clinicscheduling.models.ScheduleAction;
import language.domain.metrics.IStandardMetricQFunction;
import language.domain.metrics.Transition;
import language.exceptions.AttributeNameNotFoundException;
import language.exceptions.VarNotFoundException;

public class SwitchABPQFunction implements IStandardMetricQFunction<ScheduleAction, SwitchABPDomain> {

	public static final String NAME = "switching-ABP";

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private SwitchABPDomain mDomain;
	private double mSwitchABPCostFactor;

	public SwitchABPQFunction(SwitchABPDomain domain, double switchABPCostFactor) {
		mDomain = domain;
		mSwitchABPCostFactor = switchABPCostFactor;
	}

	public double getSwitchABPCostFactor() {
		return mSwitchABPCostFactor;
	}

	/**
	 * Switching ABP cost = S * |w - a|, where:
	 * 
	 * S = switching ABP cost factor,
	 * 
	 * w = current ABP,
	 * 
	 * a = new ABP.
	 */
	@Override
	public double getValue(Transition<ScheduleAction, SwitchABPDomain> transition)
			throws VarNotFoundException, AttributeNameNotFoundException {
		ABP currABP = mDomain.getCurrentABP(transition);
		ABP newABP = mDomain.getNewABP(transition);
		int w = currABP.getValue();
		int a = newABP.getValue();
		return mSwitchABPCostFactor * Math.abs(w - a);
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public SwitchABPDomain getTransitionStructure() {
		return mDomain;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof SwitchABPQFunction)) {
			return false;
		}
		SwitchABPQFunction qFunction = (SwitchABPQFunction) obj;
		return qFunction.mDomain.equals(mDomain)
				&& Double.compare(qFunction.mSwitchABPCostFactor, mSwitchABPCostFactor) == 0;
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mDomain.hashCode();
			result = 31 * result + Double.hashCode(mSwitchABPCostFactor);
			hashCode = result;
		}
		return result;
	}

}
