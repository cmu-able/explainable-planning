package language.metrics;

import java.util.HashSet;
import java.util.Set;

import language.qfactors.ActionDefinition;
import language.qfactors.IAction;
import language.qfactors.IStateVarValue;
import language.qfactors.StateVarDefinition;

/**
 * {@link TransitionDefinition} defines a set of transitions of particular state-variable and action types.
 * 
 * @author rsukkerd
 *
 */
public class TransitionDefinition {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private ActionDefinition<IAction> mActionDef;
	private Set<StateVarDefinition<IStateVarValue>> mSrcStateVarDefs = new HashSet<>();
	private Set<StateVarDefinition<IStateVarValue>> mDestStateVarDefs = new HashSet<>();

	public TransitionDefinition(ActionDefinition<? extends IAction> actionDef) {
		mActionDef = new ActionDefinition<>(actionDef.getName(), actionDef.getActions());
	}

	public void addSrcStateVarDef(StateVarDefinition<? extends IStateVarValue> stateVarDef) {
		StateVarDefinition<IStateVarValue> genericVarDef = new StateVarDefinition<>(stateVarDef.getName(),
				stateVarDef.getPossibleValues());
		mSrcStateVarDefs.add(genericVarDef);
	}

	public void addDestStateVarDef(StateVarDefinition<? extends IStateVarValue> stateVarDef) {
		StateVarDefinition<IStateVarValue> genericVarDef = new StateVarDefinition<>(stateVarDef.getName(),
				stateVarDef.getPossibleValues());
		mDestStateVarDefs.add(genericVarDef);
	}

	public Set<StateVarDefinition<IStateVarValue>> getSrcStateVarDefs() {
		return mSrcStateVarDefs;
	}

	public Set<StateVarDefinition<IStateVarValue>> getDestStateVarDefs() {
		return mDestStateVarDefs;
	}

	public ActionDefinition<IAction> getActionDef() {
		return mActionDef;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof TransitionDefinition)) {
			return false;
		}
		TransitionDefinition transDef = (TransitionDefinition) obj;
		return transDef.mActionDef.equals(mActionDef) && transDef.mSrcStateVarDefs.equals(mSrcStateVarDefs)
				&& transDef.mDestStateVarDefs.equals(mDestStateVarDefs);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mActionDef.hashCode();
			result = 31 * result + mSrcStateVarDefs.hashCode();
			result = 31 * result + mDestStateVarDefs.hashCode();
			hashCode = result;
		}
		return result;
	}
}
