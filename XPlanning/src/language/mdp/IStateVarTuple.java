package language.mdp;

import language.exceptions.VarNotFoundException;
import language.qfactors.IStateVarValue;
import language.qfactors.StateVar;
import language.qfactors.StateVarDefinition;

/**
 * {@link IStateVarTuple} is a tuple (v1,...,vk) of state variables. It defines at most 1 allowable value for each state
 * variable. Undefined state variables can have any value.
 * 
 * @author rsukkerd
 *
 */
public interface IStateVarTuple extends Iterable<StateVar<IStateVarValue>> {

	public boolean contains(StateVarDefinition<? extends IStateVarValue> stateVarDef);

	public <E extends IStateVarValue> E getStateVarValue(Class<E> valueType, StateVarDefinition<E> stateVarDef)
			throws VarNotFoundException;
}
