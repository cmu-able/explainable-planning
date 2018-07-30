package language.metrics;

import java.util.Set;

import factors.ActionDefinition;
import factors.IAction;
import factors.IStateVarValue;
import factors.StateVarDefinition;

/**
 * {@link IQFunctionDomain} is an interface to the domain of a {@link IQFunction}. This is to facilitate PRISM
 * translator in generating a reward structure for the corresponding QA function.
 * 
 * @author rsukkerd
 *
 * @param <E>
 */
public interface IQFunctionDomain<E extends IAction> {

	public Set<StateVarDefinition<IStateVarValue>> getSrcStateVarDefs();

	public Set<StateVarDefinition<IStateVarValue>> getDestStateVarDefs();

	public ActionDefinition<E> getActionDef();

	public boolean containsSrcStateVarDef(StateVarDefinition<? extends IStateVarValue> srcVarDef);

	public boolean containsDestStateVarDef(StateVarDefinition<? extends IStateVarValue> destVarDef);
}
