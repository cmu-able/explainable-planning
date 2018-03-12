package mdp;

import java.util.Map;
import java.util.Set;

import factors.IAction;
import factors.IFactoredPSO;
import factors.IStateVarValue;
import factors.StateVar;
import factors.StateVarDefinition;
import metrics.IQFunction;
import preferences.CostFunction;

/**
 * {@link XMDP} is an Explainable MDP.
 * 
 * @author rsukkerd
 *
 */
public class XMDP {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Set<StateVarDefinition<IStateVarValue>> mStateVarDefs;
	private Set<StateVar<IStateVarValue>> mInitialState;
	private Set<StateVar<IStateVarValue>> mGoals;
	private Set<IAction> mActions;
	private Map<IAction, IFactoredPSO> mTransitions;
	private Set<IQFunction> mQFunctions;
	private CostFunction mCostFunction;

	public XMDP(Set<StateVarDefinition<IStateVarValue>> stateVarDefs, Set<StateVar<IStateVarValue>> initialState,
			Set<StateVar<IStateVarValue>> goals, Set<IAction> actions, Map<IAction, IFactoredPSO> transitions,
			Set<IQFunction> qFunctions, CostFunction costFunction) {
		mStateVarDefs = stateVarDefs;
		mInitialState = initialState;
		mGoals = goals;
		mActions = actions;
		mTransitions = transitions;
		mQFunctions = qFunctions;
		mCostFunction = costFunction;
	}

	public Set<StateVarDefinition<IStateVarValue>> getStateVarDefs() {
		return mStateVarDefs;
	}

	public Set<StateVar<IStateVarValue>> getInitialState() {
		return mInitialState;
	}

	public Set<StateVar<IStateVarValue>> getGoals() {
		return mGoals;
	}

	public Set<IAction> getActions() {
		return mActions;
	}

	public IFactoredPSO getTransitionFunction(IAction action) {
		return mTransitions.get(action);
	}

	public Set<IQFunction> getQFunctions() {
		return mQFunctions;
	}

	public CostFunction getCostFunction() {
		return mCostFunction;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof XMDP)) {
			return false;
		}
		XMDP mdp = (XMDP) obj;
		return mdp.mStateVarDefs.equals(mStateVarDefs) && mdp.mInitialState.equals(mInitialState)
				&& mdp.mGoals.equals(mGoals) && mdp.mActions.equals(mActions) && mdp.mTransitions.equals(mTransitions)
				&& mdp.mQFunctions.equals(mQFunctions) && mdp.mCostFunction.equals(mCostFunction);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mStateVarDefs.hashCode();
			result = 31 * result + mInitialState.hashCode();
			result = 31 * result + mGoals.hashCode();
			result = 31 * result + mActions.hashCode();
			result = 31 * result + mTransitions.hashCode();
			result = 31 * result + mQFunctions.hashCode();
			result = 31 * result + mCostFunction.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
