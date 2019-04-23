package solver.gurobiconnector;

import java.util.Arrays;

import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;
import solver.common.ExplicitMDP;
import solver.common.LPSolution;
import solver.common.NonStrictConstraint;

public class AverageCostMDPSolver {

	private ExplicitMDP mExplicitMDP;
	private NonStrictConstraint[] mSoftConstraints;
	private NonStrictConstraint[] mHardConstraints;
	private GRBConnectorSettings mSettings;

	/**
	 * Constructor for unconstrained average-cost MDP.
	 * 
	 * @param explicitMDP
	 *            : Explicit MDP
	 * @param settings
	 *            : GRBConnector settings, containing tolerance parameters
	 */
	public AverageCostMDPSolver(ExplicitMDP explicitMDP, GRBConnectorSettings settings) {
		mExplicitMDP = explicitMDP;
		mSoftConstraints = null;
		mHardConstraints = null;
		mSettings = settings;
	}

	/**
	 * Average-cost MDP constructor.
	 * 
	 * @param explicitMDP
	 *            : Explicit MDP
	 * @param softConstraints
	 *            : Null iff unconstrained
	 * @param hardConstraints
	 *            : Null iff unconstrained
	 * @param settings
	 *            : GRBConnector settings, containing tolerance parameters
	 */
	public AverageCostMDPSolver(ExplicitMDP explicitMDP, NonStrictConstraint[] softConstraints,
			NonStrictConstraint[] hardConstraints, GRBConnectorSettings settings) {
		mExplicitMDP = explicitMDP;
		mSoftConstraints = softConstraints;
		mHardConstraints = hardConstraints;
		mSettings = settings;
	}

	/**
	 * Solve for an optimal policy for the average-cost MDP.
	 * 
	 * @param outputPolicy
	 *            : Return parameter of optimal policy
	 * @return Whether a solution policy exists, its objective value, and the solution
	 * @throws GRBException
	 */
	public LPSolution solveOptimalPolicy(double[][] outputPolicy) throws GRBException {
		double feasTol = mSettings.getFeasibilityTolerance();
		double roundOff = mSettings.getRoundOff();

		int n = mExplicitMDP.getNumStates();
		int m = mExplicitMDP.getNumActions();

		double[][] xResults = new double[n][m];
		double[][] yResults = new double[n][m];
		LPSolution solution = solve(xResults, yResults);

		if (solution.exists()) {
			for (int i = 0; i < n; i++) {
				// out_x(i) = sum_a (x_ia)
				double xDenom = GRBSolverUtils.getOutValue(i, xResults, mExplicitMDP);
				// out_y(i) = sum_a (y_ia)
				double yDenom = GRBSolverUtils.getOutValue(i, yResults, mExplicitMDP);

				if (xDenom > roundOff) {
					// Recurrent states: S_x = states i such that sum_a (x_ia) > 0
					fillPolicyMatrix(outputPolicy, i, xResults, xDenom);
				} else {
					// Transient states: S/S_x = states i such that sum_a (x_ia) = 0
					fillPolicyMatrix(outputPolicy, i, yResults, yDenom);
				}
			}

			assert GRBSolverUtils.consistencyCheckDeterministicPolicy(outputPolicy, mExplicitMDP, feasTol);
		}

		return solution;
	}

