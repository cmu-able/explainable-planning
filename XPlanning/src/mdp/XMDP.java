package mdp;

import java.util.Set;

import metrics.IQFunction;
import objectives.CostFunction;

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
	private StatePredicate mInitialState;
	private StatePredicate mGoal;
	private TransitionFunction mTransFunction;
	private Set<IQFunction> mQFunctions;
	private CostFunction mCostFunction;

	public XMDP(StateSpace stateVarDefs, ActionSpace actionSpace, StatePredicate initialState, StatePredicate goal,
			TransitionFunction transFunction, Set<IQFunction> qFunctions, CostFunction costFunction) {
		mStateSpace = stateVarDefs;
		mActionSpace = actionSpace;
		mInitialState = initialState;
		mGoal = goal;
		mTransFunction = transFunction;
		mQFunctions = qFunctions;
		mCostFunction = costFunction;
	}

	public StateSpace getStateSpace() {
		return mStateSpace;
	}

	public ActionSpace getActionSpace() {
		return mActionSpace;
	}

	public StatePredicate getInitialState() {
		return mInitialState;
	}

	public StatePredicate getGoal() {
		return mGoal;
	}

	public TransitionFunction getTransitionFunction() {
		return mTransFunction;
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
				&& mdp.mTransFunction.equals(mTransFunction) && mdp.mQFunctions.equals(mQFunctions)
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
			result = 31 * result + mTransFunction.hashCode();
			result = 31 * result + mQFunctions.hashCode();
			result = 31 * result + mCostFunction.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
