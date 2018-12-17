package solver.gurobiconnector;

import java.util.Arrays;

import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;
import solver.common.ExplicitMDP;

public class AverageCostMDPSolver {

	private ExplicitMDP mExplicitMDP;
	private Double[] mUpperBounds;

	public AverageCostMDPSolver(ExplicitMDP explicitMDP) {
		mExplicitMDP = explicitMDP;
	}

	public AverageCostMDPSolver(ExplicitMDP explicitMDP, Double[] upperBounds) {
		mExplicitMDP = explicitMDP;
		mUpperBounds = upperBounds;
	}

	/**
	 * Solve for an optimal policy for the average-cost MDP.
	 * 
	 * @param policy
	 *            : Return parameter of optimal policy
	 * @return Whether a solution policy exists
	 * @throws GRBException
	 */
	public boolean solveOptimalPolicy(double[][] policy) throws GRBException {
		int n = mExplicitMDP.getNumStates();
		int m = mExplicitMDP.getNumActions();

		double[][] xResults = new double[n][m];
		double[][] yResults = new double[n][m];
		boolean solutionFound = solve(xResults, yResults);

		if (solutionFound) {
			for (int i = 0; i < n; i++) {
				// out_x(i) = sum_a (x_ia)
				double denom = GRBSolverUtils.getOutValue(i, xResults, mExplicitMDP);

				if (denom > 0) {
					// Recurrent states: S_x = states i such that sum_a (x_ia) > 0
					fillPolicyForRecurrentState(policy, i, xResults, denom);
				} else {
					// Transient states: S/S_x = states i such that sum_a (x_ia) = 0
					fillPolicyForTransientState(policy, i, yResults);
				}
			}

			assert GRBSolverUtils.consistencyCheckDeterministicPolicy(policy, mExplicitMDP);
		}

		return solutionFound;
	}

	/**
	 * Fill in action for a given recurrent state in the policy.
	 * 
	 * It is guaranteed that, for any recurrent state i, x*_ia > 0 for only one a in A_i. Therefore, the policy is
	 * deterministic.
	 * 
	 * @param policy
	 *            : Policy (return parameter)
	 * @param i
	 *            : Recurrent state
	 * @param xResults
	 *            : Optimal values of x_ia
	 * @param denom
	 *            : sum_a (x_ia)
	 */
	private void fillPolicyForRecurrentState(double[][] policy, int i, double[][] xResults, double denom) {
		int m = mExplicitMDP.getNumActions();

		// Interpret x_ia as the limiting probability under a stationary (deterministic) policy that the
		// system occupies state i and chooses action a when the initial state distribution is alpha.

		for (int a = 0; a < m; a++) {
			// Exclude any x_ia value when action a is not applicable in state i
			if (mExplicitMDP.isActionApplicable(i, a)) {
				// pi_ia = x_ia / sum_a (x_ia)
				policy[i][a] = xResults[i][a] / denom;
			}
		}
	}

	/**
	 * Fill in action for a given transient state in the policy.
	 * 
	 * If, for any transient state i, there are more than one y*_ia > 0, this method chooses the action for i to be that
	 * of the first non-zero y*_ia. Therefore, the policy is deterministic.
	 * 
	 * @param policy
	 *            : Policy (return parameter)
	 * @param i
	 *            : Transient state
	 * @param yResults
	 *            : Optimal values of y_ia
	 */
	private void fillPolicyForTransientState(double[][] policy, int i, double[][] yResults) {
		int m = mExplicitMDP.getNumActions();
		for (int a = 0; a < m; a++) {
			if (mExplicitMDP.isActionApplicable(i, a) && yResults[i][a] > 0) {
				// First non-zero y*_ia
				policy[i][a] = 1;
				break;
			}
		}
	}

