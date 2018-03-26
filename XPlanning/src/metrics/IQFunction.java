package metrics;

import exceptions.AttributeNameNotFoundException;
import exceptions.QValueNotFound;
import exceptions.VarNotFoundException;

/**
 * {@link IQFunction} is an interface to a function Q_i: S x A x S -> R>=0 that characterizes the QA i at a single
 * {@link Transition}.
 * 
 * @author rsukkerd
 *
 */
public interface IQFunction {

	public double getValue(Transition trans)
			throws VarNotFoundException, QValueNotFound, AttributeNameNotFoundException;
}
