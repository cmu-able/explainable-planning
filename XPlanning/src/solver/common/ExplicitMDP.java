package solver.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class ExplicitMDP {

	/**
	 * Index of the optimization objective function of this MDP.
	 */
	private static final int OBJECTIVE_FUNCTION_INDEX = 0;

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private int mNumStates;
	private List<String> mIndexedActions;
	private CostType mCostType;
	private int mIniState;
	private Set<Integer> mGoalStates;
	private double[][][] mTransProbs;
	private double[][][] mTransCosts;
	private double[][] mStateCosts;

	public ExplicitMDP(int numStates, Set<String> actionNames, CostType costType, int numCostFunctions, int iniState,
			Set<Integer> goalStates) {
		int numActions = actionNames.size();
		mNumStates = numStates;
		mIndexedActions = sortActions(actionNames);
		mCostType = costType;
		mTransProbs = new double[numStates][numActions][numStates];
		if (costType == CostType.TRANSITION_COST) {
			mTransCosts = new double[numCostFunctions][numStates][numActions];
		} else if (costType == CostType.STATE_COST) {
			mStateCosts = new double[numCostFunctions][numStates];
		}
		mIniState = iniState;
		mGoalStates = goalStates;
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
	 * Add a transition cost of the cost function k: C_k(s,a) = c.
	 * 
	 * @param costFuncIndex
	 * @param srcState
	 * @param actionName
	 * @param cost
	 */
	public void addTransitionCost(int costFuncIndex, int srcState, String actionName, double cost) {
		int actionIndex = getActionIndex(actionName);
		addTransitionCost(costFuncIndex, srcState, actionIndex, cost);
	}

	public void addTransitionCost(int costFuncIndex, int srcState, int actionIndex, double cost) {
		checkTransitionCost();
		mTransCosts[costFuncIndex][srcState][actionIndex] = cost;
	}

	/**
	 * Add a state cost of the cost function k: C_k(s) = c.
	 * 
	 * @param costFuncIndex
	 * @param state
	 * @param cost
	 */
	public void addStateCost(int costFuncIndex, int state, double cost) {
		checkStateCost();
		mStateCosts[costFuncIndex][state] = cost;
	}

	/**
	 * Add an objective transition cost: C(s,a) = c.
	 * 
	 * @param srcState
	 * @param actionIndex
	 * @param objectiveCost
	 */
	public void addObjectiveTransitionCost(int srcState, int actionIndex, double objectiveCost) {
		checkTransitionCost();
		mTransCosts[OBJECTIVE_FUNCTION_INDEX][srcState][actionIndex] = objectiveCost;
	}

	/**
	 * Add an objective state cost: C(s) = c.
	 * 
	 * @param state
	 * @param objectiveCost
	 */
	public void addObjectiveStateCost(int state, double objectiveCost) {
		checkStateCost();
		mStateCosts[OBJECTIVE_FUNCTION_INDEX][state] = objectiveCost;
	}

	/**
	 * Set the objective cost function of this MDP to be the given cost function.
	 * 
	 * @param costFuncIndex
	 *            : Index of the cost function to be used as the objective cost function
	 */
	public void setObjectiveCosts(int costFuncIndex) {
		if (mCostType == CostType.TRANSITION_COST) {
			setObjectiveTransitionCosts(costFuncIndex);
		} else {
			setObjectiveStateCosts(costFuncIndex);
		}
	}

	private void setObjectiveTransitionCosts(int costFuncIndex) {
		for (int i = 0; i < mNumStates; i++) {
			for (int a = 0; a < mIndexedActions.size(); a++) {
				mTransCosts[OBJECTIVE_FUNCTION_INDEX][i][a] = mTransCosts[costFuncIndex][i][a];
			}
		}
	}

	private void setObjectiveStateCosts(int costFuncIndex) {
		for (int i = 0; i < mNumStates; i++) {
			mStateCosts[OBJECTIVE_FUNCTION_INDEX][i] = mStateCosts[costFuncIndex][i];
		}
	}

	public int getNumStates() {
		return mNumStates;
	}

	public int getNumActions() {
		return mIndexedActions.size();
	}

	public String getActionNameAtIndex(int actionIndex) {
		return mIndexedActions.get(actionIndex);
	}

	public CostType getCostType() {
		return mCostType;
	}

	public int getNumCostFunctions() {
		return mCostType == CostType.TRANSITION_COST ? mTransCosts.length : mStateCosts.length;
	}

	public int getInitialState() {
		return mIniState;
	}

	/**
	 * For SSP, there must be at least one goal state.
	 * 
	 * For average-cost MDP, there is no goal state (this method returns empty set).
	 * 
	 * @return A set of goal states
	 */
	public Set<Integer> getGoalStates() {
		return mGoalStates;
	}

	/**
	 * Action a is NOT applicable in state i iff p[i][a][j] = 0 for all j.
	 * 
	 * @param srcState
	 * @param actionIndex
	 * @return Whether the action at a given index is applicable in a given state.
	 */
	public boolean isActionApplicable(int srcState, int actionIndex) {
		double[] transProbs = mTransProbs[srcState][actionIndex];
		for (int j = 0; j < transProbs.length; j++) {
			if (transProbs[j] > 0) {
				return true;
			}
		}
		return false;
	}

	public double getTransitionProbability(int srcState, int actionIndex, int destState) {
		return mTransProbs[srcState][actionIndex][destState];
	}

	public double getTransitionCost(int costFuncIndex, int srcState, int actionIndex) {
		checkTransitionCost();
		return mTransCosts[costFuncIndex][srcState][actionIndex];
	}

	public double getStateCost(int costFuncIndex, int state) {
		checkStateCost();
		return mStateCosts[costFuncIndex][state];
	}

	public double getObjectiveTransitionCost(int srcState, int actionIndex) {
		checkTransitionCost();
		return mTransCosts[OBJECTIVE_FUNCTION_INDEX][srcState][actionIndex];
	}

	public double getObjectiveStateCost(int state) {
		checkStateCost();
		return mStateCosts[OBJECTIVE_FUNCTION_INDEX][state];
	}

	private int getActionIndex(String actionName) {
		return mIndexedActions.indexOf(actionName);
	}

	private void checkTransitionCost() {
		if (mCostType != CostType.TRANSITION_COST) {
			throw new UnsupportedOperationException();
		}
	}

	private void checkStateCost() {
		if (mCostType != CostType.STATE_COST) {
			throw new UnsupportedOperationException();
		}
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
		return mdp.mNumStates == mNumStates && mdp.mIndexedActions.equals(mIndexedActions) && mdp.mCostType == mCostType
				&& mdp.mIniState == mIniState && mdp.mGoalStates.equals(mGoalStates)
				&& Arrays.equals(mdp.mTransProbs, mTransProbs) && Arrays.equals(mdp.mTransCosts, mTransCosts)
				&& Arrays.equals(mdp.mStateCosts, mStateCosts);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mNumStates;
			result = 31 * result + mIndexedActions.hashCode();
			result = 31 * result + mCostType.hashCode();
			result = 31 * result + mIniState;
			result = 31 * result + mGoalStates.hashCode();
			result = 31 * result + Arrays.hashCode(mTransProbs);
			result = 31 * result + (mCostType == CostType.TRANSITION_COST ? Arrays.hashCode(mTransCosts) : 0);
			result = 31 * result + (mCostType == CostType.STATE_COST ? Arrays.hashCode(mStateCosts) : 0);
			hashCode = result;
		}
		return hashCode;
	}
}
