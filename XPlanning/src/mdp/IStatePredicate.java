package mdp;

import exceptions.VarNotFoundException;
import factors.IStateVarValue;
import factors.StateVar;
import factors.StateVarDefinition;

/**
 * {@link IStatePredicate} defines at most 1 allowable value for each state variable.
 * 
 * @author rsukkerd
 *
 */
public interface IStatePredicate extends Iterable<StateVar<IStateVarValue>> {

	public <E extends IStateVarValue> E getStateVarValue(Class<E> valueType, StateVarDefinition<E> stateVarDef)
			throws VarNotFoundException;
}
