package solver.gurobiconnector;

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
import language.objectives.AdditiveCostFunction;
import language.objectives.AttributeConstraint;
import language.objectives.AttributeCostFunction;
import language.qfactors.IAction;
import solver.prismconnector.QFunctionEncodingScheme;

public class ConstrainedMDPSolver {

	private static final double TOLERANCE_FACTOR = 0.99;

	private ExplicitMDP mExplicitMDP;
	private double[] mUpperBoundConstraints;

	public ConstrainedMDPSolver(ExplicitMDP explicitMDP, AdditiveCostFunction objectiveFunction,
			Set<AttributeConstraint<IQFunction<?, ?>>> attrConstraints, QFunctionEncodingScheme qFunctionEncoding)
			throws QFunctionNotFoundException {
		mExplicitMDP = explicitMDP;
		setObjectiveFunctionOfExplicitMDP(objectiveFunction, qFunctionEncoding);
		mUpperBoundConstraints = getUpperBoundConstraints(attrConstraints, qFunctionEncoding);
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
	private void setObjectiveFunctionOfExplicitMDP(AdditiveCostFunction objectiveFunction,
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

	private double[] getUpperBoundConstraints(Set<AttributeConstraint<IQFunction<?, ?>>> attrConstraints,
			QFunctionEncodingScheme qFunctionEncoding) throws QFunctionNotFoundException {
		// Constraints are on the cost functions starting from index 1 in ExplicitMDP
		// Align the indices of the constraints to those of the cost functions in ExplicitMDP
		double[] constraints = new double[attrConstraints.size() + 1];

		for (AttributeConstraint<IQFunction<?, ?>> attrConstraint : attrConstraints) {
			IQFunction<?, ?> qFunction = attrConstraint.getQFunction();
			double upperBound = attrConstraint.getExpectedTotalUpperBound();
			int costFuncIndex = qFunctionEncoding.getRewardStructureIndex(qFunction);

			if (attrConstraint.isStrictBound()) {
				constraints[costFuncIndex] = TOLERANCE_FACTOR * upperBound;
			} else {
				constraints[costFuncIndex] = upperBound;
			}
		}

		return constraints;
	}

	public double[][] solveOptimalPolicy() throws GRBException {
		int n = mExplicitMDP.getNumStates();
		int m = mExplicitMDP.getNumActions();
		double[][] policy = new double[n][m];
		double[][] occupationMeasure = solveOccupationMeasure();

		for (int i = 0; i < n; i++) {
			// sum_a(x_ia)
			int denom = 0;
			for (int a = 0; a < m; a++) {
				denom += occupationMeasure[i][a];
			}

			assert denom > 0;

			for (int a = 0; a < m; a++) {
				// pi_ia = x_ia / sum_a(x_ia)
				policy[i][a] = occupationMeasure[i][a] / denom;
			}
		}

		assert sanityCheckDeterministicPolicy(policy);
		return policy;
	}

	public double[][] solveOccupationMeasure() throws GRBException {
		GRBEnv env = new GRBEnv();
		GRBModel model = new GRBModel(env);

		int n = mExplicitMDP.getNumStates();
		int m = mExplicitMDP.getNumActions();

		// Variables: x_ia
		// Lower bound on variables: x_ia >= 0
		GRBVar[][] xVars = new GRBVar[n][m];
		for (int i = 0; i < n; i++) {
			for (int a = 0; a < m; a++) {
				String xVarName = "x_" + i + a;
				xVars[i][a] = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, xVarName);
			}
		}

		// Variables: Delta_ia (binary)
		GRBVar[][] deltaVars = new GRBVar[n][m];
		for (int i = 0; i < n; i++) {
			for (int a = 0; a < m; a++) {
				String deltaVarName = "Delta_" + i + a;
				deltaVars[i][a] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, deltaVarName);
			}
		}

		// Set optimization objective
		setOptimizationObjective(n, m, xVars, model);

		// Add constraints
		addConstraintsA(n, m, xVars, model);
		addConstraintsB(n, m, xVars, model);
		addConstraintsC(n, m, deltaVars, model);
		addConstraintsD(n, m, xVars, deltaVars, model);

		// Solve optimization problem for x_ia and Delta_ia
		model.optimize();

		// Query results: optimal values of x_ia and Delta_ia
		double[][] xResults = model.get(GRB.DoubleAttr.X, xVars);
		double[][] deltaResults = model.get(GRB.DoubleAttr.X, deltaVars);

		// Dispose of model and environment
		model.dispose();
		env.dispose();

		assert sanityCheckResults(xResults, deltaResults);

		return xResults;
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
				// Objective cost: c_ia
				// OR
				// c_i
				double objectiveCost = isTransitionCost()
						? mExplicitMDP.getTransitionCost(ExplicitMDP.OBJECTIVE_FUNCTION_INDEX, i, a)
						: mExplicitMDP.getStateCost(ExplicitMDP.OBJECTIVE_FUNCTION_INDEX, i);
				objectiveLinExpr.addTerm(objectiveCost, xVars[i][a]);
			}
		}

		// Set objective
		model.setObjective(objectiveLinExpr, GRB.MAXIMIZE);
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
				constraintLinExpr.addTerm(1.0, xVars[j][a]);
			}

			// - gamma * sum_i,a(x_ia * p_iaj)
			double coeff = -1 * gamma;
			for (int i = 0; i < n; i++) {
				for (int a = 0; a < m; a++) {
					double p = mExplicitMDP.getTransitionProbability(i, a, j);
					double termCoeff = coeff * p;

					// - gamma * p_iaj * x_ia
					constraintLinExpr.addTerm(termCoeff, xVars[i][a]);
				}
			}

			// Add constraint: [...] = alpha_j
			model.addConstr(constraintLinExpr, GRB.EQUAL, alpha[j], constraintName);
		}
	}

	/**
	 * Add constraints.
	 * 
	 * For transition costs: sum_i,a(c^k_ia * x_ia) <= upper bound of c^k, for all k.
	 * 
	 * For state costs: sum_i,a(c^k_i * x_ia) <= upper bound of c^k, for all k.
	 * 
	 * @param n
	 * @param m
	 * @param xVars
	 * @param model
	 * @throws GRBException
	 */
	private void addConstraintsB(int n, int m, GRBVar[][] xVars, GRBModel model) throws GRBException {
		// Constraints: sum_i,a(c^k_ia * x_ia) <= upper bound of c^k, for all k
		// OR
		// sum_i,a(c^k_i * x_ia) <= upper bound of c^k, for all k

		// Non-objective cost functions start at index 1 in ExplicitMDP
		for (int k = 1; k < mUpperBoundConstraints.length; k++) {
			String constraintName = "constraintB_" + k;
			GRBLinExpr constraintLinExpr = new GRBLinExpr();

			for (int i = 0; i < n; i++) {
				for (int a = 0; a < m; a++) {
					// Transition k-cost: c^k_ia
					// OR
					// State k-cost: c^k_i
					double stepCost = isTransitionCost() ? mExplicitMDP.getTransitionCost(k, i, a)
							: mExplicitMDP.getStateCost(k, i);

					// c^k_ia * x_ia
					// OR
					// c^k_i * x_ia
					constraintLinExpr.addTerm(stepCost, xVars[i][a]);
				}
			}

			// Add constraint: [...] <= upper bound of c^k
			model.addConstr(constraintLinExpr, GRB.LESS_EQUAL, mUpperBoundConstraints[k], constraintName);
		}
	}

	/**
	 * Add constraints: sum_a(Delta_ia) <= 1, for all i.
	 * 
	 * @param n
	 * @param m
	 * @param deltaVars
	 * @param model
	 * @throws GRBException
	 */
	private void addConstraintsC(int n, int m, GRBVar[][] deltaVars, GRBModel model) throws GRBException {
		// Constraints: sum_a(Delta_ia) <= 1, for all i
		for (int i = 0; i < n; i++) {
			String constraintName = "constraintC_" + i;
			GRBLinExpr constraintLinExpr = new GRBLinExpr();

			// sum_a(Delta_ia)
			for (int a = 0; a < m; a++) {
				constraintLinExpr.addTerm(1.0, deltaVars[i][a]);
			}

			// Add constraint: [...] <= 1
			model.addConstr(constraintLinExpr, GRB.LESS_EQUAL, 1, constraintName);
		}
	}

	/**
	 * Add constraints: x_ia / X <= Delta_ia, for all i, a.
	 * 
	 * @param n
	 * @param m
	 * @param xVars
	 * @param deltaVars
	 * @param model
	 * @throws GRBException
	 */
	private void addConstraintsD(int n, int m, GRBVar[][] xVars, GRBVar[][] deltaVars, GRBModel model)
			throws GRBException {
		// Constraints: x_ia / X <= Delta_ia, for all i, a
		double upperBoundOM = UpperBoundOccupationMeasureSolver.getUpperBoundOccupationMeasure(mExplicitMDP);
		for (int i = 0; i < n; i++) {
			for (int a = 0; a < m; a++) {
				String constaintName = "constraintC_" + i + a;

				// x_ia / X
				GRBLinExpr lhsConstraintLinExpr = new GRBLinExpr();
				lhsConstraintLinExpr.addTerm(1 / upperBoundOM, xVars[i][a]);

				// Delta_ia
				GRBLinExpr rhsConstraintLinExpr = new GRBLinExpr();
				rhsConstraintLinExpr.addTerm(1.0, deltaVars[i][a]);

				// Add constraint
				model.addConstr(lhsConstraintLinExpr, GRB.LESS_EQUAL, rhsConstraintLinExpr, constaintName);
			}
		}
	}

	/**
	 * 
	 * @param xResults
	 * @param deltaResults
	 * @return Check whether the property Delta_ia = 1 <=> x_ia > 0 holds
	 */
	private boolean sanityCheckResults(double[][] xResults, double[][] deltaResults) {
		int n = mExplicitMDP.getNumStates();
		int m = mExplicitMDP.getNumActions();

		for (int i = 0; i < n; i++) {
			for (int a = 0; a < m; a++) {
				boolean property = (deltaResults[i][a] == 1 && xResults[i][a] > 0)
						|| (deltaResults[i][a] == 0 && xResults[i][a] == 0);

				if (!property) {
					return false;
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
	private boolean sanityCheckDeterministicPolicy(double[][] policy) {
		for (int i = 0; i < policy.length; i++) {
			for (int a = 0; a < policy[0].length; a++) {
				// Check for any randomized decision
				if (policy[i][a] > 0 && policy[i][a] < 1) {
					return false;
				}
			}
		}
		return true;
	}

	private boolean isTransitionCost() {
		return mExplicitMDP.getCostType() == CostType.TRANSITION_COST;
	}
}
