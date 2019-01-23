package solver.gurobiconnector;

import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;
import language.objectives.AttributeConstraint.BOUND_TYPE;
import solver.common.CostType;
import solver.common.ExplicitMDP;
import solver.common.ExplicitModelChecker;
import solver.common.NonStrictConstraint;

public class GRBSolverUtils {

	private static final double DEFAULT_FEASIBILITY_TOL = 1e-6;

	private GRBSolverUtils() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Create n-array of optimization variables, and add the variables to the model.
	 * 
	 * @param varName
	 *            : Variable name prefix
	 * @param grbVarType
	 *            : Variable type (continuous or binary)
	 * @param n
	 *            : Number of variables
	 * @param lowerBound
	 *            : Lower bound of the variables
	 * @param upperBound
	 *            : Upper bound of the variables
	 * @param model
	 *            : GRB model to which to add the variables
	 * @return n-array of optimization variables
	 * @throws GRBException
	 */
	public static GRBVar[] createOptimizationVars(String varName, char grbVarType, int n, double lowerBound,
			double upperBound, GRBModel model) throws GRBException {
		double lb = Double.isInfinite(lowerBound) ? -1 * GRB.INFINITY : lowerBound;
		double ub = Double.isInfinite(upperBound) ? GRB.INFINITY : upperBound;

		GRBVar[] vars = new GRBVar[n];
		for (int i = 0; i < n; i++) {
			String variName = varName + "_" + i;
			vars[i] = model.addVar(lb, ub, 0.0, grbVarType, variName);
		}
		return vars;
	}

	/**
	 * Create n x m matrix of optimization variables, and add the variables to the model.
	 * 
	 * @param varName
	 *            : Variable name prefix
	 * @param grbVarType
	 *            : Variable type (continuous or binary)
	 * @param n
	 *            : Number of rows (typically number of states)
	 * @param m
	 *            : Number of columns (typically number of actions)
	 * @param lowerBound
	 *            : Lower bound of the variables
	 * @param upperBound
	 *            : Upper bound of the variables
	 * @param model
	 *            : GRB model to which to add the variables
	 * @return n x m matrix of optimization variables
	 * @throws GRBException
	 */
	public static GRBVar[][] createOptimizationVars(String varName, char grbVarType, int n, int m, double lowerBound,
			double upperBound, GRBModel model) throws GRBException {
		double lb = Double.isInfinite(lowerBound) ? -1 * GRB.INFINITY : lowerBound;
		double ub = Double.isInfinite(upperBound) ? GRB.INFINITY : upperBound;

		GRBVar[][] vars = new GRBVar[n][m];
		for (int i = 0; i < n; i++) {
			for (int a = 0; a < m; a++) {
				// Add all variables var_ia to the model, but for action a that is not applicable in state i, the
				// variable var_ia will be excluded from the objective and constraints
				String variaName = varName + "_" + i + "_" + a;
				vars[i][a] = model.addVar(lb, ub, 0.0, grbVarType, variaName);
			}
		}
		return vars;
	}

	/**
	 * Set the optimization objective.
	 * 
	 * For transition costs: minimize sum_i,a(x_ia * c_ia).
	 * 
	 * For state costs: minimize sum_i,a(x_ia * c_i).
	 * 
	 * @param n
	 * @param m
	 * @param xVars
	 * @param model
	 * @throws GRBException
	 */
	public static void setOptimizationObjective(ExplicitMDP explicitMDP, GRBVar[][] xVars, GRBModel model)
			throws GRBException {
		int n = explicitMDP.getNumStates();
		int m = explicitMDP.getNumActions();

		// Objective: minimize sum_i,a(x_ia * c_ia)
		// OR
		// minimize sum_i,a(x_ia * c_i)

		// In this case, c_ia is an objective cost: c_0[i][a]
		// OR
		// c_i is an objective cost: c_0[i]
		GRBLinExpr objectiveLinExpr = new GRBLinExpr();
		for (int i = 0; i < n; i++) {
			for (int a = 0; a < m; a++) {
				// Exclude any x_ia term when action a is not applicable in state i
				if (explicitMDP.isActionApplicable(i, a)) {
					// Objective cost: c_ia
					// OR
					// c_i
					double objectiveCost = explicitMDP.getCostType() == CostType.TRANSITION_COST
							? explicitMDP.getObjectiveTransitionCost(i, a)
							: explicitMDP.getObjectiveStateCost(i);
					objectiveLinExpr.addTerm(objectiveCost, xVars[i][a]);
				}
			}
		}

		// Set objective
		model.setObjective(objectiveLinExpr, GRB.MINIMIZE);
	}

