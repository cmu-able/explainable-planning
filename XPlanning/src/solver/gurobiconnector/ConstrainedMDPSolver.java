package solver.gurobiconnector;

import java.util.Arrays;
import java.util.Set;

import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;
import language.exceptions.QFunctionNotFoundException;
import language.metrics.IQFunction;
import language.metrics.ITransitionStructure;
import language.objectives.AttributeConstraint;
import language.objectives.AttributeCostFunction;
import language.objectives.IAdditiveCostFunction;
import language.qfactors.IAction;
import solver.common.CostType;
import solver.common.ExplicitMDP;
import solver.prismconnector.QFunctionEncodingScheme;

public class ConstrainedMDPSolver {

	private static final double TOLERANCE_FACTOR = 0.99;

	private ExplicitMDP mExplicitMDP;
	private Double[] mUpperBoundConstraints;

	public ConstrainedMDPSolver(ExplicitMDP explicitMDP, IAdditiveCostFunction objectiveFunction,
			Set<AttributeConstraint<IQFunction<?, ?>>> attrConstraints, QFunctionEncodingScheme qFunctionEncoding)
			throws QFunctionNotFoundException {
		mExplicitMDP = explicitMDP;
		setObjectiveFunctionOfExplicitMDP(objectiveFunction, qFunctionEncoding);
		setUpperBoundConstraints(attrConstraints, qFunctionEncoding);
	}

	/**
	 * Set the objective function of the ExplicitMDP.
	 * 
	 * If the cost type is transition, objective cost of a transition is: c_0[i][a] = sum_k(scaling const * attribute
	 * cost of c_k[i][a]).
	 * 
	 * If the cost type is state, objective cost of a state is: c_0[i] = sum_k(scaling const * attribute cost of
	 * c_k[i]).
	 * 
	 * @param objectiveFunction
	 * @param qFunctionEncoding
	 * @throws QFunctionNotFoundException
	 */
	private void setObjectiveFunctionOfExplicitMDP(IAdditiveCostFunction objectiveFunction,
			QFunctionEncodingScheme qFunctionEncoding) throws QFunctionNotFoundException {
		int numStates = mExplicitMDP.getNumStates();
		int numActions = mExplicitMDP.getNumActions();

		Set<IQFunction<IAction, ITransitionStructure<IAction>>> qFunctions = objectiveFunction.getQFunctions();

		for (int i = 0; i < numStates; i++) {
			for (int a = 0; a < numActions; a++) {
				// Objective cost of a transition: c_0[i][a] = sum_k(scaling const * attribute cost of c_k[i][a])
				// OR
				// Objective cost of a state: c_0[i] = sum_k(scaling const * attribute cost of c_k[i])
				double objectiveCost = 0;

				for (IQFunction<?, ?> qFunction : qFunctions) {
					AttributeCostFunction<?> attrCostFunc = objectiveFunction.getAttributeCostFunction(qFunction);
					double scalingConst = objectiveFunction.getScalingConstant(attrCostFunc);

					// When constructing ExplicitMDP, we ensure that the indices of the cost functions of ExplicitMDP
					// are aligned with the indices of the PRISM reward structures
					int costFuncIndex = qFunctionEncoding.getRewardStructureIndex(qFunction);

					// QA value of a single transition
					// OR
					// QA value of a single state
					double stepQValue = isTransitionCost() ? mExplicitMDP.getTransitionCost(costFuncIndex, i, a)
							: mExplicitMDP.getStateCost(costFuncIndex, i);

					// Attribute cost of the transition value
					// OR
					// Attribute cost of the state value
					double stepAttrCost = attrCostFunc.getCost(stepQValue);

					objectiveCost += scalingConst * stepAttrCost;
				}

				// Set objective cost at c_0[i][a]
				// OR
				// Set objective cost at c_0[i]
				if (isTransitionCost()) {
					mExplicitMDP.addTransitionCost(ExplicitMDP.OBJECTIVE_FUNCTION_INDEX, i, a, objectiveCost);
				} else {
					mExplicitMDP.addStateCost(ExplicitMDP.OBJECTIVE_FUNCTION_INDEX, i, objectiveCost);
				}
			}
		}

	}

