package language.mdp;

import language.domain.models.IStateVarValue;
import language.domain.models.StateVarDefinition;

/**
 * {@link IStateVarClass} is an interface of a class of state variables that have some dependency relationship among
 * each other.
 * 
 * @author rsukkerd
 *
 */
public interface IStateVarClass extends Iterable<StateVarDefinition<IStateVarValue>> {

}
