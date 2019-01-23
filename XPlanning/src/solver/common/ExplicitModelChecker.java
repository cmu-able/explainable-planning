package solver.common;

public class ExplicitModelChecker {

	private ExplicitModelChecker() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Compute occupancy cost: sum_i,a (x(i,a) * c_k(i,a)).
	 * 
	 * @param explicitMDP
	 *            : Explicit MDP
	 * @param costFuncIndex
	 *            : Index of the cost function to compute occupancy cost
	 * @param xResults
	 *            : Occupation measure
	 * @return sum_i,a (x(i,a) * c_k(i,a))
	 */
	public static double computeOccupancyCost(ExplicitMDP explicitMDP, int costFuncIndex, double[][] xResults) {
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

					// c^k_ia * x_ia
					// OR
					// c^k_i * x_ia
					sum += stepCost * xResults[i][a];
				}
			}
		}
		return sum;
	}
}