	private void setUpperBoundConstraints(Set<AttributeConstraint<IQFunction<?, ?>>> attrConstraints,
			QFunctionEncodingScheme qFunctionEncoding) throws QFunctionNotFoundException {
		// Constraints are on the cost functions starting from index 1 in ExplicitMDP
		// Align the indices of the constraints to those of the cost functions in ExplicitMDP
		mUpperBoundConstraints = new Double[mExplicitMDP.getNumCostFunctions()];

		// Set upper bound to null for all cost-function indices that don't have constraints
		Arrays.fill(mUpperBoundConstraints, null);

		for (AttributeConstraint<IQFunction<?, ?>> attrConstraint : attrConstraints) {
			IQFunction<?, ?> qFunction = attrConstraint.getQFunction();
			double upperBound = attrConstraint.getExpectedTotalUpperBound();
			int costFuncIndex = qFunctionEncoding.getRewardStructureIndex(qFunction);

			if (attrConstraint.isStrictBound()) {
				mUpperBoundConstraints[costFuncIndex] = TOLERANCE_FACTOR * upperBound;
			} else {
				mUpperBoundConstraints[costFuncIndex] = upperBound;
			}
		}
	}

	public double[][] solveOptimalPolicy() throws GRBException {
		int n = mExplicitMDP.getNumStates();
		int m = mExplicitMDP.getNumActions();
		double[][] policy = new double[n][m];
		double[][] occupationMeasure = solveOccupationMeasure();

		for (int i = 0; i < n; i++) {
			// sum_a(x_ia)
			double denom = 0;
			for (int a = 0; a < m; a++) {
				// Exclude any x_ia value when action a is not applicable in state i
				if (mExplicitMDP.isActionApplicable(i, a)) {
					denom += occupationMeasure[i][a];
				}
			}

			if (denom > 0) {
				// Interpret occupation measure x_ia as the total expected discounted number of times action a is
				// executed in state i.
				// When sum_a(x_ia) > 0, it means state i is reachable.

				for (int a = 0; a < m; a++) {
					// Exclude any x_ia value when action a is not applicable in state i
					if (mExplicitMDP.isActionApplicable(i, a)) {
						// pi_ia = x_ia / sum_a(x_ia)
						policy[i][a] = occupationMeasure[i][a] / denom;
					}
				}
			}
		}

		assert consistencyCheckDeterministicPolicy(policy);
		return policy;
	}

