package metrics;

import exceptions.AttributeNameNotFoundException;
import exceptions.QValueNotFound;
import exceptions.VarNameNotFoundException;
import factors.Transition;

/**
 * {@link IStandardMetricQFunction} is an interface to a Q_i function that characterizes a QA i using a standard metric.
 * 
 * @author rsukkerd
 *
 */
public interface IStandardMetricQFunction extends IQFunction {

	public double getValue(Transition trans)
			throws VarNameNotFoundException, QValueNotFound, AttributeNameNotFoundException;
}
