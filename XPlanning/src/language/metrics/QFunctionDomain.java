package language.metrics;

import java.util.HashSet;
import java.util.Set;

import language.qfactors.ActionDefinition;
import language.qfactors.IAction;
import language.qfactors.IStateVarValue;
import language.qfactors.StateVarDefinition;

/**
 * {@link QFunctionDomain} represents the domain of a {@link IQFunction}. It contains a set of variable definitions in
 * the source state, a set in the destination state, and an action definition. This is to facilitate PRISM translator in
 * generating a reward structure for the corresponding QA function.
 * 
 * @author rsukkerd
 *
 * @param <E>
 */
public class QFunctionDomain<E extends IAction> implements IQFunctionDomain<E> {

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
		if (!(obj instanceof QFunctionDomain<?>)) {
			return false;
		}
		QFunctionDomain<?> domain = (QFunctionDomain<?>) obj;
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
