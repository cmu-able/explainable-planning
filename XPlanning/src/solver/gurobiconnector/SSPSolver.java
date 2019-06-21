package solver.gurobiconnector;

import java.util.Set;

import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;
import solver.common.ExplicitMDP;
import solver.common.LPSolution;
import solver.common.NonStrictConstraint;

public class SSPSolver {

	private ExplicitMDP mExplicitMDP;
	private NonStrictConstraint[] mSoftConstraints;
	private NonStrictConstraint[] mHardConstraints;
	private GRBConnectorSettings mSettings;

	/**
	 * Constructor for unconstrained SSP.
	 * 
	 * @param explicitMDP
	 *            : Explicit MDP
	 * @param settings
	 *            : GRBConnector settings, containing tolerance parameters
	 */
	public SSPSolver(ExplicitMDP explicitMDP, GRBConnectorSettings settings) {
		mExplicitMDP = explicitMDP;
		mSoftConstraints = null;
		mHardConstraints = null;
		mSettings = settings;
	}

	/**
	 * SSP constructor.
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
	public SSPSolver(ExplicitMDP explicitMDP, NonStrictConstraint[] softConstraints,
			NonStrictConstraint[] hardConstraints, GRBConnectorSettings settings) {
		mExplicitMDP = explicitMDP;
		mSoftConstraints = softConstraints;
		mHardConstraints = hardConstraints;
		mSettings = settings;
	}

	public LPSolution solveOptimalPolicy(double[][] outputPolicy) throws GRBException {
		double feasTol = mSettings.getFeasibilityTolerance();
		double roundOff = mSettings.getRoundOff();

		int n = mExplicitMDP.getNumStates();
		int m = mExplicitMDP.getNumActions();

		double[][] xResults = new double[n][m];
		LPSolution solution = solve(xResults);

		if (solution.exists()) {
			for (int i = 0; i < n; i++) {
				// out(i) = sum_a (x_ia)
				double denom = GRBSolverUtils.getOutValue(i, xResults, mExplicitMDP);

				if (denom > roundOff) {
					// Interpret occupation measure x_ia as the total expected discounted number of times action a is
					// executed in state i.
					// When sum_a (x_ia) > 0, it means state i is reachable.

					for (int a = 0; a < m; a++) {
						// Exclude any x_ia value when action a is not applicable in state i
						if (mExplicitMDP.isActionApplicable(i, a)) {
							// pi_ia = x_ia / sum_a (x_ia)
							fillStateActionProbability(outputPolicy, xResults, denom, i, a);
						}
					}
				}
			}

			assert GRBSolverUtils.consistencyCheckDeterministicPolicy(outputPolicy, mExplicitMDP, feasTol);
		}

		return solution;
	}

	private void fillStateActionProbability(double[][] outputPolicy, double[][] xResults, double denom, int i, int a) {
		// pi_ia = x_ia / sum_a (x_ia)
		double prob = xResults[i][a] / denom;

		// Some x_ia value may be very small due to floating-point arithmetic error
		// Round-off very small prob value to 0 -- to ensure that outputPolicy: pi_ia doesn't
		// contain the floating-point arithmetic error
		double roundOff = mSettings.getRoundOff();
		outputPolicy[i][a] = prob > roundOff ? prob : 0.0;
	}

	/**
	 * Solve: minimize_x sum_i,a (x_ia * c_ia) subject to:
	 * 
	 * (C1) out(i) - in(i) = 0, for all i in S \ (G and s0)
	 * 
	 * (C2) out(s0) - in(s0) = 1
	 * 
	 * (C3) sum_{sg in G} (in(sg)) = 1
	 * 
	 * (C4) x_ia >= 0, for all i in S, a in A_i
	 * 
	 * (C5) sum_a (Delta_ia) <= 1, for all i in S
	 * 
	 * (C6) x_ia / X <= Delta_ia, for all i, a, where X >= x_ia
	 * 
	 * and optionally,
	 * 
	 * for upper-bound constraints:
	 * 
	 * (Ck) sum_i,a (c^k_ia * x_ia) <= beta_k, for all k
	 * 
	 * for lower-bound constraints:
	 * 
	 * (Ck) sum_i,a (c^k_ia * x_ia) >= beta_k, for all k
	 * 
	 * where:
	 * 
	 * in(i) = sum_j,a (x_ja * P(i|j,a)) and
	 * 
	 * out(i) = sum_a (x_ia).
	 * 
	 * @param xResults
	 *            : Return parameter of x*_ia results
	 * @return Whether a feasible solution exists, its objective value, and the solution
	 * @throws GRBException
	 */
	public LPSolution solve(double[][] xResults) throws GRBException {
		double intFeasTol = mSettings.getIntegralityTolerance();
		double feasTol = mSettings.getFeasibilityTolerance();

		GRBEnv env = new GRBEnv();
		GRBModel model = new GRBModel(env);

		GRBSolverUtils.configureToleranceParameters(model, intFeasTol, feasTol);

		int n = mExplicitMDP.getNumStates();
		int m = mExplicitMDP.getNumActions();

		// Create variables: x_ia
		// Lower bound on variables: x_ia >= 0
		GRBVar[][] xVars = GRBSolverUtils.createOptimizationVars("x", GRB.CONTINUOUS, n, m, 0.0,
				Double.POSITIVE_INFINITY, model);

		// Create variables: Delta_ia (binary)
		String deltaxVarName = "Deltax";
		GRBVar[][] deltaVars = GRBSolverUtils.createOptimizationVars(deltaxVarName, GRB.BINARY, n, m, 0.0, 1.0, model);

		// Set optimization objective
		GRBSolverUtils.setOptimizationObjective(mExplicitMDP, xVars, model);

		// Add constraints
		addFlowConservationConstraints(xVars, model);
		addSourceFlowConstraint(xVars, model);
		addSinksFlowConstraint(xVars, model);

		// Add constraints to ensure deterministic solution policy
		GRBSolverUtils.addDeltaConstraints(mExplicitMDP, deltaxVarName, deltaVars, model);

		// For SSP, X is an upper-bound on occupation measure
		double upperBoundOM = UpperBoundOccupationMeasureSolver.computeUpperBoundOccupationMeasure(mExplicitMDP,
				feasTol);
		GRBSolverUtils.addVarDeltaConstraints(upperBoundOM, mExplicitMDP, "x", xVars, deltaxVarName, deltaVars, model);

		// Add (upper/lower bound) cost constraints, if any
		if (mSoftConstraints != null) {
			// Soft constraints
			CostConstraintUtils.addSoftCostConstraints(mSoftConstraints, mHardConstraints, mExplicitMDP, xVars, model);
		} else if (mHardConstraints != null) {
			// Hard constraints
			CostConstraintUtils.addHardCostConstraints(mHardConstraints, mExplicitMDP, xVars, model);
		}

		// Solve optimization problem for x_ia and Delta_ia
		model.optimize();

		int numSolutions = model.get(GRB.IntAttr.SolCount);
		double objectiveValue = -1;

		if (numSolutions > 0) {
			// Solution found

			// Objective value: sum_i,a (c_ia * x_ia)
			objectiveValue = model.get(GRB.DoubleAttr.ObjVal);

			// Query results: optimal values of x_ia and Delta_ia
			double[][] grbXResults = model.get(GRB.DoubleAttr.X, xVars);
			double[][] grbDeltaResults = model.get(GRB.DoubleAttr.X, deltaVars);

			// Copy x_ia results to the return parameters
			System.arraycopy(grbXResults, 0, xResults, 0, grbXResults.length);

			// Consistency checks
			verifyAllConstraints(grbXResults, grbDeltaResults, upperBoundOM);
			assert GRBSolverUtils.consistencyCheckResults(grbXResults, grbDeltaResults, mExplicitMDP, feasTol);
		}

		// Dispose of model and environment
		model.dispose();
		env.dispose();

		// LP solution
		LPSolution solution = new LPSolution(numSolutions > 0, objectiveValue);
		solution.addSolution("x", xResults);
		return solution;
	}

