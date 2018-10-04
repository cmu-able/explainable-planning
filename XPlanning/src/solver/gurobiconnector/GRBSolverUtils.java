package solver.gurobiconnector;

import java.util.Arrays;
import java.util.Set;

import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;
import language.exceptions.QFunctionNotFoundException;
import language.metrics.IQFunction;
import language.objectives.AttributeConstraint;
import solver.common.CostType;
import solver.common.ExplicitMDP;
import solver.prismconnector.QFunctionEncodingScheme;

public class GRBSolverUtils {

	private static final double DEFAULT_FEASIBILITY_TOL = 1e-6;

	/**
	 * For approximating strict inequality.
	 */
	private static final double TOLERANCE_FACTOR = 0.99;

	private GRBSolverUtils() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Create an array of upper-bounds on QAs. The indices of the upper-bounds are aligned with those of the cost
	 * functions in {@link ExplicitMDP}. An upper-bound is null iff the corresponding cost function doesn't have a
	 * constraint.
	 * 
	 * @param attrConstraints
	 *            : Upper-bound constraints on QA values
	 * @param qFunctionEncoding
	 *            : QA-function encoding scheme
	 * @return Array of upper-bounds on QAs
	 * @throws QFunctionNotFoundException
	 */
	public static Double[] createUpperBounds(Set<AttributeConstraint<IQFunction<?, ?>>> attrConstraints,
			QFunctionEncodingScheme qFunctionEncoding) throws QFunctionNotFoundException {
		// Constraints are on the cost functions starting from index 1 in ExplicitMDP
		// Align the indices of the constraints to those of the cost functions in ExplicitMDP
		Double[] upperBoundConstraints = new Double[qFunctionEncoding.getNumRewardStructures() + 1];

		// Set upper bound to null for all cost-function indices that don't have constraints
		Arrays.fill(upperBoundConstraints, null);

		for (AttributeConstraint<IQFunction<?, ?>> attrConstraint : attrConstraints) {
			IQFunction<?, ?> qFunction = attrConstraint.getQFunction();
			double upperBound = attrConstraint.getExpectedTotalUpperBound();
			int costFuncIndex = qFunctionEncoding.getRewardStructureIndex(qFunction);

			if (attrConstraint.isStrictBound()) {
				upperBoundConstraints[costFuncIndex] = TOLERANCE_FACTOR * upperBound;
			} else {
				upperBoundConstraints[costFuncIndex] = upperBound;
			}
		}

		return upperBoundConstraints;
	}

