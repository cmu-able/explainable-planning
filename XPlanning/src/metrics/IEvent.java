package metrics;

import exceptions.AttributeNameNotFoundException;
import exceptions.VarNameNotFoundException;
import mdp.Transition;

/**
 * {@link IEvent} is an interface to an event.
 * 
 * @author rsukkerd
 *
 */
public interface IEvent {

	public boolean hasEventOccurred(Transition trans) throws VarNameNotFoundException, AttributeNameNotFoundException;
}
