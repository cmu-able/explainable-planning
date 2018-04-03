package metrics;

import exceptions.AttributeNameNotFoundException;
import exceptions.VarNotFoundException;

/**
 * {@link IEvent} is an interface to an event.
 * 
 * @author rsukkerd
 *
 */
public interface IEvent {

	public TransitionDefinition getTransitionDefinition();

	public boolean hasEventOccurred(Transition trans) throws VarNotFoundException, AttributeNameNotFoundException;
}