	/**
	 * Create n x m matrix of continuous optimization variables: v_ia, where v_ia >=0 for all i, a.
	 * 
	 * @param n
	 *            : Number of states
	 * @param m
	 *            : Number of actions
	 * @param model
	 *            : GRB model to which to add the variables v_ia
	 * @return n x m matrix of continuous optimization variables: v_ia, where v_ia >=0 for all i, a
	 * @throws GRBException
	 */
	public static GRBVar[][] createContinuousOptimizationVars(String varName, int n, int m, GRBModel model)
			throws GRBException {
		// Variables: {varName}_ia
		// Lower bound on variables: {varName}_ia >= 0
		GRBVar[][] vars = new GRBVar[n][m];
		for (int i = 0; i < n; i++) {
			for (int a = 0; a < m; a++) {
				// Add all variables v_ia to the model, but for action a that is not applicable in state i, the variable
				// v_ia will be excluded from the objective and constraints
				String elemVarName = varName + "_" + i + a;
				vars[i][a] = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, elemVarName);
			}
		}
		return vars;
	}

	/**
	 * Create n x m matrix of binary optimization variables: Delta_ia.
	 * 
	 * @param n
	 *            : Number of states
	 * @param m
	 *            : Number of actions
	 * @param model
	 *            : GRB model to which to add the variables Delta_ia
	 * @return n x m matrix of binary optimization variables: Delta_ia
	 * @throws GRBException
	 */
	public static GRBVar[][] createBinaryOptimizationVars(String varName, int n, int m, GRBModel model)
			throws GRBException {
		GRBVar[][] deltaVars = new GRBVar[n][m];
		for (int i = 0; i < n; i++) {
			for (int a = 0; a < m; a++) {
				// Add all variables Delta_ia to the model, but for action a that is not applicable in state i, the
				// variable Delta_ia will be excluded from the objective and constraints
				String deltaVarName = varName + "_" + i + a;
				deltaVars[i][a] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, deltaVarName);
			}
		}
		return deltaVars;
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
					String constaintName = "constraintEq9_" + i + a;

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
	 * Add cost constraints according to a given upper-bounds.
	 * 
	 * For transition costs: sum_i,a(c^k_ia * x_ia) <= upper bound of c^k, for all k.
	 * 
	 * For state costs: sum_i,a(c^k_i * x_ia) <= upper bound of c^k, for all k.
	 * 
	 * @param upperBounds
	 *            : Upper-bounds on costs
	 * @param explicitMDP
	 *            : ExplicitMDP
	 * @param xVars
	 *            : Optimization x variables
	 * @param model
	 *            : GRB model to which to add the cost constraints
	 * @throws GRBException
	 */
	public static void addCostConstraints(Double[] upperBounds, ExplicitMDP explicitMDP, GRBVar[][] xVars,
			GRBModel model) throws GRBException {
		// Constraints: sum_i,a(c^k_ia * x_ia) <= upper bound of c^k, for all k
		// OR
		// sum_i,a(c^k_i * x_ia) <= upper bound of c^k, for all k

		// Non-objective cost functions start at index 1 in ExplicitMDP
		for (int k = 1; k < upperBounds.length; k++) {
			if (upperBounds[k] == null) {
				// Skip -- there is no constraint on this cost function k
				continue;
			}

			GRBSolverUtils.addCostConstraint(k, upperBounds[k], explicitMDP, xVars, model);
		}
	}

	/**
	 * Add the cost-k constraint C5: sum_i,a (x_ia * C_k(i,a)) <= upper bound on c_k.
	 * 
	 * @param costFuncIndex
	 *            : Index of constrained cost function
	 * @param upperBound
	 *            : Upper bound on the constrained cost
	 * @param explicitMDP
	 *            : Explicit MDP
	 * @param xVars
	 *            : Optimization x variables
	 * @param model
	 *            : GRB model to which to add the cost-k constraint
	 * @throws GRBException
	 */
	public static void addCostConstraint(int costFuncIndex, double upperBound, ExplicitMDP explicitMDP,
			GRBVar[][] xVars, GRBModel model) throws GRBException {
		int n = explicitMDP.getNumStates();
		int m = explicitMDP.getNumActions();

		String constraintName = "constraintC5_" + costFuncIndex;
		// sum_i,a (x_ia * C_k(i,a)) <= upper bound on c_k
		GRBLinExpr constraintLinExpr = new GRBLinExpr();

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

		// Add constraint
		model.addConstr(constraintLinExpr, GRB.LESS_EQUAL, upperBound, constraintName);
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
	 * Check whether the results of x_ia satisfies the constraints: sum_i,a(c^k_ia * x_ia) <= upper bound of c^k, for
	 * all k.
	 * 
	 * @param xResults
	 * @param upperBounds
	 * @param explicitMDP
	 * @return Whether the results of x_ia satisfies: sum_i,a(c^k_ia * x_ia) <= upper bound of c^k, for all k
	 */
	static boolean consistencyCheckCostConstraints(double[][] xResults, Double[] upperBounds, ExplicitMDP explicitMDP) {
		for (int k = 1; k < upperBounds.length; k++) {
			if (upperBounds[k] == null) {
				// Skip -- there is no constraint on this cost function k
				continue;
			}

			if (!GRBSolverUtils.consistencyCheckCostConstraint(xResults, explicitMDP, k, upperBounds[k])) {
				return false;
			}
		}
		return true;
	}

	static boolean consistencyCheckCostConstraint(double[][] xResults, ExplicitMDP explicitMDP, int costFuncIndex,
			double upperBound) {
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
		return sum <= upperBound;
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