	/**
	 * Fill in an action for a given state in the policy.
	 * 
	 * It is guaranteed that, for any state i, v*_ia > 0 for only one a in A_i. Therefore, the policy is deterministic.
	 * 
	 * @param policyMatrix
	 *            : Policy matrix (return parameter)
	 * @param i
	 *            : State
	 * @param vResults
	 *            : Optimal values of v_ia
	 * @param vDenom
	 *            : sum_a (v_ia)
	 */
	private void fillPolicyMatrix(double[][] policyMatrix, int i, double[][] vResults, double vDenom) {
		int m = mExplicitMDP.getNumActions();

		// Interpret x_ia as the limiting probability under a stationary (deterministic) policy that the
		// system occupies state i and chooses action a when the initial state distribution is alpha.

		for (int a = 0; a < m; a++) {
			// Exclude any v_ia value when action a is not applicable in state i
			if (mExplicitMDP.isActionApplicable(i, a)) {
				// pi_ia = x_ia / sum_a (x_ia) for recurrent states
				// OR
				// pi_ia = y_ia / sum_a (y_ia) for transient states
				policyMatrix[i][a] = vResults[i][a] / vDenom;
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
	 * (C5) sum_a (Deltax_ia) <= 1, for all i in S
	 * 
	 * (C6) x_ia / X <= Deltax_ia, for all i, a, where X >= x_ia
	 * 
	 * (C7) sum_a (Deltay_ia) <= 1, for all i in S
	 * 
	 * (C8) y_ia / Y <= Deltay_ia, for all i, a, where Y >= y_ia
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
	 * @return Whether a feasible solution exists, its objective value, and the solution
	 * @throws GRBException
	 */
	public LPSolution solve(double[][] xResults, double[][] yResults) throws GRBException {
		double intFeasTol = mSettings.getIntegralityTolerance();
		double feasTol = mSettings.getFeasibilityTolerance();

		GRBEnv env = new GRBEnv();
		GRBModel model = new GRBModel(env);

		GRBSolverUtils.configureToleranceParameters(model, intFeasTol, feasTol);

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

		// Create variables: Deltax_ia (binary)
		String deltaxVarName = "Deltax";
		GRBVar[][] deltaxVars = GRBSolverUtils.createOptimizationVars(deltaxVarName, GRB.BINARY, n, m, 0.0, 1.0, model);

		// Create variables: Deltay_ia (binary)
		String deltayVarName = "Deltay";
		GRBVar[][] deltayVars = GRBSolverUtils.createOptimizationVars(deltayVarName, GRB.BINARY, n, m, 0.0, 1.0, model);

		// Set optimization objective
		GRBSolverUtils.setOptimizationObjective(mExplicitMDP, xVars, model);

		// Add constraints
		addC1Constraints(xVars, model);
		addC2Constraints(alpha, xVars, yVars, model);

		// Add constraints to ensure deterministic solution policy
		GRBSolverUtils.addDeltaConstraints(mExplicitMDP, deltaxVarName, deltaxVars, model);
		GRBSolverUtils.addDeltaConstraints(mExplicitMDP, deltayVarName, deltayVars, model);

		// For average-cost MDP, sum_i,a (x_ia) = 1; therefore, we can use X = 1
		GRBSolverUtils.addVarDeltaConstraints(1.0, mExplicitMDP, "x", xVars, deltaxVarName, deltaxVars, model);

		// Similarly, we use Y = 1
		GRBSolverUtils.addVarDeltaConstraints(1.0, mExplicitMDP, "y", yVars, deltayVarName, deltayVars, model);

		// Add (upper/lower bound) cost constraints, if any
		if (mSoftConstraints != null) {
			// Soft constraints
			CostConstraintUtils.addSoftCostConstraints(mSoftConstraints, mHardConstraints, mExplicitMDP, xVars, model);
		} else if (mHardConstraints != null) {
			// Hard constraints
			CostConstraintUtils.addHardCostConstraints(mHardConstraints, mExplicitMDP, xVars, model);
		}

		// Solve optimization problem for x_ia, y_ia, and Delta_ia
		model.optimize();

		int numSolutions = model.get(GRB.IntAttr.SolCount);
		double objectiveValue = -1;

		if (numSolutions > 0) {
			// Solution found

			// Objective value: sum_i,a (c_ia * x_ia)
			objectiveValue = model.get(GRB.DoubleAttr.ObjVal);

			// Query results: optimal values of x_ia, y_ia, and Delta_ia
			double[][] grbXResults = model.get(GRB.DoubleAttr.X, xVars);
			double[][] grbYResults = model.get(GRB.DoubleAttr.X, yVars);
			double[][] grbDeltaxResults = model.get(GRB.DoubleAttr.X, deltaxVars);
			double[][] grbDeltayResults = model.get(GRB.DoubleAttr.X, deltayVars);

			// Copy x_ia and y_ia results to the return parameters
			System.arraycopy(grbXResults, 0, xResults, 0, grbXResults.length);
			System.arraycopy(grbYResults, 0, yResults, 0, grbYResults.length);

			// Consistency checks
			verifyAllConstraints(grbXResults, grbYResults, grbDeltaxResults, grbDeltayResults, alpha);
			assert GRBSolverUtils.consistencyCheckResults(grbXResults, grbDeltaxResults, mExplicitMDP, feasTol);
			assert GRBSolverUtils.consistencyCheckResults(grbYResults, grbDeltayResults, mExplicitMDP, feasTol);
		}

		// Dispose of model and environment
		model.dispose();
		env.dispose();

		// LP solution
		LPSolution solution = new LPSolution(numSolutions > 0, objectiveValue);
		solution.addSolution("x", xResults);
		solution.addSolution("y", yResults);
		return solution;
	}

	/**
	 * Add the constraints (C1): out_x(i) - in_x(i) = 0, for all i in S.
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
	 * Add the constraints (C2): out_x(i) + out_y(i) - in_y(i) = alpha_i, for all i in S.
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

	private void verifyAllConstraints(double[][] xResults, double[][] yResults, double[][] deltaxResults,
			double[][] deltayResults, double[] alpha) {
		double feasTol = mSettings.getFeasibilityTolerance();

		assert consistencyCheckC1Constraints(xResults);
		assert consistencyCheckC2Constraints(xResults, yResults, alpha);
		assert GRBSolverUtils.consistencyCheckDeltaConstraints(deltaxResults, mExplicitMDP);
		assert GRBSolverUtils.consistencyCheckVarDeltaConstraints(xResults, deltaxResults, 1.0, mExplicitMDP, feasTol);
		assert GRBSolverUtils.consistencyCheckDeltaConstraints(deltayResults, mExplicitMDP);
		assert GRBSolverUtils.consistencyCheckVarDeltaConstraints(yResults, deltayResults, 1.0, mExplicitMDP, feasTol);
		if (mHardConstraints != null) {
			assert GRBSolverUtils.consistencyCheckCostConstraints(xResults, mHardConstraints, mExplicitMDP, feasTol);
		}
	}

	/**
	 * Check constraints (C1): out_x(i) - in_x(i) = 0, for all i in S.
	 * 
	 * @param xResults
	 * @return Whether (C1) is satisfied
	 */
	private boolean consistencyCheckC1Constraints(double[][] xResults) {
		int n = mExplicitMDP.getNumStates();

		for (int i = 0; i < n; i++) {
			double outxValue = GRBSolverUtils.getOutValue(i, xResults, mExplicitMDP);
			double inxValue = GRBSolverUtils.getInValue(i, xResults, mExplicitMDP);
			boolean satisfied = GRBSolverUtils.approximatelyEqual(outxValue, inxValue,
					mSettings.getFeasibilityTolerance());

			if (!satisfied) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Check constraints (C2): out_x(i) + out_y(i) - in_y(i) = alpha_i, for all i in S.
	 * 
	 * @param xResults
	 * @param yResults
	 * @param alpha
	 * @return Whether (C2) is satisfied
	 */
	private boolean consistencyCheckC2Constraints(double[][] xResults, double[][] yResults, double[] alpha) {
		int n = mExplicitMDP.getNumStates();

		for (int i = 0; i < n; i++) {
			double outxValue = GRBSolverUtils.getOutValue(i, xResults, mExplicitMDP);
			double outyValue = GRBSolverUtils.getOutValue(i, yResults, mExplicitMDP);
			double inyValue = GRBSolverUtils.getInValue(i, yResults, mExplicitMDP);
			boolean satisfied = GRBSolverUtils.approximatelyEqual(outxValue + outyValue - inyValue, alpha[i],
					mSettings.getFeasibilityTolerance());

			if (!satisfied) {
				return false;
			}
		}
		return true;
	}

}