	/**
	 * Solve: minimize_x,y sum_i,a (c_ia * x_ia) subject to:
	 * 
	 * (C1) out_x(i) - in_x(i) = 0, for all i in S
	 * 
	 * (C2) out_x(i) + out_y(i) - in_y(i) = alpha_i, for all i in S
	 * 
	 * (C3) x_ia >= 0, for all i in S, a in A_i
	 * 
	 * (C4) y_ia >= 0, for all i in S, a in A_i
	 * 
	 * (C5) sum_a (Delta_ia) <= 1, for all i in S
	 * 
	 * (C6) x_ia / X <= Delta_ia, for all i, a, where X >= x_ia
	 * 
	 * and optionally,
	 * 
	 * (Ck) sum_i,a (c^k_ia * x_ia) <= beta_k, for all k
	 * 
	 * where:
	 * 
	 * in_v(i) = sum_j,a (v_ja * P(i|j,a)) and
	 * 
	 * out_v(i) = sum_a (v_ia).
	 * 
	 * @param xResults
	 *            : Return parameter of x*_ia results
	 * @param yResults
	 *            : Return parameter of y*_ia results
	 * @return Whether a feasible solution exists
	 * @throws GRBException
	 */
	public boolean solve(double[][] xResults, double[][] yResults) throws GRBException {
		GRBEnv env = new GRBEnv();
		GRBModel model = new GRBModel(env);

		int n = mExplicitMDP.getNumStates();
		int m = mExplicitMDP.getNumActions();

		// Initial state distribution
		double[] alpha = new double[n];
		Arrays.fill(alpha, 1.0 / n);

		// Create variables: x_ia
		// Lower bound on variables: x_ia >= 0
		GRBVar[][] xVars = GRBSolverUtils.createOptimizationVars("x", GRB.CONTINUOUS, n, m, 0.0,
				Double.POSITIVE_INFINITY, model);

		// Create variables: y_ia
		// Lower bound on variables: y_ia >= 0
		GRBVar[][] yVars = GRBSolverUtils.createOptimizationVars("y", GRB.CONTINUOUS, n, m, 0.0,
				Double.POSITIVE_INFINITY, model);

		// Create variables: Delta_ia (binary)
		GRBVar[][] deltaVars = GRBSolverUtils.createOptimizationVars("Delta", GRB.BINARY, n, m, 0.0, 1.0, model);

		// Set optimization objective
		GRBSolverUtils.setOptimizationObjective(mExplicitMDP, xVars, model);

		// Add constraints
		addC1Constraints(xVars, model);
		addC2Constraints(alpha, xVars, yVars, model);

		// Add constraints to ensure deterministic solution policy
		GRBSolverUtils.addDeltaConstraints(mExplicitMDP, deltaVars, model);

		// For average-cost MDP, sum_i,a (x_ia) = 1; therefore, we can use X = 1
		GRBSolverUtils.addxDeltaConstraints(1.0, mExplicitMDP, xVars, deltaVars, model);

		// Add upper-bound cost constraints, if any
		if (mUpperBounds != null) {
			GRBSolverUtils.addCostConstraints(mUpperBounds, mExplicitMDP, xVars, model);
		}

		// Solve optimization problem for x_ia, y_ia, and Delta_ia
		model.optimize();

		int numSolutions = model.get(GRB.IntAttr.SolCount);

		if (numSolutions > 0) {
			// Solution found
			// Query results: optimal values of x_ia, y_ia, and Delta_ia
			double[][] grbXResults = model.get(GRB.DoubleAttr.X, xVars);
			double[][] grbYResults = model.get(GRB.DoubleAttr.X, yVars);
			double[][] grbDeltaResults = model.get(GRB.DoubleAttr.X, deltaVars);

			// Copy x_ia and y_ia results to the return parameters
			System.arraycopy(grbXResults, 0, xResults, 0, grbXResults.length);
			System.arraycopy(grbYResults, 0, yResults, 0, grbYResults.length);

			// Consistency checks
			verifyAllConstraints(grbXResults, grbYResults, grbDeltaResults, alpha);
			assert GRBSolverUtils.consistencyCheckResults(grbXResults, grbDeltaResults, mExplicitMDP);
		}

		// Dispose of model and environment
		model.dispose();
		env.dispose();

		return numSolutions > 0;
	}

