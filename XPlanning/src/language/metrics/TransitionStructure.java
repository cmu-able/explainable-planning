package language.metrics;

import java.util.HashSet;
import java.util.Set;

import language.domain.models.ActionDefinition;
import language.domain.models.IAction;
import language.domain.models.IStateVarValue;
import language.domain.models.StateVarDefinition;

/**
 * {@link TransitionStructure} represents the structure of a transition. It contains a set of variable definitions in
 * the source state, a set in the destination state, and an action definition. It can be used to represent the domain of
 * a {@link IQFunction} -- among others. This is to facilitate PRISM translator in generating a reward structure for the
 * corresponding QA function.
 * 
 * @author rsukkerd
 *
 * @param <E>
 */
public class TransitionStructure<E extends IAction> implements ITransitionStructure<E> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Set<StateVarDefinition<? extends IStateVarValue>> mSrcVarDefs = new HashSet<>();
	private Set<StateVarDefinition<? extends IStateVarValue>> mDestVarDefs = new HashSet<>();
	private ActionDefinition<E> mActionDef;

	// Generic state variables of the transition
	// For PRISM translator
	private Set<StateVarDefinition<IStateVarValue>> mGenericSrcVarDefs = new HashSet<>();
	private Set<StateVarDefinition<IStateVarValue>> mGenericDestVarDefs = new HashSet<>();

	public void addSrcStateVarDef(StateVarDefinition<? extends IStateVarValue> stateVarDef) {
		mSrcVarDefs.add(stateVarDef);

		StateVarDefinition<IStateVarValue> genericVarDef = new StateVarDefinition<>(stateVarDef.getName(),
				stateVarDef.getPossibleValues());
		mGenericSrcVarDefs.add(genericVarDef);
	}

	public void addDestStateVarDef(StateVarDefinition<? extends IStateVarValue> stateVarDef) {
		mDestVarDefs.add(stateVarDef);

		StateVarDefinition<IStateVarValue> genericVarDef = new StateVarDefinition<>(stateVarDef.getName(),
				stateVarDef.getPossibleValues());
		mGenericDestVarDefs.add(genericVarDef);
	}

	public void setActionDef(ActionDefinition<E> actionDef) {
		mActionDef = actionDef;
	}

	@Override
	public Set<StateVarDefinition<IStateVarValue>> getSrcStateVarDefs() {
		return mGenericSrcVarDefs;
	}

	@Override
	public Set<StateVarDefinition<IStateVarValue>> getDestStateVarDefs() {
		return mGenericDestVarDefs;
	}

	@Override
	public ActionDefinition<E> getActionDef() {
		return mActionDef;
	}

	@Override
	public boolean containsSrcStateVarDef(StateVarDefinition<? extends IStateVarValue> srcVarDef) {
		return mSrcVarDefs.contains(srcVarDef);
	}

	@Override
	public boolean containsDestStateVarDef(StateVarDefinition<? extends IStateVarValue> destVarDef) {
		return mDestVarDefs.contains(destVarDef);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof TransitionStructure<?>)) {
			return false;
		}
		TransitionStructure<?> domain = (TransitionStructure<?>) obj;
		return domain.mSrcVarDefs.equals(mSrcVarDefs) && domain.mDestVarDefs.equals(mDestVarDefs)
				&& domain.mActionDef.equals(mActionDef);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mSrcVarDefs.hashCode();
			result = 31 * result + mDestVarDefs.hashCode();
			result = 31 * result + mActionDef.hashCode();
			hashCode = result;
		}
		return result;
	}

}
