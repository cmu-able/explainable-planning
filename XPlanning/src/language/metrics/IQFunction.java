package language.metrics;

import language.exceptions.AttributeNameNotFoundException;
import language.exceptions.VarNotFoundException;
import language.qfactors.IAction;

/**
 * {@link IQFunction} is an interface to a function Q_i: S x A x S -> R>=0 that characterizes the QA i at a single
 * {@link Transition}.
 * 
 * @author rsukkerd
 *
 */
public interface IQFunction<E extends IAction, T extends IQFunctionDomain<E>> {

	public String getName();

	public T getQFunctionDomain();

	public double getValue(Transition<E, T> transition) throws VarNotFoundException, AttributeNameNotFoundException;
}
