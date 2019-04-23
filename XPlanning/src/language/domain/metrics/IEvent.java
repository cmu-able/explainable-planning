package language.domain.metrics;

import language.domain.models.IAction;
import language.exceptions.AttributeNameNotFoundException;
import language.exceptions.VarNotFoundException;

/**
 * {@link IEvent} is an interface to an event.
 * 
 * @author rsukkerd
 *
 */
public interface IEvent<E extends IAction, T extends ITransitionStructure<E>> {

	public String getName();

	public T getTransitionStructure();

	public double getEventProbability(Transition<E, T> transition)
			throws VarNotFoundException, AttributeNameNotFoundException;
}