	/**
	 * Add the flow-conservation constraints C1: out(i) - in(i) = 0, for all i in S \ (G and s0).
	 * 
	 * @param xVars
	 *            : Occupation measure variables
	 * @param model
	 *            : GRB model to which to add the flow-conservation constraints
	 * @throws GRBException
	 */
	private void addFlowConservationConstraints(GRBVar[][] xVars, GRBModel model) throws GRBException {
		int n = mExplicitMDP.getNumStates();
		Set<Integer> goals = mExplicitMDP.getGoalStates();
		int iniState = mExplicitMDP.getInitialState();

		for (int i = 0; i < n; i++) {
			if (goals.contains(Integer.valueOf(i)) || iniState == i) {
				// Exclude goal states G and initial state s0
				continue;
			}

			String constraintName = "constraintC1_" + i;
			// out(i) - in(i) = 0
			GRBLinExpr constraintLinExpr = new GRBLinExpr();

			// Expression += out(i)
			GRBSolverUtils.addOutTerm(i, 1, mExplicitMDP, xVars, constraintLinExpr);

			// Expression -= in(i)
			GRBSolverUtils.addInTerm(i, -1, mExplicitMDP, xVars, constraintLinExpr);

			// Add constraint
			model.addConstr(constraintLinExpr, GRB.EQUAL, 0, constraintName);
		}
	}

