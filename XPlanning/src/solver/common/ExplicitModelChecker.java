package solver.common;

public class ExplicitModelChecker {

	private ExplicitModelChecker() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Compute occupancy cost: sum_i,a (x(i,a) * c_k(i,a)).
	 * 
	 * @param xResults
	 *            : Occupation measure
	 * @param costFuncIndex
	 *            : Index of the cost function to compute occupancy cost
	 * @param explicitMDP
	 *            : Explicit MDP
	 * @return sum_i,a (x(i,a) * c_k(i,a))
	 */
	public static double computeOccupancyCost(double[][] xResults, int costFuncIndex, ExplicitMDP explicitMDP) {
		return computeOccupancyCost(xResults, costFuncIndex, 0, 1, explicitMDP);
	}

	/**
	 * Compute transformed occupancy cost: sum_i,a (x(i,a) * (shift + multiplier * c_k(i,a))).
	 * 
	 * @param xResults
	 *            : Occupation measure
	 * @param costFuncIndex
	 *            : Index of the cost function to compute transformed occupancy cost
	 * @param costShift
	 *            : Cost shift
	 * @param costMultiplier
	 *            : Cost multiplier
	 * @param explicitMDP
	 *            : Explicit MDP
	 * @return sum_i,a (x(i,a) * (shift + multiplier * c_k(i,a)))
	 */
	public static double computeOccupancyCost(double[][] xResults, int costFuncIndex, double costShift,
			double costMultiplier, ExplicitMDP explicitMDP) {
		int n = explicitMDP.getNumStates();
		int m = explicitMDP.getNumActions();
		double sum = 0;

		for (int i = 0; i < n; i++) {
			for (int a = 0; a < m; a++) {
				// Exclude any x_ia term when action a is not applicable in state i
				if (explicitMDP.isActionApplicable(i, a)) {
					// Transition k-cost: c^k_ia
					// OR
					// State k-cost: c^k_i
					double stepCost = explicitMDP.getCostType() == CostType.TRANSITION_COST
							? explicitMDP.getTransitionCost(costFuncIndex, i, a)
							: explicitMDP.getStateCost(costFuncIndex, i);

					// shift + multiplier * c^k_ia
					// OR
					// shift + multiplier * c^k_i
					double transformedStepCost = costShift + costMultiplier * stepCost;

					// (transformed c^k_ia) * x_ia
					// OR
					// (transformed c^k_i) * x_ia
					sum += transformedStepCost * xResults[i][a];
				}
			}
		}
		return sum;
	}
}
