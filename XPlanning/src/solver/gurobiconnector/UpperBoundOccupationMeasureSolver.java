package solver.gurobiconnector;

import java.nio.channels.IllegalSelectorException;

import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;
import solver.common.ExplicitMDP;

public class UpperBoundOccupationMeasureSolver {

	private static final double DEFAULT_DISCOUNT_FACTOR = 0.99;

	private UpperBoundOccupationMeasureSolver() {
		throw new IllegalSelectorException();
	}

	/**
	 * Solve for X >= x_ia for all i, a, where x_ia is the occupation measure corresponding to a policy of a given MDP.
	 * 
	 * @param explicitMDP
	 *            : Explicit MDP
	 * @param feasibilityTol
	 *            : Feasibility tolerance
	 * @return Upper bound of occupation measure
	 * @throws GRBException
	 */
	public static double computeUpperBoundOccupationMeasure(ExplicitMDP explicitMDP, double feasibilityTol)
			throws GRBException {
		double[][] xResults = solveMaximumOccupationMeasure(explicitMDP, feasibilityTol);
		int n = explicitMDP.getNumStates();
		int m = explicitMDP.getNumActions();
		// From the constraint: x_ia >=0 for all i, a
		double upperBoundX = 0;
		for (int i = 0; i < n; i++) {
			for (int a = 0; a < m; a++) {
				// Exclude any x_ia value when action a is not applicable in state i
				if (explicitMDP.isActionApplicable(i, a)) {
					upperBoundX += xResults[i][a];
				}
			}
		}
		return upperBoundX;
	}

	/**
	 * Solve: maximize_x sum_i,a (x_ia) subject to:
	 * 
	 * (C1) x_ia >= 0, for all i, a
	 * 
	 * (C2) out(i) - gamma * in(i) = alpha_i, for all i in S
	 * 
	 * where:
	 * 
	 * in(i) = sum_j,a (x_ja * P(i|j,a)) and
	 * 
	 * out(i) = sum_a (x_ia).
	 * 
	 * @param explicitMDP
	 *            : Explicit MDP
	 * @param feasibilityTol
	 *            : Feasibility tolerance
	 * @return Occupation measure
	 * @throws GRBException
	 */
	public static double[][] solveMaximumOccupationMeasure(ExplicitMDP explicitMDP, double feasibilityTol)
			throws GRBException {
		GRBEnv env = new GRBEnv();
		GRBModel model = new GRBModel(env);

		GRBSolverUtils.configureToleranceParameters(model, GRBSolverUtils.DEFAULT_INT_FEAS_TOL, feasibilityTol);

		int n = explicitMDP.getNumStates();
		int m = explicitMDP.getNumActions();

		// Create variables: x_ia
		// Lower bound on variables: x_ia >= 0
		GRBVar[][] xVars = GRBSolverUtils.createOptimizationVars("x", GRB.CONTINUOUS, n, m, 0.0,
				Double.POSITIVE_INFINITY, model);

		// Set optimization objective
		setOptimizationObjective(xVars, model, explicitMDP);

		// Add constraints
		addDiscountedFlowConservationConstraints(explicitMDP, xVars, model);

		// Solve optimization problem for x_ia
		model.optimize();

		double[][] xResults = model.get(GRB.DoubleAttr.X, xVars);

		// Dispose of model and environment
		model.dispose();
		env.dispose();

		assert consistencyCheckDiscountedFlowConservationConstraints(xResults, explicitMDP, feasibilityTol);

		return xResults;
	}

	/**
	 * Objective: maximize_x sum_i,a(x_ia).
	 * 
	 * @param xVars
	 * @param model
	 * @param explicitMDP
	 * @throws GRBException
	 */
	private static void setOptimizationObjective(GRBVar[][] xVars, GRBModel model, ExplicitMDP explicitMDP)
			throws GRBException {
		int n = explicitMDP.getNumStates();
		int m = explicitMDP.getNumActions();

		// Objective: maximize sum_i,a(x_ia)
		GRBLinExpr objectiveLinExpr = new GRBLinExpr();
		for (int i = 0; i < n; i++) {
			for (int a = 0; a < m; a++) {
				// Exclude any x_ia term when action a is not applicable in state i
				if (explicitMDP.isActionApplicable(i, a)) {
					objectiveLinExpr.addTerm(1.0, xVars[i][a]);
				}
			}
		}

		// Set objective
		model.setObjective(objectiveLinExpr, GRB.MAXIMIZE);
	}

	/**
	 * Add the discounted flow-conservation constraints:
	 * 
	 * out(i) - gamma * in(i) = alpha_i, for all i in S, where alpha is the initial state distribution.
	 * 
	 * @param explicitMDP
	 *            : Explicit MDP
	 * @param xVars
	 *            : Occupation measure variables
	 * @param model
	 *            : GRB model to which to add the constraints
	 * @throws GRBException
	 */
	private static void addDiscountedFlowConservationConstraints(ExplicitMDP explicitMDP, GRBVar[][] xVars,
			GRBModel model) throws GRBException {
		int n = explicitMDP.getNumStates();

		// Initial state distribution
		double[] alpha = new double[n];
		int iniState = explicitMDP.getInitialState();
		alpha[iniState] = 1.0;

		double gamma = DEFAULT_DISCOUNT_FACTOR;

		// Constraints: sum_a (x_ia) - gamma * sum_j,a (x_ja * P(i|j,a)) = alpha_j, for all i in S
		for (int i = 0; i < n; i++) {
			String constraintName = "constraint_" + i;
			GRBLinExpr constraintLinExpr = new GRBLinExpr();

			GRBSolverUtils.addOutTerm(i, 1, explicitMDP, xVars, constraintLinExpr);
			GRBSolverUtils.addInTerm(i, -1 * gamma, explicitMDP, xVars, constraintLinExpr);

			// Add constraint
			model.addConstr(constraintLinExpr, GRB.EQUAL, alpha[i], constraintName);
		}
	}

	private static boolean consistencyCheckDiscountedFlowConservationConstraints(double[][] xResults,
			ExplicitMDP explicitMDP, double feasibilityTol) {
		int n = explicitMDP.getNumStates();

		// Initial state distribution
		double[] alpha = new double[n];
		int iniState = explicitMDP.getInitialState();
		alpha[iniState] = 1.0;

		double gamma = DEFAULT_DISCOUNT_FACTOR;

		// Constraints: sum_a (x_ia) - gamma * sum_j,a (x_ja * P(i|j,a)) = alpha_j, for all i in S
		for (int i = 0; i < n; i++) {
			double outValue = GRBSolverUtils.getOutValue(i, xResults, explicitMDP);
			double inValue = GRBSolverUtils.getInValue(i, xResults, explicitMDP);
			double diff = outValue - gamma * inValue;
			boolean satisfied = GRBSolverUtils.approximatelyEqual(diff, alpha[i], feasibilityTol);

			if (!satisfied) {
				return false;
			}
		}
		return true;
	}
}
