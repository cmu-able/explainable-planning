package examples.mobilerobot.metrics;

import examples.mobilerobot.factors.Area;
import examples.mobilerobot.factors.Location;
import examples.mobilerobot.factors.MoveToAction;
import exceptions.AttributeNameNotFoundException;
import exceptions.VarNotFoundException;
import factors.ActionDefinition;
import factors.StateVarDefinition;
import metrics.EventBasedMetric;
import metrics.INonStandardMetricQFunction;
import metrics.NonStandardMetricQFunction;
import metrics.Transition;
import metrics.TransitionDefinition;

/**
 * {@link IntrusivenessQFunction} determines the intrusiveness of the robot of a single transition.
 * 
 * @author rsukkerd
 *
 */
public class IntrusivenessQFunction implements INonStandardMetricQFunction {

	private static final String NAME = "intrusiveness";
	private static final double NON_INTRUSIVE_PENALTY = 0;
	private static final double SEMI_INTRUSIVE_PEANLTY = 1;
	private static final double VERY_INTRUSIVE_PENALTY = 3;

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private NonStandardMetricQFunction mNonStdQFn;

	public IntrusivenessQFunction(ActionDefinition<MoveToAction> moveTo, StateVarDefinition<Location> rLocDestDef) {
		TransitionDefinition transitionDef = new TransitionDefinition(moveTo);
		transitionDef.addDestStateVarDef(rLocDestDef);
		EventBasedMetric metric = new EventBasedMetric(transitionDef);
		metric.put(new IntrusiveMoveEvent(moveTo, rLocDestDef, Area.PUBLIC), NON_INTRUSIVE_PENALTY);
		metric.put(new IntrusiveMoveEvent(moveTo, rLocDestDef, Area.SEMI_PRIVATE), SEMI_INTRUSIVE_PEANLTY);
		metric.put(new IntrusiveMoveEvent(moveTo, rLocDestDef, Area.PRIVATE), VERY_INTRUSIVE_PENALTY);
		mNonStdQFn = new NonStandardMetricQFunction(NAME, metric);
	}

	@Override
	public EventBasedMetric getMetric() {
		return mNonStdQFn.getMetric();
	}

	@Override
	public String getName() {
		return mNonStdQFn.getName();
	}

	@Override
	public TransitionDefinition getTransitionDefinition() {
		return mNonStdQFn.getTransitionDefinition();
	}

	@Override
	public double getValue(Transition trans) throws VarNotFoundException, AttributeNameNotFoundException {
		return mNonStdQFn.getValue(trans);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof IntrusivenessQFunction)) {
			return false;
		}
		IntrusivenessQFunction qFun = (IntrusivenessQFunction) obj;
		return qFun.mNonStdQFn.equals(mNonStdQFn);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mNonStdQFn.hashCode();
			hashCode = result;
		}
		return result;
	}

}
