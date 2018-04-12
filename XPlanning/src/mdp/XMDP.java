package mdp;

import java.util.Map;
import java.util.Set;

import factors.IAction;
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

	private StateSpace mStateSpace;
	private ActionSpace mActionSpace;
	private State mInitialState;
	private State mGoal;
	private Map<IAction, IFactoredPSO> mTransitions;
	private Set<IQFunction> mQFunctions;
	private CostFunction mCostFunction;

	public XMDP(StateSpace stateVarDefs, ActionSpace actionSpace, State initialState, State goal,
			Map<IAction, IFactoredPSO> transitions, Set<IQFunction> qFunctions, CostFunction costFunction) {
		mStateSpace = stateVarDefs;
		mActionSpace = actionSpace;
		mInitialState = initialState;
		mGoal = goal;
		mTransitions = transitions;
		mQFunctions = qFunctions;
		mCostFunction = costFunction;
	}

	public StateSpace getStateSpace() {
		return mStateSpace;
	}

	public ActionSpace getActionSpace() {
		return mActionSpace;
	}

	public State getInitialState() {
		return mInitialState;
	}

	public State getGoal() {
		return mGoal;
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
		return mdp.mStateSpace.equals(mStateSpace) && mdp.mActionSpace.equals(mActionSpace)
				&& mdp.mInitialState.equals(mInitialState) && mdp.mGoal.equals(mGoal)
				&& mdp.mTransitions.equals(mTransitions) && mdp.mQFunctions.equals(mQFunctions)
				&& mdp.mCostFunction.equals(mCostFunction);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mStateSpace.hashCode();
			result = 31 * result + mActionSpace.hashCode();
			result = 31 * result + mInitialState.hashCode();
			result = 31 * result + mGoal.hashCode();
			result = 31 * result + mTransitions.hashCode();
			result = 31 * result + mQFunctions.hashCode();
			result = 31 * result + mCostFunction.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