	/**
	 * Add Delta constraints: sum_a (Delta_ia) <= 1, for all i.
	 * 
	 * @param explicitMDP
	 * @param deltaVars
	 * @param model
	 * @throws GRBException
	 */
	public static void addDeltaConstraints(ExplicitMDP explicitMDP, GRBVar[][] deltaVars, GRBModel model)
			throws GRBException {
		int n = explicitMDP.getNumStates();
		int m = explicitMDP.getNumActions();

		// Constraints: sum_a (Delta_ia) <= 1, for all i
		for (int i = 0; i < n; i++) {
			String constraintName = "constraintDelta_" + i;
			GRBLinExpr constraintLinExpr = new GRBLinExpr();

			// sum_a (Delta_ia)
			for (int a = 0; a < m; a++) {
				// Exclude any Delta_ia term when action a is not applicable in state i
				if (explicitMDP.isActionApplicable(i, a)) {
					constraintLinExpr.addTerm(1.0, deltaVars[i][a]);
				}
			}

			// Add constraint: [...] <= 1
			model.addConstr(constraintLinExpr, GRB.LESS_EQUAL, 1, constraintName);
		}
	}

	/**
	 * Add x-Delta constraints: x_ia / X <= Delta_ia, for all i, a.
	 * 
	 * @param xMax
	 *            : Constant X >= x_ia for all i, a
	 * @param explicitMDP
	 * @param xVars
	 * @param deltaVars
	 * @param model
	 * @throws GRBException
	 */
	public static void addxDeltaConstraints(double xMax, ExplicitMDP explicitMDP, GRBVar[][] xVars,
			GRBVar[][] deltaVars, GRBModel model) throws GRBException {
		int n = explicitMDP.getNumStates();
		int m = explicitMDP.getNumActions();

		// Constraints: x_ia / X <= Delta_ia, for all i, a
		for (int i = 0; i < n; i++) {
			for (int a = 0; a < m; a++) {
				// Exclude any x_ia and Delta_ia terms when action a is not applicable in state i
				if (explicitMDP.isActionApplicable(i, a)) {
					String constaintName = "constraintxDelta_" + i + a;

					// x_ia / X
					GRBLinExpr lhsConstraintLinExpr = new GRBLinExpr();
					lhsConstraintLinExpr.addTerm(1.0 / xMax, xVars[i][a]);

					// Delta_ia
					GRBLinExpr rhsConstraintLinExpr = new GRBLinExpr();
					rhsConstraintLinExpr.addTerm(1.0, deltaVars[i][a]);

					// Add constraint
					model.addConstr(lhsConstraintLinExpr, GRB.LESS_EQUAL, rhsConstraintLinExpr, constaintName);
				}
			}
		}
	}

	/**
	 * Add coeff * in_v(i) term to a given linear expression, where in_v(i) = sum_j,a (v_ja * P(i|j,a)), for all i in S.
	 * 
	 * @param i
	 *            : State i
	 * @param coeff
	 *            : Coefficient of in_v(i) term in the linear expression
	 * @param explicitMDP
	 *            : ExplicitMDP
	 * @param vVars
	 *            : Variables of in_v(i) term
	 * @param linExpr
	 *            : Linear expression to which to add in_v(i) term
	 */
	public static void addInTerm(int i, double coeff, ExplicitMDP explicitMDP, GRBVar[][] vVars, GRBLinExpr linExpr) {
		int n = explicitMDP.getNumStates();
		int m = explicitMDP.getNumActions();

		// in_v(i) = sum_j,a (v_ja * P(i|j,a))
		// Expression += coeff * in_v(i)
		for (int j = 0; j < n; j++) {
			for (int a = 0; a < m; a++) {
				// Exclude any v_ja term when action a is not applicable in state j
				if (explicitMDP.isActionApplicable(j, a)) {
					double prob = explicitMDP.getTransitionProbability(j, a, i);
					linExpr.addTerm(coeff * prob, vVars[j][a]);
				}
			}
		}
	}

