package solver.gurobiconnector;

import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;
import solver.common.CostType;
import solver.common.ExplicitMDP;

public class CostConstraintUtils {

	private CostConstraintUtils() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Add the cost-k hard constraint: sum_i,a (x_ia * C_k(i,a)) <= upper bound on c_k.
	 * 
	 * @param costFuncIndex
	 * @param upperBound
	 * @param explicitMDP
	 * @param xVars
	 * @param model
	 * @throws GRBException
	 */
	public static void addHardCostConstraint(int costFuncIndex, double upperBound, ExplicitMDP explicitMDP,
			GRBVar[][] xVars, GRBModel model) throws GRBException {
		addCostConstraint(costFuncIndex, upperBound, explicitMDP, xVars, null, model);
	}

	/**
	 * Add the cost-k soft constraint: sum_i,a (x_ia * C_k(i,a)) - v <= upper bound on c_k.
	 * 
	 * @param costFuncIndex
	 * @param upperBound
	 * @param explicitMDP
	 * @param xVars
	 * @param vVar
	 * @param model
	 * @throws GRBException
	 */
	public static void addSoftCostConstraint(int costFuncIndex, double upperBound, ExplicitMDP explicitMDP,
			GRBVar[][] xVars, GRBVar vVar, GRBModel model) throws GRBException {
		addCostConstraint(costFuncIndex, upperBound, explicitMDP, xVars, vVar, model);
	}

