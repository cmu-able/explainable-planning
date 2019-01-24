package language.mdp;

import language.objectives.CostFunction;

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
	private StateVarTuple mInitialState;
	private StateVarTuple mGoal;
	private TransitionFunction mTransFunction;
	private QSpace mQSpace;
	private CostFunction mCostFunction;

	public XMDP(StateSpace stateSpace, ActionSpace actionSpace, StateVarTuple initialState, StateVarTuple goal,
			TransitionFunction transFunction, QSpace qSpace, CostFunction costFunction) {
		mStateSpace = stateSpace;
		mActionSpace = actionSpace;
		mInitialState = initialState;
		mGoal = goal; // nullable
		mTransFunction = transFunction;
		mQSpace = qSpace;
		mCostFunction = costFunction;
	}

	public StateSpace getStateSpace() {
		return mStateSpace;
	}

	public ActionSpace getActionSpace() {
		return mActionSpace;
	}

	public StateVarTuple getInitialState() {
		return mInitialState;
	}

	public StateVarTuple getGoal() {
		return mGoal;
	}

	public TransitionFunction getTransitionFunction() {
		return mTransFunction;
	}

	public QSpace getQSpace() {
		return mQSpace;
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
				&& mdp.mInitialState.equals(mInitialState)
				&& (mdp.mGoal == mGoal || mdp.mGoal != null && mdp.mGoal.equals(mGoal))
				&& mdp.mTransFunction.equals(mTransFunction) && mdp.mQSpace.equals(mQSpace)
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
			result = 31 * result + (mGoal == null ? 0 : mGoal.hashCode());
			result = 31 * result + mTransFunction.hashCode();
			result = 31 * result + mQSpace.hashCode();
			result = 31 * result + mCostFunction.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