	/**
	 * Add coeff * out_v(i) term to a given linear expression, where out_v(i) = sum_a (v_ia), for all i in S \ G (if G
	 * exists).
	 * 
	 * @param i
	 *            : State i
	 * @param coeff
	 *            : Coefficient of out_v(i) term in the linear expression
	 * @param explicitMDP
	 *            : ExplicitMDP
	 * @param vVars
	 *            : Variables of out_v(i) term
	 * @param linExpr
	 *            : Linear expression to which to add out_v(i) term
	 */
	public static void addOutTerm(int i, double coeff, ExplicitMDP explicitMDP, GRBVar[][] vVars, GRBLinExpr linExpr) {
		int m = explicitMDP.getNumActions();

		// out_v(i) = sum_a (v_ia)
		// Expression += coeff * out_v(i)
		for (int a = 0; a < m; a++) {
			// Exclude any v_ia term when action a is not applicable in state i
			if (explicitMDP.isActionApplicable(i, a)) {
				linExpr.addTerm(coeff, vVars[i][a]);
			}
		}
	}

	/**
	 * Check whether the results of Delta_ia satisfy the constraints: sum_a (Delta_ia) <= 1, for all i.
	 * 
	 * @param deltaResults
	 * @param explicitMDP
	 * @return Whether the results of Delta_ia satisfy: sum_a (Delta_ia) <= 1, for all i
	 */
	static boolean consistencyCheckDeltaConstraints(double[][] deltaResults, ExplicitMDP explicitMDP) {
		int n = explicitMDP.getNumStates();
		int m = explicitMDP.getNumActions();

		for (int i = 0; i < n; i++) {
			double sum = 0;
			for (int a = 0; a < m; a++) {
				if (explicitMDP.isActionApplicable(i, a)) {
					sum += deltaResults[i][a];
				}
			}
			if (sum > 1) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Check whether the results of x_ia and Delta_ia satisfy the constraints: x_ia / X <= Delta_ia, for all i, a.
	 * 
	 * @param xResults
	 * @param deltaResults
	 * @param xMax
	 * @param explicitMDP
	 * @return Whether the results of x_ia and Delta_ia satisfy: x_ia / X <= Delta_ia, for all i, a
	 */
	static boolean consistencyCheckxDeltaConstraints(double[][] xResults, double[][] deltaResults, double xMax,
			ExplicitMDP explicitMDP) {
		int n = explicitMDP.getNumStates();
		int m = explicitMDP.getNumActions();

		for (int i = 0; i < n; i++) {
			for (int a = 0; a < m; a++) {
				if (explicitMDP.isActionApplicable(i, a)) {
					double xResult = xResults[i][a];
					double deltaResult = deltaResults[i][a];
					if (xResult / xMax > deltaResult) {
						return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * Check whether the results of x_ia satisfies the constraints: sum_i,a(c^k_ia * x_ia) <= upper bound of c^k (or >=
	 * lower bound of c^k), for all k.
	 * 
	 * @param xResults
	 * @param hardConstraints
	 *            : Non-strict hard constraints
	 * @param explicitMDP
	 * @return Whether the results of x_ia satisfies: sum_i,a(c^k_ia * x_ia) <= upper bound of c^k, for all k
	 */
	static boolean consistencyCheckCostConstraints(double[][] xResults, NonStrictConstraint[] hardConstraints,
			ExplicitMDP explicitMDP) {
		for (int k = 1; k < hardConstraints.length; k++) {
			if (hardConstraints[k] == null) {
				// Skip -- there is no constraint on this cost function k
				continue;
			}

			NonStrictConstraint hardConstraint = hardConstraints[k];
			double occupancyCost = ExplicitModelChecker.computeOccupancyCost(explicitMDP, k, xResults);
			boolean satisfyConstraint = hardConstraint.getBoundType() == BOUND_TYPE.UPPER_BOUND
					? occupancyCost <= hardConstraint.getBoundValue()
					: occupancyCost >= hardConstraint.getBoundValue();

			if (!satisfyConstraint) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Check, for all states i such that sum_a(x_ia) > 0, whether the property Delta_ia = 1 <=> x_ia > 0 holds.
	 * 
	 * @param xResults
	 * @param deltaResults
	 * @return Whether the property Delta_ia = 1 <=> x_ia > 0 holds for all states i such that sum_a(x_ia) > 0
	 */
	static boolean consistencyCheckResults(double[][] xResults, double[][] deltaResults, ExplicitMDP explicitMDP) {
		int n = explicitMDP.getNumStates();
		int m = explicitMDP.getNumActions();

		for (int i = 0; i < n; i++) {
			if (hasNonZeroProbVisited(i, xResults, explicitMDP)) {
				// sum_a(x_ia) > 0

				for (int a = 0; a < m; a++) {
					// Exclude any x_ia and Delta_ia terms when action a is not applicable in state i
					if (explicitMDP.isActionApplicable(i, a)) {
						double deltaResult = deltaResults[i][a];
						double xResult = xResults[i][a];

						if (!checkResultsConsistency(deltaResult, xResult)) {
							return false;
						}
					}
				}
			}
		}
		return true;
	}

	/**
	 * Check whether the state i has non-zero probability of being visited, i.e., sum_a(x_ia) > 0.
	 * 
	 * @param i
	 * @param xResults
	 * @param explicitMDP
	 * @return Whether the state i has non-zero probability of being visited
	 */
	private static boolean hasNonZeroProbVisited(int i, double[][] xResults, ExplicitMDP explicitMDP) {
		int m = explicitMDP.getNumActions();

		// sum_a(x_ia)
		double probVisited = 0;
		for (int a = 0; a < m; a++) {
			if (explicitMDP.isActionApplicable(i, a)) {
				probVisited += xResults[i][a];
			}
		}

		// Check whether sum_a(x_ia) > 0
		return probVisited > 0;
	}

	private static boolean checkResultsConsistency(double deltaResult, double xResult) {
		return (deltaResult == 1 && xResult > 0) || (deltaResult == 0 && xResult == 0);
	}

	/**
	 * Check whether the policy is deterministic.
	 * 
	 * @param policy
	 * @param explicitMDP
	 * @return Whether the policy is deterministic
	 */
	static boolean consistencyCheckDeterministicPolicy(double[][] policy, ExplicitMDP explicitMDP) {
		for (int i = 0; i < policy.length; i++) {
			for (int a = 0; a < policy[0].length; a++) {
				// Exclude any pi_ia term when action a is not applicable in state i
				if (explicitMDP.isActionApplicable(i, a)) {
					double pi = policy[i][a];

					// Check for any randomized decision
					if (pi > 0 && pi < 1) {
						return false;
					}
				}
			}
		}
		return true;
	}

	static double getInValue(int i, double[][] xResults, ExplicitMDP explicitMDP) {
		int n = explicitMDP.getNumStates();
		int m = explicitMDP.getNumActions();
		double inValue = 0;

		// in(i) = sum_j,a (x_ja * P(i|j,a))
		for (int j = 0; j < n; j++) {
			for (int a = 0; a < m; a++) {
				// Exclude any x_ja term when action a is not applicable in state j
				if (explicitMDP.isActionApplicable(j, a)) {
					double prob = explicitMDP.getTransitionProbability(j, a, i);
					inValue += prob * xResults[j][a];
				}
			}
		}
		return inValue;
	}

	static double getOutValue(int i, double[][] xResults, ExplicitMDP explicitMDP) {
		int m = explicitMDP.getNumActions();
		double outValue = 0;

		// out(i) = sum_a (x_ia)
		for (int a = 0; a < m; a++) {
			// Exclude any x_ia term when action a is not applicable in state i
			if (explicitMDP.isActionApplicable(i, a)) {
				outValue += xResults[i][a];
			}
		}
		return outValue;
	}

	static boolean approximatelyEquals(double valueA, double valueB) {
		return Math.abs(valueA - valueB) <= DEFAULT_FEASIBILITY_TOL;
	}
}
