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

	public static final double DEFAULT_INT_FEAS_TOL = 1e-7;
	public static final double DEFAULT_FEASIBILITY_TOL = 1e-7;
	public static final double DEFAULT_OPT_TOL = 1e-6;
	public static final double DEFAULT_ROUND_OFF = 1e-5;

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
	 * @param deltaVarName
	 * @param deltaVars
	 * @param model
	 * @throws GRBException
	 */
	public static void addDeltaConstraints(ExplicitMDP explicitMDP, String deltaVarName, GRBVar[][] deltaVars,
			GRBModel model) throws GRBException {
		int n = explicitMDP.getNumStates();
		int m = explicitMDP.getNumActions();

		// Constraints: sum_a (Delta_ia) <= 1, for all i
		for (int i = 0; i < n; i++) {
			String constraintName = "constraint_" + deltaVarName + "_" + i;
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
	 * Add {var}-Delta{var} constraints: {var}_ia / V <= Delta{var}_ia, for all i, a.
	 * 
	 * @param vUpperBound
	 *            : Constant V >= {var}_ia for all i, a
	 * @param explicitMDP
	 *            : Explicit MDP
	 * @param vVarName
	 *            : Continuous variable name
	 * @param vVars
	 *            : Continuous variables
	 * @param deltavVarName
	 *            : Binary variable name
	 * @param deltavVars
	 *            : Binary variables
	 * @param model
	 *            : GRB model
	 * @throws GRBException
	 */
	public static void addVarDeltaConstraints(double vUpperBound, ExplicitMDP explicitMDP, String vVarName,
			GRBVar[][] vVars, String deltavVarName, GRBVar[][] deltavVars, GRBModel model) throws GRBException {
		int n = explicitMDP.getNumStates();
		int m = explicitMDP.getNumActions();

		// Constraints: x_ia / X <= Deltax_ia, for all i, a
		// OR
		// y_ia / Y <= Deltay_ia, for all i, a
		for (int i = 0; i < n; i++) {
			for (int a = 0; a < m; a++) {
				// Exclude any v_ia and Deltav_ia terms when action a is not applicable in state i
				if (explicitMDP.isActionApplicable(i, a)) {
					String constaintName = "constraint_" + vVarName + "_" + deltavVarName + "_" + i + "_" + a;

					// v_ia / V
					GRBLinExpr lhsConstraintLinExpr = new GRBLinExpr();
					lhsConstraintLinExpr.addTerm(1.0 / vUpperBound, vVars[i][a]);

					// Deltav_ia
					GRBLinExpr rhsConstraintLinExpr = new GRBLinExpr();
					rhsConstraintLinExpr.addTerm(1.0, deltavVars[i][a]);

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

	public static void configureToleranceParameters(GRBModel model, double intFeasTol, double feasibilityTol)
			throws GRBException {
		model.set(GRB.DoubleParam.IntFeasTol, intFeasTol);
		model.set(GRB.DoubleParam.FeasibilityTol, feasibilityTol);
		model.set(GRB.DoubleParam.OptimalityTol, DEFAULT_OPT_TOL);
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
	 * Check whether the results of v_ia and Deltav_ia satisfy the constraints: v_ia / V <= Deltav_ia, for all i, a.
	 * 
	 * @param vResults
	 * @param deltavResults
	 * @param vUpperBound
	 * @param explicitMDP
	 * @param feasibilityTol
	 * @return Whether the results of v_ia and Deltav_ia satisfy: v_ia / V <= Deltav_ia, for all i, a
	 */
	static boolean consistencyCheckVarDeltaConstraints(double[][] vResults, double[][] deltavResults,
			double vUpperBound, ExplicitMDP explicitMDP, double feasibilityTol) {
		int n = explicitMDP.getNumStates();
		int m = explicitMDP.getNumActions();

		for (int i = 0; i < n; i++) {
			for (int a = 0; a < m; a++) {
				if (explicitMDP.isActionApplicable(i, a)) {
					double vResult = vResults[i][a];
					double deltavResult = deltavResults[i][a];
					boolean satisfiedConstraint = vResult / vUpperBound <= deltavResult + feasibilityTol;

					if (!satisfiedConstraint) {
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
	 * @param feasibilityTol
	 * @return Whether the results of x_ia satisfies: sum_i,a(c^k_ia * x_ia) <= upper bound of c^k, for all k
	 */
	static boolean consistencyCheckCostConstraints(double[][] xResults, NonStrictConstraint[] hardConstraints,
			ExplicitMDP explicitMDP, double feasibilityTol) {
		for (int k = 1; k < hardConstraints.length; k++) {
			if (hardConstraints[k] == null) {
				// Skip -- there is no constraint on this cost function k
				continue;
			}

			NonStrictConstraint hardConstraint = hardConstraints[k];
			double occupancyCost = ExplicitModelChecker.computeOccupancyCost(xResults, k, explicitMDP);
			boolean satisfiedConstraint = hardConstraint.getBoundType() == BOUND_TYPE.UPPER_BOUND
					? occupancyCost <= hardConstraint.getBoundValue() + feasibilityTol
					: occupancyCost >= hardConstraint.getBoundValue() - feasibilityTol;

			if (!satisfiedConstraint) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Check, for all states i such that sum_a(v_ia) > 0, whether the property Deltav_ia = 1 <=> v_ia > 0 holds.
	 * 
	 * @param vResults
	 * @param deltavResults
	 * @param explicitMDP
	 * @param feasibilityTol
	 * @return Whether the property Deltav_ia = 1 <=> v_ia > 0 holds for all states i such that sum_a(v_ia) > 0
	 */
	static boolean consistencyCheckResults(double[][] vResults, double[][] deltavResults, ExplicitMDP explicitMDP,
			double feasibilityTol) {
		int n = explicitMDP.getNumStates();
		int m = explicitMDP.getNumActions();

		for (int i = 0; i < n; i++) {
			// For x: sum_a(x_ia) = probability of state i being visited
			double outValue = getOutValue(i, vResults, explicitMDP);

			// For all states i such that sum_a(v_ia) > 0
			if (outValue > 0 + feasibilityTol) {
				for (int a = 0; a < m; a++) {
					// Exclude any v_ia and Deltav_ia terms when action a is not applicable in state i
					if (explicitMDP.isActionApplicable(i, a)) {
						double deltavResult = deltavResults[i][a];
						double vResult = vResults[i][a];
						boolean consistent = checkResultsConsistency(deltavResult, vResult, feasibilityTol);

						if (!consistent) {
							return false;
						}
					}
				}
			}
		}
		return true;
	}

	/**
	 * Check if (deltavResult == 1 && vResult > 0) || (deltavResult == 0 && vResult == 0).
	 * 
	 * @param deltavResult
	 * @param vResult
	 * @param feasibilityTol
	 * @return (deltavResult == 1 && vResult > 0) || (deltavResult == 0 && vResult == 0)
	 */
	private static boolean checkResultsConsistency(double deltavResult, double vResult, double feasibilityTol) {
		return (approximatelyEqual(deltavResult, 1, feasibilityTol) && vResult > 0)
				|| (approximatelyEqual(deltavResult, 0, feasibilityTol)
						&& approximatelyEqual(vResult, 0, feasibilityTol));
	}

	/**
	 * Check whether the policy is deterministic.
	 * 
	 * @param policy
	 * @param explicitMDP
	 * @param feasibilityTol
	 * @return Whether the policy is deterministic
	 */
	static boolean consistencyCheckDeterministicPolicy(double[][] policy, ExplicitMDP explicitMDP,
			double feasibilityTol) {
		for (int i = 0; i < policy.length; i++) {
			for (int a = 0; a < policy[0].length; a++) {
				// Exclude any pi_ia term when action a is not applicable in state i
				if (explicitMDP.isActionApplicable(i, a)) {
					double pi = policy[i][a];
					boolean isDeterministic = approximatelyEqual(pi, 0, feasibilityTol)
							|| approximatelyEqual(pi, 1, feasibilityTol);

					// Check for any randomized decision
					if (!isDeterministic) {
						return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * Compute in_v(i) = sum_j,a (v_ja * P(i|j,a)).
	 * 
	 * @param i
	 *            : State
	 * @param vResults
	 * @param explicitMDP
	 * @return in_v(i) = sum_j,a (v_ja * P(i|j,a))
	 */
	static double getInValue(int i, double[][] vResults, ExplicitMDP explicitMDP) {
		int n = explicitMDP.getNumStates();
		int m = explicitMDP.getNumActions();
		double inValue = 0;

		// in_v(i) = sum_j,a (v_ja * P(i|j,a))
		for (int j = 0; j < n; j++) {
			for (int a = 0; a < m; a++) {
				// Exclude any v_ja term when action a is not applicable in state j
				if (explicitMDP.isActionApplicable(j, a)) {
					double prob = explicitMDP.getTransitionProbability(j, a, i);
					inValue += prob * vResults[j][a];
				}
			}
		}
		return inValue;
	}

	/**
	 * Compute out_v(i) = sum_a (v_ia).
	 * 
	 * @param i
	 *            : State
	 * @param vResults
	 * @param explicitMDP
	 * @return out_v(i) = sum_a (v_ia)
	 */
	static double getOutValue(int i, double[][] vResults, ExplicitMDP explicitMDP) {
		int m = explicitMDP.getNumActions();
		double outValue = 0;

		// out_v(i) = sum_a (v_ia)
		for (int a = 0; a < m; a++) {
			// Exclude any v_ia term when action a is not applicable in state i
			if (explicitMDP.isActionApplicable(i, a)) {
				outValue += vResults[i][a];
			}
		}
		return outValue;
	}

	static boolean approximatelyEqual(double valueA, double valueB, double feasibilityTol) {
		double diff = Math.abs(valueA - valueB);
		return diff <= feasibilityTol;
	}
}
