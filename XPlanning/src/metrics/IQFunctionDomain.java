package metrics;

import java.util.Set;

import factors.ActionDefinition;
import factors.IAction;
import factors.IStateVarValue;
import factors.StateVarDefinition;

public interface IQFunctionDomain<E extends IAction> {

	public Set<StateVarDefinition<IStateVarValue>> getSrcStateVarDefs();

	public Set<StateVarDefinition<IStateVarValue>> getDestStateVarDefs();

	public ActionDefinition<E> getActionDef();

	public boolean containsSrcStateVarDef(StateVarDefinition<? extends IStateVarValue> srcVarDef);

	public boolean containsDestStateVarDef(StateVarDefinition<? extends IStateVarValue> destVarDef);
}