	public double[][] solveOccupationMeasure() throws GRBException {
		GRBEnv env = new GRBEnv();
		GRBModel model = new GRBModel(env);

		int n = mExplicitMDP.getNumStates();
		int m = mExplicitMDP.getNumActions();

		// Create variables: x_ia
		// Lower bound on variables: x_ia >= 0
		GRBVar[][] xVars = GRBSolverUtils.createOccupationMeasureVars(mExplicitMDP, model);

		// Create variables: Delta_ia (binary)
		GRBVar[][] deltaVars = createDeltaVars(n, m, model);

		// Set optimization objective
		setOptimizationObjective(n, m, xVars, model);

		// Add constraints
		GRBSolverUtils.addFlowConservationConstraints(mExplicitMDP, xVars, model);
		GRBSolverUtils.addSourceFlowConstraint(mExplicitMDP, xVars, model);
		GRBSolverUtils.addSinksFlowConstraint(mExplicitMDP, xVars, model);
		addCostConstraints(xVars, model);
		addDeltaConstraints(n, m, deltaVars, model);
		double upperBoundOM = addOMDeltaConstraints(n, m, xVars, deltaVars, model);

		// Solve optimization problem for x_ia and Delta_ia
		model.optimize();

		// Query results: optimal values of x_ia and Delta_ia
		double[][] xResults = model.get(GRB.DoubleAttr.X, xVars);
		double[][] deltaResults = model.get(GRB.DoubleAttr.X, deltaVars);

		// Dispose of model and environment
		model.dispose();
		env.dispose();

		consistencyCheckConstraints(n, m, upperBoundOM, xResults, deltaResults);
		assert consistencyCheckResults(xResults, deltaResults);

		return xResults;
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
	private GRBVar[][] createDeltaVars(int n, int m, GRBModel model) throws GRBException {
		GRBVar[][] deltaVars = new GRBVar[n][m];
		for (int i = 0; i < n; i++) {
			for (int a = 0; a < m; a++) {
				// Add all variables Delta_ia to the model, but for action a that is not applicable in state i, the
				// variable Delta_ia will be excluded from the objective and constraints
				String deltaVarName = "Delta_" + i + a;
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
	private void setOptimizationObjective(int n, int m, GRBVar[][] xVars, GRBModel model) throws GRBException {
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
				if (mExplicitMDP.isActionApplicable(i, a)) {
					// Objective cost: c_ia
					// OR
					// c_i
					double objectiveCost = isTransitionCost()
							? mExplicitMDP.getTransitionCost(ExplicitMDP.OBJECTIVE_FUNCTION_INDEX, i, a)
							: mExplicitMDP.getStateCost(ExplicitMDP.OBJECTIVE_FUNCTION_INDEX, i);
					objectiveLinExpr.addTerm(objectiveCost, xVars[i][a]);
				}
			}
		}

		// Set objective
		model.setObjective(objectiveLinExpr, GRB.MINIMIZE);
	}

	/**
	 * Add constraints: sum_a(x_ja) - gamma * sum_i,a(x_ia * p_iaj) = alpha_j, for all j.
	 * 
	 * @param n
	 * @param m
	 * @param xVars
	 * @param deltaVars
	 * @param model
	 * @throws GRBException
	 */
	private void addConstraintsA(int n, int m, GRBVar[][] xVars, GRBModel model) throws GRBException {
		// Initial state distribution
		double[] alpha = new double[n];
		int iniState = mExplicitMDP.getInitialState();
		alpha[iniState] = 1.0;

		double gamma = ExplicitMDP.DEFAULT_DISCOUNT_FACTOR;

		// Constraints: sum_a(x_ja) - gamma * sum_i,a(x_ia * p_iaj) = alpha_j, for all j
		for (int j = 0; j < n; j++) {
			String constraintName = "constraintA_" + j;
			GRBLinExpr constraintLinExpr = new GRBLinExpr();

			// sum_a(x_ja)
			for (int a = 0; a < m; a++) {
				// Exclude any x_ia term when action a is not applicable in state i
				if (mExplicitMDP.isActionApplicable(j, a)) {
					constraintLinExpr.addTerm(1.0, xVars[j][a]);
				}
			}

			// - gamma * sum_i,a(x_ia * p_iaj)
			double coeff = -1 * gamma;
			for (int i = 0; i < n; i++) {
				for (int a = 0; a < m; a++) {
					// Exclude any x_ia term when action a is not applicable in state i
					if (mExplicitMDP.isActionApplicable(i, a)) {
						double p = mExplicitMDP.getTransitionProbability(i, a, j);
						double termCoeff = coeff * p;

						// - gamma * p_iaj * x_ia
						constraintLinExpr.addTerm(termCoeff, xVars[i][a]);
					}
				}
			}

			// Add constraint: [...] = alpha_j
			model.addConstr(constraintLinExpr, GRB.EQUAL, alpha[j], constraintName);
		}
	}

	/**
	 * Add cost constraints.
	 * 
	 * For transition costs: sum_i,a(c^k_ia * x_ia) <= upper bound of c^k, for all k.
	 * 
	 * For state costs: sum_i,a(c^k_i * x_ia) <= upper bound of c^k, for all k.
	 * 
	 * @param xVars
	 * @param model
	 * @throws GRBException
	 */
	private void addCostConstraints(GRBVar[][] xVars, GRBModel model) throws GRBException {
		// Constraints: sum_i,a(c^k_ia * x_ia) <= upper bound of c^k, for all k
		// OR
		// sum_i,a(c^k_i * x_ia) <= upper bound of c^k, for all k

		// Non-objective cost functions start at index 1 in ExplicitMDP
		for (int k = 1; k < mUpperBoundConstraints.length; k++) {
			if (mUpperBoundConstraints[k] == null) {
				// Skip -- there is no constraint on this cost function k
				continue;
			}

			GRBSolverUtils.addCostConstraint(k, mUpperBoundConstraints[k], mExplicitMDP, xVars, model);
		}
	}

	/**
	 * Add Delta constraints (from Eq. 8): sum_a(Delta_ia) <= 1, for all i.
	 * 
	 * @param n
	 * @param m
	 * @param deltaVars
	 * @param model
	 * @throws GRBException
	 */
	private void addDeltaConstraints(int n, int m, GRBVar[][] deltaVars, GRBModel model) throws GRBException {
		// Constraints: sum_a(Delta_ia) <= 1, for all i
		for (int i = 0; i < n; i++) {
			String constraintName = "constraintEq8_" + i;
			GRBLinExpr constraintLinExpr = new GRBLinExpr();

			// sum_a(Delta_ia)
			for (int a = 0; a < m; a++) {
				// Exclude any Delta_ia term when action a is not applicable in state i
				if (mExplicitMDP.isActionApplicable(i, a)) {
					constraintLinExpr.addTerm(1.0, deltaVars[i][a]);
				}
			}

			// Add constraint: [...] <= 1
			model.addConstr(constraintLinExpr, GRB.LESS_EQUAL, 1, constraintName);
		}
	}

	/**
	 * Add constraints (from Eq. 9): x_ia / X <= Delta_ia, for all i, a.
	 * 
	 * @param n
	 * @param m
	 * @param xVars
	 * @param deltaVars
	 * @param model
	 * @throws GRBException
	 */
	private double addOMDeltaConstraints(int n, int m, GRBVar[][] xVars, GRBVar[][] deltaVars, GRBModel model)
			throws GRBException {
		// Constraints: x_ia / X <= Delta_ia, for all i, a
		double upperBoundOM = UpperBoundOccupationMeasureSolver.getUpperBoundOccupationMeasure(mExplicitMDP);
		for (int i = 0; i < n; i++) {
			for (int a = 0; a < m; a++) {
				// Exclude any x_ia and Delta_ia terms when action a is not applicable in state i
				if (mExplicitMDP.isActionApplicable(i, a)) {
					String constaintName = "constraintEq9_" + i + a;

					// x_ia / X
					GRBLinExpr lhsConstraintLinExpr = new GRBLinExpr();
					lhsConstraintLinExpr.addTerm(1.0 / upperBoundOM, xVars[i][a]);

					// Delta_ia
					GRBLinExpr rhsConstraintLinExpr = new GRBLinExpr();
					rhsConstraintLinExpr.addTerm(1.0, deltaVars[i][a]);

					// Add constraint
					model.addConstr(lhsConstraintLinExpr, GRB.LESS_EQUAL, rhsConstraintLinExpr, constaintName);
				}
			}
		}

		return upperBoundOM;
	}

	private boolean isTransitionCost() {
		return mExplicitMDP.getCostType() == CostType.TRANSITION_COST;
	}

	/**
	 * 
	 * @param xResults
	 * @param deltaResults
	 * @return Check, for all states i such that sum_a(x_ia) > 0, whether the property Delta_ia = 1 <=> x_ia > 0 holds
	 */
	private boolean consistencyCheckResults(double[][] xResults, double[][] deltaResults) {
		int n = mExplicitMDP.getNumStates();
		int m = mExplicitMDP.getNumActions();

		for (int i = 0; i < n; i++) {
			if (hasNonZeroProbVisited(i, xResults)) {
				// sum_a(x_ia) > 0

				for (int a = 0; a < m; a++) {
					// Exclude any x_ia and Delta_ia terms when action a is not applicable in state i
					if (mExplicitMDP.isActionApplicable(i, a)) {
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
	 * 
	 * @param i
	 * @param xResults
	 * @return Whether the state i has non-zero probability of being visited
	 */
	private boolean hasNonZeroProbVisited(int i, double[][] xResults) {
		int m = mExplicitMDP.getNumActions();
	
		// sum_a(x_ia)
		double probVisited = 0;
		for (int a = 0; a < m; a++) {
			if (mExplicitMDP.isActionApplicable(i, a)) {
				probVisited += xResults[i][a];
			}
		}
	
		// Check whether sum_a(x_ia) > 0
		return probVisited > 0;
	}

	private boolean checkResultsConsistency(double deltaResult, double xResult) {
		return (deltaResult == 1 && xResult > 0) || (deltaResult == 0 && xResult == 0);
	}

	private void consistencyCheckConstraints(int n, int m, double upperBoundOM, double[][] xResults,
			double[][] deltaResults) {
		assert GRBSolverUtils.consistencyCheckFlowConservationConstraints(xResults, mExplicitMDP);
		assert GRBSolverUtils.consistencyCheckSourceFlowConstraint(xResults, mExplicitMDP);
		assert GRBSolverUtils.consistencyCheckSinksFlowConstraint(xResults, mExplicitMDP);
		for (int k = 1; k < mUpperBoundConstraints.length; k++) {
			if (mUpperBoundConstraints[k] == null) {
				// Skip -- there is no constraint on this cost function k
				continue;
			}

			assert GRBSolverUtils.consistencyCheckCostConstraint(xResults, mExplicitMDP, k, mUpperBoundConstraints[k]);
		}
		assert consistencyCheckDeltaConstraints(n, m, deltaResults);
		assert consistencyCheckOMDeltaConstraints(n, m, xResults, deltaResults, upperBoundOM);
	}

	private boolean consistencyCheckConstraintsA(int n, int m, double[][] xResults) {
		// Initial state distribution
		double[] alpha = new double[n];
		int iniState = mExplicitMDP.getInitialState();
		alpha[iniState] = 1.0;

		double gamma = ExplicitMDP.DEFAULT_DISCOUNT_FACTOR;

		// Constraints: sum_a(x_ja) - gamma * sum_i,a(x_ia * p_iaj) = alpha_j, for all j
		for (int j = 0; j < n; j++) {
			double sum = 0;

			// sum_a(x_ja)
			for (int a = 0; a < m; a++) {
				// Exclude any x_ia term when action a is not applicable in state i
				if (mExplicitMDP.isActionApplicable(j, a)) {
					sum += xResults[j][a];
				}
			}

			// - gamma * sum_i,a(x_ia * p_iaj)
			double coeff = -1 * gamma;
			for (int i = 0; i < n; i++) {
				for (int a = 0; a < m; a++) {
					// Exclude any x_ia term when action a is not applicable in state i
					if (mExplicitMDP.isActionApplicable(i, a)) {
						double p = mExplicitMDP.getTransitionProbability(i, a, j);
						double termCoeff = coeff * p;

						// - gamma * p_iaj * x_ia
						sum += termCoeff * xResults[i][a];
					}
				}
			}

			// GRB FeasibilityTol
			if (Math.abs(sum - alpha[j]) > 1e-6) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Check whether the results of x_ia satisfy the constraints: sum_i,a(c^k_ia * x_ia) <= upper bound of c^k, for all
	 * k.
	 * 
	 * @param n
	 * @param m
	 * @param xResults
	 * @return Whether the results of x_ia satisfy: sum_i,a(c^k_ia * x_ia) <= upper bound of c^k, for all k
	 */
	private boolean consistencyCheckConstraintsB(int n, int m, double[][] xResults) {
		// Non-objective cost functions start at index 1 in ExplicitMDP
		for (int k = 1; k < mUpperBoundConstraints.length; k++) {
			if (mUpperBoundConstraints[k] == null) {
				// Skip -- there is no constraint on this cost function k
				continue;
			}

			double sum = 0;
			for (int i = 0; i < n; i++) {
				for (int a = 0; a < m; a++) {
					if (mExplicitMDP.isActionApplicable(i, a)) {
						// Transition k-cost: c^k_ia
						// OR
						// State k-cost: c^k_i
						double stepCost = isTransitionCost() ? mExplicitMDP.getTransitionCost(k, i, a)
								: mExplicitMDP.getStateCost(k, i);

						// c^k_ia * x_ia
						// OR
						// c^k_i * x_ia
						sum += stepCost * xResults[i][a];
					}
				}
			}

			if (sum > mUpperBoundConstraints[k]) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Check whether the results of Delta_ia satisfy the constraints: sum_a(Delta_ia) <= 1, for all i.
	 * 
	 * @param n
	 * @param m
	 * @param deltaResults
	 * @return Whether the results of Delta_ia satisfy: sum_a(Delta_ia) <= 1, for all i
	 */
	private boolean consistencyCheckDeltaConstraints(int n, int m, double[][] deltaResults) {
		for (int i = 0; i < n; i++) {
			double sum = 0;
			for (int a = 0; a < m; a++) {
				if (mExplicitMDP.isActionApplicable(i, a)) {
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
	 * @param n
	 * @param m
	 * @param xResults
	 * @param deltaResults
	 * @param upperBoundOM
	 * @return Whether the results of x_ia and Delta_ia satisfy: x_ia / X <= Delta_ia, for all i, a
	 */
	private boolean consistencyCheckOMDeltaConstraints(int n, int m, double[][] xResults, double[][] deltaResults,
			double upperBoundOM) {
		for (int i = 0; i < n; i++) {
			for (int a = 0; a < m; a++) {
				if (mExplicitMDP.isActionApplicable(i, a)) {
					double xResult = xResults[i][a];
					double deltaResult = deltaResults[i][a];
					if (xResult / upperBoundOM > deltaResult) {
						return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * 
	 * @param policy
	 * @return Check whether the policy is deterministic
	 */
	private boolean consistencyCheckDeterministicPolicy(double[][] policy) {
		for (int i = 0; i < policy.length; i++) {
			for (int a = 0; a < policy[0].length; a++) {
				// Exclude any pi_ia term when action a is not applicable in state i
				if (mExplicitMDP.isActionApplicable(i, a)) {
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
}
