package factors;

import java.util.Iterator;

/**
 * {@link IChangeSet} is an interface to a change set of dependent state variables. This change set is part of a
 * factored PSO action representation.
 * 
 * @author rsukkerd
 *
 */
public interface IChangeSet extends Iterable<StateVar<IStateVarValue>>, Iterator<StateVar<IStateVarValue>> {

}