	/**
	 * Add the constraints: out_x(i) - in_x(i) = 0, for all i in S.
	 * 
	 * @param xVars
	 *            : Optimization x variables
	 * @param model
	 *            : GRB model to which to add the constraints
	 * @throws GRBException
	 */
	private void addC1Constraints(GRBVar[][] xVars, GRBModel model) throws GRBException {
		int n = mExplicitMDP.getNumStates();

		for (int i = 0; i < n; i++) {
			String constraintName = "constraintC1_" + i;
			// out_x(i) - in_x(i) = 0
			GRBLinExpr constraintLinExpr = new GRBLinExpr();

			// Expression += out_x(i)
			GRBSolverUtils.addOutTerm(i, 1, mExplicitMDP, xVars, constraintLinExpr);

			// Expression -= in_x(i)
			GRBSolverUtils.addInTerm(i, -1, mExplicitMDP, xVars, constraintLinExpr);

			// Add constraint
			model.addConstr(constraintLinExpr, GRB.EQUAL, 0, constraintName);
		}
	}

	/**
	 * Add the constraints: out_x(i) + out_y(i) - in_y(i) = alpha_i, for all i in S.
	 * 
	 * @param alpha
	 *            : Initial state distribution
	 * @param xVars
	 *            : Optimization x variables
	 * @param yVars
	 *            : Optimization y variables
	 * @param model
	 *            : GRB model to which to add the constraints
	 * @throws GRBException
	 */
	private void addC2Constraints(double[] alpha, GRBVar[][] xVars, GRBVar[][] yVars, GRBModel model)
			throws GRBException {
		int n = mExplicitMDP.getNumStates();

		for (int i = 0; i < n; i++) {
			String constraintName = "constraintC2_" + i;
			// out_x(i) + out_y(i) - in_y(i) = alpha_i
			GRBLinExpr constraintLinExpr = new GRBLinExpr();

			// Expression += out_x(i)
			GRBSolverUtils.addOutTerm(i, 1, mExplicitMDP, xVars, constraintLinExpr);

			// Expression += out_y(i)
			GRBSolverUtils.addOutTerm(i, 1, mExplicitMDP, yVars, constraintLinExpr);

			// Expression -= in_y(i)
			GRBSolverUtils.addInTerm(i, -1, mExplicitMDP, yVars, constraintLinExpr);

			// Add constraint
			model.addConstr(constraintLinExpr, GRB.EQUAL, alpha[i], constraintName);
		}
	}

	private void verifyAllConstraints(double[][] xResults, double[][] yResults, double[][] deltaResults,
			double[] alpha) {
		assert consistencyCheckC1Constraints(xResults);
		assert consistencyCheckC2Constraints(xResults, yResults, alpha);
		assert GRBSolverUtils.consistencyCheckDeltaConstraints(deltaResults, mExplicitMDP);
		assert GRBSolverUtils.consistencyCheckxDeltaConstraints(xResults, deltaResults, 1.0, mExplicitMDP);
		if (mUpperBounds != null) {
			assert GRBSolverUtils.consistencyCheckCostConstraints(xResults, mUpperBounds, mExplicitMDP);
		}
	}

	private boolean consistencyCheckC1Constraints(double[][] xResults) {
		int n = mExplicitMDP.getNumStates();

		for (int i = 0; i < n; i++) {
			double outxValue = GRBSolverUtils.getOutValue(i, xResults, mExplicitMDP);
			double inxValue = GRBSolverUtils.getInValue(i, xResults, mExplicitMDP);

			if (!GRBSolverUtils.approximatelyEquals(outxValue, inxValue)) {
				return false;
			}
		}
		return true;
	}

	private boolean consistencyCheckC2Constraints(double[][] xResults, double[][] yResults, double[] alpha) {
		int n = mExplicitMDP.getNumStates();

		for (int i = 0; i < n; i++) {
			double outxValue = GRBSolverUtils.getOutValue(i, xResults, mExplicitMDP);
			double outyValue = GRBSolverUtils.getInValue(i, yResults, mExplicitMDP);
			double inyValue = GRBSolverUtils.getInValue(i, yResults, mExplicitMDP);

			if (!GRBSolverUtils.approximatelyEquals(outxValue + outyValue - inyValue, alpha[i])) {
				return false;
			}
		}
		return true;
	}

}
