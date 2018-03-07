package metrics;

import java.util.Map;

import exceptions.AttributeNameNotFoundException;
import exceptions.QValueNotFound;
import exceptions.VarNameNotFoundException;
import factors.Transition;

/**
 * {@link INonStandardMetricQFunction} is an interface to a Q_i function that characterizes a QA i using a non-standard
 * metric.
 * 
 * @author rsukkerd
 *
 */
public interface INonStandardMetricQFunction extends IQFunction {

	public Map<IEvent, Double> getMetric();

	public double getValue(Transition trans)
			throws VarNameNotFoundException, QValueNotFound, AttributeNameNotFoundException;
}
