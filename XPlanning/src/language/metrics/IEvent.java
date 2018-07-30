package language.metrics;

import language.exceptions.AttributeNameNotFoundException;
import language.exceptions.VarNotFoundException;
import language.qfactors.IAction;

/**
 * {@link IEvent} is an interface to an event.
 * 
 * @author rsukkerd
 *
 */
public interface IEvent<E extends IAction, T extends IQFunctionDomain<E>> {

	public String getName();

	public T getQFunctionDomain();

	public boolean hasEventOccurred(Transition<E, T> transition)
			throws VarNotFoundException, AttributeNameNotFoundException;
}