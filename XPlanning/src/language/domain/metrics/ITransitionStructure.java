package language.domain.metrics;

import language.domain.models.ActionDefinition;
import language.domain.models.IAction;
import language.domain.models.IStateVarValue;
import language.domain.models.StateVarDefinition;
import language.mdp.StateVarClass;

/**
 * {@link ITransitionStructure} is an interface to the structure of a transition. It can be used to represent the domain
 * of a {@link IQFunction} -- among others. This is to facilitate PRISM translator in generating a reward structure for
 * the corresponding QA function.
 * 
 * @author rsukkerd
 *
 * @param <E>
 */
public interface ITransitionStructure<E extends IAction> {

	public StateVarClass getSrcStateVarClass();

	public StateVarClass getDestStateVarClass();

	public ActionDefinition<E> getActionDef();

	public boolean containsSrcStateVarDef(StateVarDefinition<? extends IStateVarValue> srcVarDef);

	public boolean containsDestStateVarDef(StateVarDefinition<? extends IStateVarValue> destVarDef);
}
