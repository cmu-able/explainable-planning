package metrics;

import java.util.Map;

import exceptions.AttributeNameNotFoundException;
import exceptions.QValueNotFound;
import exceptions.VarNameNotFoundException;

/**
 * {@link NonStandardMetricQFunction} represents a Q_i function that characterizes a QA i using a non-standard metric.
 * 
 * @author rsukkerd
 *
 */
public class NonStandardMetricQFunction implements IQFunction {
	
	private Map<IEvent, Double> mMetric;

	public NonStandardMetricQFunction(Map<IEvent, Double> metric) {
		mMetric = metric;
	}
	
	@Override
	public double getValue(Transition trans) 
			throws VarNameNotFoundException, QValueNotFound, AttributeNameNotFoundException {
		for (IEvent e : mMetric.keySet()) {
			if (e.isEventOccurred(trans)) {
				return mMetric.get(e);
			}
		}
		throw new QValueNotFound(trans);
	}
}
