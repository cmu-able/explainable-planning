package examples.mobilerobot.metrics;

import java.util.HashMap;
import java.util.Map;

import exceptions.AttributeNameNotFoundException;
import exceptions.QValueNotFound;
import exceptions.VarNameNotFoundException;
import metrics.IEvent;
import metrics.IQFunction;
import metrics.NonStandardMetricQFunction;
import metrics.Transition;

/**
 * {@link IntrusivenessQFunction} determines the intrusiveness of the robot of a single transition.
 * 
 * @author rsukkerd
 *
 */
public class IntrusivenessQFunction implements IQFunction {

	private double NON_INTRUSIVE_PENALTY = 0;
	private double SEMI_INTRUSIVE_PEANLTY = 1;
	private double VERY_INTRUSIVE_PENALTY = 3;
	
	private NonStandardMetricQFunction mNonStdQFn;
	
	public IntrusivenessQFunction() {
		Map<IEvent, Double> metric = new HashMap<>();
		metric.put(new NonIntrusiveMoveEvent(), NON_INTRUSIVE_PENALTY);
		metric.put(new SemiIntrusiveMoveEvent(), SEMI_INTRUSIVE_PEANLTY);
		metric.put(new VeryIntrusiveMoveEvent(), VERY_INTRUSIVE_PENALTY);
		mNonStdQFn = new NonStandardMetricQFunction(metric);
	}
	
	@Override
	public double getValue(Transition trans)
			throws VarNameNotFoundException, QValueNotFound, AttributeNameNotFoundException {
		return mNonStdQFn.getValue(trans);
	}

}
