package solver.gurobiconnector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class ExplicitMDP {

	public static final int OBJECTIVE_FUNCTION_INDEX = 0;
	static final double DEFAULT_DISCOUNT_FACTOR = 1.0;

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private int mNumStates;
	private List<String> mIndexedActions;
	private CostType mCostType;
	private int mIniState;
	private double[][][] mTransProbs;
	private double[][][] mTransCosts;
	private double[][] mStateCosts;

	public ExplicitMDP(int numStates, Set<String> actionNames, CostType costType, int numCostFunctions) {
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

	public void setInitialState(int iniState) {
		mIniState = iniState;
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

	public int getNumStates() {
		return mNumStates;
	}

	public int getNumActions() {
		return mIndexedActions.size();
	}

	public CostType getCostType() {
		return mCostType;
	}

	public int getInitialState() {
		return mIniState;
	}

	public double getTransitionProbability(int srcState, String actionName, int destState) {
		int actionIndex = getActionIndex(actionName);
		return getTransitionProbability(srcState, actionIndex, destState);
	}

	public double getTransitionProbability(int srcState, int actionIndex, int destState) {
		return mTransProbs[srcState][actionIndex][destState];
	}

	public double getTransitionCost(int costFuncIndex, int srcState, String actionName) {
		int actionIndex = getActionIndex(actionName);
		return getTransitionCost(costFuncIndex, srcState, actionIndex);
	}

	public double getTransitionCost(int costFuncIndex, int srcState, int actionIndex) {
		checkTransitionCost();
		return mTransCosts[costFuncIndex][srcState][actionIndex];
	}

	public double getStateCost(int costFuncIndex, int state) {
		checkStateCost();
		return mStateCosts[costFuncIndex][state];
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
				&& mdp.mIniState == mIniState && Arrays.equals(mdp.mTransProbs, mTransProbs)
				&& Arrays.equals(mdp.mTransCosts, mTransCosts) && Arrays.equals(mdp.mStateCosts, mStateCosts);
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
			result = 31 * result + Arrays.hashCode(mTransProbs);
			result = 31 * result + (mCostType == CostType.TRANSITION_COST ? Arrays.hashCode(mTransCosts) : 0);
			result = 31 * result + (mCostType == CostType.STATE_COST ? Arrays.hashCode(mStateCosts) : 0);
			hashCode = result;
		}
		return hashCode;
	}
}