	/**
	 * Add the cost-k constraint:
	 * 
	 * For hard constraint: sum_i,a (x_ia * C_k(i,a)) <= hard upper bound on c_k.
	 * 
	 * For soft constraint: sum_i,a (x_ia * C_k(i,a)) - v <= soft upper bound on c_k.
	 * 
	 * @param costFuncIndex
	 *            : Index of constrained cost function
	 * @param upperBound
	 *            : Upper bound on the constrained cost
	 * @param explicitMDP
	 *            : Explicit MDP
	 * @param xVars
	 *            : Optimization x variables
	 * @param vVar
	 *            : Optimization v variable (amount of violation of soft constraint); null for hard constraint
	 * @param model
	 *            : GRB model to which to add the cost-k constraint
	 * @throws GRBException
	 */
	private static void addCostConstraint(int costFuncIndex, double upperBound, ExplicitMDP explicitMDP,
			GRBVar[][] xVars, GRBVar vVar, GRBModel model) throws GRBException {
		int n = explicitMDP.getNumStates();
		int m = explicitMDP.getNumActions();

		String constraintName = "constraintC_" + costFuncIndex + (vVar == null ? "_hard" : "_soft");
		GRBLinExpr constraintLinExpr = new GRBLinExpr();

		// sum_i,a (x_ia * C_k(i,a))
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
					constraintLinExpr.addTerm(stepCost, xVars[i][a]);
				}
			}
		}

		if (vVar != null) {
			// For soft constraint:
			// - v
			constraintLinExpr.addTerm(-1, vVar);
		}

		// Add constraint: [...] <= upper bound on c_k
		model.addConstr(constraintLinExpr, GRB.LESS_EQUAL, upperBound, constraintName);
	}

	/**
	 * Add non-linear penalty term: sum_{i=1 to m} alpha_i * penalty_i, to the objective function.
	 * 
	 * @param m
	 *            : Number of sampled (v, penalty) points
	 * @param penaltySamples
	 *            : [ penalty_0 (not used), penalty_1, ..., penalty_m ]
	 * @param scalingConst
	 *            : Scaling constant of the penalty term
	 * @param alphaVars
	 *            : [ alpha_0 (not used), alpha_1, ..., alpha_m ]
	 * @param objectiveLinExpr
	 *            : Objective function to which to add the penalty term
	 */
	public static void addNonlinearPenaltyTerm(int m, Double[] penaltySamples, double scalingConst, GRBVar[] alphaVars,
			GRBLinExpr objectiveLinExpr) {
		for (int i = 1; i <= m; i++) {
			objectiveLinExpr.addTerm(penaltySamples[i] * scalingConst, alphaVars[i]);
		}
	}

	/**
	 * Add the following constraints for piecewise linear approximation of non-linear penalty function:
	 * 
	 * (1) sum_{i=1 to m-1} h_i = 1
	 * 
	 * (2) alpha_i <= h_{i-1} + h_i, for i = 1,...,m
	 * 
	 * (3) sum_{i=1 to m} alpha_i = 1
	 * 
	 * (4) v = sum_{i=1 to m} alpha_i * v_i
	 * 
	 * @param m
	 *            : Number of sampled (v, penalty) points
	 * @param hVars
	 *            : [h_0 = 0, h_1, ..., h_{m-1}, h_m = 0]
	 * @param alphaVars
	 *            : [ alpha_0 (not used), alpha_1, ..., alpha_m ]
	 * @param vVar
	 *            : Violation variable
	 * @param model
	 *            : GRB model to which to add the constraints
	 * @throws GRBException
	 */
	public static void addPenaltyConstraints(int m, Double[] vSamples, GRBVar[] hVars, GRBVar[] alphaVars, GRBVar vVar,
			GRBModel model) throws GRBException {
		addHConstraint(m, hVars, model);
		addAlphaHConstraints(m, hVars, alphaVars, model);
		addAlphaConstraint(m, alphaVars, model);
		addAlphaVConstraint(m, vSamples, alphaVars, vVar, model);
	}

	/**
	 * Add constraint: sum_{i=1 to m-1} h_i = 1.
	 * 
	 * @param m
	 * @param hVars
	 * @param model
	 * @throws GRBException
	 */
	private static void addHConstraint(int m, GRBVar[] hVars, GRBModel model) throws GRBException {
		// Constraint: sum_{i=1 to m-1} h_i = 1

		// sum_{i=1 to m-1} h_i
		GRBLinExpr hConstraintLinExpr = new GRBLinExpr();
		for (int i = 1; i <= m - 1; i++) {
			hConstraintLinExpr.addTerm(1, hVars[i]);
		}

		// Add constraint: sum_{i=1 to m-1} h_i = 1
		model.addConstr(hConstraintLinExpr, GRB.EQUAL, 1, "constraint_h");
	}

	/**
	 * Add constraint: alpha_i <= h_{i-1} + h_i, for i = 1,...,m.
	 * 
	 * @param m
	 * @param hVars
	 * @param alphaVars
	 * @param model
	 * @throws GRBException
	 */
	private static void addAlphaHConstraints(int m, GRBVar[] hVars, GRBVar[] alphaVars, GRBModel model)
			throws GRBException {
		// Constraints: alpha_i <= h_{i-1} + h_i, for i = 1,...,m

		for (int i = 1; i <= m; i++) {
			String constraintName = "constraint_alphah_" + i;

			// alpha_i
			GRBLinExpr alphaLinExpr = new GRBLinExpr();
			alphaLinExpr.addTerm(1, alphaVars[i]);

			// h_{i-1} + h_i
			GRBLinExpr hLinExpr = new GRBLinExpr();

			if (i > 1) {
				hLinExpr.addTerm(1, hVars[i - 1]);
			} // h_0 = 0

			if (i < m) {
				hLinExpr.addTerm(1, hVars[i]);
			} // h_m = 0

			// Add constraint: alpha_i <= h_{i-1} + h_i
			model.addConstr(alphaLinExpr, GRB.LESS_EQUAL, hLinExpr, constraintName);
		}
	}

	/**
	 * // Add constraint: sum_{i=1 to m} alpha_i = 1.
	 * 
	 * @param m
	 * @param alphaVars
	 * @param model
	 * @throws GRBException
	 */
	private static void addAlphaConstraint(int m, GRBVar[] alphaVars, GRBModel model) throws GRBException {
		// Constraint: sum_{i=1 to m} alpha_i = 1

		// sum_{i=1 to m} alpha_i
		GRBLinExpr alphaConstraintLinExpr = new GRBLinExpr();
		for (int i = 1; i <= m; i++) {
			alphaConstraintLinExpr.addTerm(1, alphaVars[i]);
		}

		// Add constraint: sum_{i=1 to m} alpha_i = 1
		model.addConstr(alphaConstraintLinExpr, GRB.EQUAL, 1, "constraint_alpha");
	}

	/**
	 * Add constraint: v = sum_{i=1 to m} alpha_i * v_i.
	 * 
	 * @param m
	 * @param vSamples
	 * @param alphaVars
	 * @param vVar
	 * @param model
	 * @throws GRBException
	 */
	private static void addAlphaVConstraint(int m, Double[] vSamples, GRBVar[] alphaVars, GRBVar vVar, GRBModel model)
			throws GRBException {
		// Constraint: v = sum_{i=1 to m} alpha_i * v_i

		// v
		GRBLinExpr vLinExpr = new GRBLinExpr();
		vLinExpr.addTerm(1, vVar);

		// sum_{i=1 to m} alpha_i * v_i
		GRBLinExpr alphavLinExpr = new GRBLinExpr();
		for (int i = 1; i <= m; i++) {
			alphavLinExpr.addTerm(vSamples[i], alphaVars[i]);
		}

		// Add constraint: v = sum_{i=1 to m} alpha_i * v_i
		model.addConstr(vLinExpr, GRB.EQUAL, alphavLinExpr, "constraint_alphav");
	}
}
