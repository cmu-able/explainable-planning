package metrics;

import exceptions.AttributeNameNotFoundException;
import exceptions.VarNotFoundException;

/**
 * {@link IQFunction} is an interface to a function Q_i: S x A x S -> R>=0 that characterizes the QA i at a single
 * {@link Transition}.
 * 
 * @author rsukkerd
 *
 */
public interface IQFunction {

	public String getName();

	public TransitionDefinition getTransitionDefinition();

	public double getValue(Transition transition) throws VarNotFoundException, AttributeNameNotFoundException;
}
