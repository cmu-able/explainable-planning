package solver.gurobiconnector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class ExplicitMDP {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private int mNumStates;
	private int mNumActions;
	private List<String> mIndexedActions;
	private CostType mCostType;
	private double[][][] mTransProbs;
	private double[][] mTransCosts;
	private double[] mStateCosts;

	public ExplicitMDP(int numStates, int numActions, Set<String> actionNames, CostType costType) {
		mNumStates = numStates;
		mNumActions = numActions;
		mIndexedActions = sortActions(actionNames);
		mCostType = costType;
		mTransProbs = new double[numStates][numActions][numStates];
		if (costType == CostType.TRANSITION_COST) {
			mTransCosts = new double[numStates][numActions];
		} else if (costType == CostType.STATE_COST) {
			mStateCosts = new double[numStates];
		}
	}

	/**
	 * This is to ensure 2 instances of {@link ExplicitMDP} with the same structure are considered equal, by setting a
	 * unique assignment of action names -> action indices.
	 * 
	 * @param actionNames
	 * @return A list of action names sorted lexicographically, ignoring case.
	 */
	private List<String> sortActions(Set<String> actionNames) {
		List<String> sortedActionNames = new ArrayList<>(actionNames);
		sortedActionNames.sort((actionName1, actionName2) -> actionName1.compareToIgnoreCase(actionName2));
		return sortedActionNames;
	}

	/**
	 * Add a transition probability: Pr(s'|s,a) = p.
	 * 
	 * @param srcState
	 * @param actionName
	 * @param destState
	 * @param probability
	 */
	public void addTransitionProbability(int srcState, String actionName, int destState, double probability) {
		int actionIndex = getActionIndex(actionName);
		mTransProbs[srcState][actionIndex][destState] = probability;
	}

	/**
	 * Add a transition cost: C(s,a) = c.
	 * 
	 * @param srcState
	 * @param actionName
	 * @param cost
	 */
	public void addTransitionCost(int srcState, String actionName, double cost) {
		if (mCostType != CostType.TRANSITION_COST) {
			throw new UnsupportedOperationException();
		}
		int actionIndex = getActionIndex(actionName);
		mTransCosts[srcState][actionIndex] = cost;
	}

	/**
	 * Add a state cost: C(s) = c.
	 * 
	 * @param state
	 * @param cost
	 */
	public void addStateCost(int state, double cost) {
		if (mCostType != CostType.STATE_COST) {
			throw new UnsupportedOperationException();
		}
		mStateCosts[state] = cost;
	}

	public int getNumStates() {
		return mNumStates;
	}

	public int getNumActions() {
		return mNumActions;
	}

	public double getTransitionProbability(int srcState, String actionName, int destState) {
		int actionIndex = getActionIndex(actionName);
		return mTransProbs[srcState][actionIndex][destState];
	}

	public double getTransitionCost(int srcState, String actionName) {
		if (mCostType != CostType.TRANSITION_COST) {
			throw new UnsupportedOperationException();
		}
		int actionIndex = getActionIndex(actionName);
		return mTransCosts[srcState][actionIndex];
	}

	public double getStateCost(int state) {
		if (mCostType != CostType.STATE_COST) {
			throw new UnsupportedOperationException();
		}
		return mStateCosts[state];
	}

	private int getActionIndex(String actionName) {
		return mIndexedActions.indexOf(actionName);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof ExplicitMDP)) {
			return false;
		}
		ExplicitMDP mdp = (ExplicitMDP) obj;
		return mdp.mNumStates == mNumStates && mdp.mNumActions == mNumActions
				&& mdp.mIndexedActions.equals(mIndexedActions) && mdp.mCostType == mCostType
				&& Arrays.equals(mdp.mTransProbs, mTransProbs) && Arrays.equals(mdp.mTransCosts, mTransCosts)
				&& Arrays.equals(mdp.mStateCosts, mStateCosts);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mNumStates;
			result = 31 * result + mNumActions;
			result = 31 * result + mIndexedActions.hashCode();
			result = 31 * result + mCostType.hashCode();
			result = 31 * result + Arrays.hashCode(mTransProbs);
			result = 31 * result + (mCostType == CostType.TRANSITION_COST ? Arrays.hashCode(mTransCosts) : 0);
			result = 31 * result + (mCostType == CostType.STATE_COST ? Arrays.hashCode(mStateCosts) : 0);
			hashCode = result;
		}
		return hashCode;
	}
}