	/**
	 * Add the source flow constraint C2: out(s0) - in(s0) = 1.
	 * 
	 * @param xVars
	 *            : Occupation measure variables
	 * @param model
	 *            : GRB model to which to add the source flow constraint
	 * @throws GRBException
	 */
	private void addSourceFlowConstraint(GRBVar[][] xVars, GRBModel model) throws GRBException {
		int iniState = mExplicitMDP.getInitialState();

		String constraintName = "constraintC3";
		// out(s0) - in(s0) = 1
		GRBLinExpr constraintLinExpr = new GRBLinExpr();

		// Expression += out(s0)
		GRBSolverUtils.addOutTerm(iniState, 1, mExplicitMDP, xVars, constraintLinExpr);

		// Expression -= in(s0)
		GRBSolverUtils.addInTerm(iniState, -1, mExplicitMDP, xVars, constraintLinExpr);

		// Add constraint
		model.addConstr(constraintLinExpr, GRB.EQUAL, 1, constraintName);
	}

	/**
	 * Add the sinks flow constraint C4: sum_{sg in G} (in(sg)) = 1.
	 * 
	 * @param xVars
	 *            : Occupation measure variables
	 * @param model
	 *            : GRB model to which to add the sinks flow constraint
	 * @throws GRBException
	 */
	private void addSinksFlowConstraint(GRBVar[][] xVars, GRBModel model) throws GRBException {
		String constraintName = "constraintC4";
		// sum_{sg in G} (in(sg)) = 1
		GRBLinExpr constraintLinExpr = new GRBLinExpr();

		for (Integer goal : mExplicitMDP.getGoalStates()) {
			// Expression += in(sg)
			GRBSolverUtils.addInTerm(goal, 1, mExplicitMDP, xVars, constraintLinExpr);
		}

		// Add constraint
		model.addConstr(constraintLinExpr, GRB.EQUAL, 1, constraintName);
	}

	private void verifyAllConstraints(double[][] xResults, double[][] deltaResults, double upperBoundOM) {
		double feasTol = mSettings.getFeasibilityTolerance();

		assert consistencyCheckFlowConservationConstraints(xResults);
		assert consistencyCheckSourceFlowConstraint(xResults);
		assert consistencyCheckSinksFlowConstraint(xResults);
		assert GRBSolverUtils.consistencyCheckDeltaConstraints(deltaResults, mExplicitMDP);
		assert GRBSolverUtils.consistencyCheckVarDeltaConstraints(xResults, deltaResults, upperBoundOM, mExplicitMDP,
				feasTol);
		if (mHardConstraints != null) {
			assert GRBSolverUtils.consistencyCheckCostConstraints(xResults, mHardConstraints, mExplicitMDP, feasTol);
		}
	}

	private boolean consistencyCheckFlowConservationConstraints(double[][] xResults) {
		int n = mExplicitMDP.getNumStates();
		Set<Integer> goals = mExplicitMDP.getGoalStates();
		int iniState = mExplicitMDP.getInitialState();

		for (int i = 0; i < n; i++) {
			if (goals.contains(Integer.valueOf(i)) || iniState == i) {
				// Exclude goal states G and initial state s0
				continue;
			}

			double outValue = GRBSolverUtils.getOutValue(i, xResults, mExplicitMDP);
			double inValue = GRBSolverUtils.getInValue(i, xResults, mExplicitMDP);
			boolean satisfied = GRBSolverUtils.approximatelyEqual(outValue, inValue,
					mSettings.getFeasibilityTolerance());

			if (!satisfied) {
				return false;
			}
		}
		return true;
	}

	private boolean consistencyCheckSourceFlowConstraint(double[][] xResults) {
		int iniState = mExplicitMDP.getInitialState();
		double outValue = GRBSolverUtils.getOutValue(iniState, xResults, mExplicitMDP);
		double inValue = GRBSolverUtils.getInValue(iniState, xResults, mExplicitMDP);
		return GRBSolverUtils.approximatelyEqual(outValue - inValue, 1, mSettings.getFeasibilityTolerance());
	}

	private boolean consistencyCheckSinksFlowConstraint(double[][] xResults) {
		double sum = 0;
		for (Integer goal : mExplicitMDP.getGoalStates()) {
			double inValue = GRBSolverUtils.getInValue(goal, xResults, mExplicitMDP);
			sum += inValue;
		}
		return GRBSolverUtils.approximatelyEqual(sum, 1, mSettings.getFeasibilityTolerance());
	}

}
