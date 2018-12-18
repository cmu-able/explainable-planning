package solver.gurobiconnector;

import java.util.Arrays;
import java.util.Set;

import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;
import language.domain.metrics.IQFunction;
import language.exceptions.QFunctionNotFoundException;
import language.objectives.AttributeConstraint;
import language.objectives.IPenaltyFunction;
import solver.common.CostType;
import solver.common.ExplicitMDP;
import solver.prismconnector.QFunctionEncodingScheme;

public class CostConstraintUtils {

	/**
	 * For approximating strict inequality for hard upper bounds.
	 */
	private static final double TOLERANCE_FACTOR = 0.99;

	private CostConstraintUtils() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Fill in the arrays hardUpperBounds according to attrSoftConstraints.
	 * 
	 * The indices of the upper-bounds are aligned with those of the cost functions in {@link ExplicitMDP}. An
	 * upper-bound is null iff the corresponding cost function doesn't have a constraint.
	 * 
	 * @param attrHardConstraints
	 *            : Upper-bound constraints on QA values
	 * @param qFunctionEncoding
	 *            : QA-function encoding scheme
	 * @return Array of upper-bounds on QAs
	 * @throws QFunctionNotFoundException
	 */
	public static void createHardUpperBounds(Set<AttributeConstraint<IQFunction<?, ?>>> attrHardConstraints,
			QFunctionEncodingScheme qFunctionEncoding, Double[] hardUpperBounds) throws QFunctionNotFoundException {
		IPenaltyFunction[] dummyPenaltyFunctions = new IPenaltyFunction[hardUpperBounds.length];
		createUpperBounds(attrHardConstraints, qFunctionEncoding, hardUpperBounds, dummyPenaltyFunctions);
	}

	/**
	 * Fill in the arrays softUpperBounds and penaltyFunctions according to attrSoftConstraints.
	 * 
	 * The indices of the upper-bounds are aligned with those of the cost functions in {@link ExplicitMDP}. An
	 * upper-bound is null iff the corresponding cost function doesn't have a constraint.
	 * 
	 * @param attrSoftConstraints
	 *            : Upper-bound soft constraints on QA values
	 * @param qFunctionEncoding
	 *            : QA-function encoding scheme
	 * @param softUpperBounds
	 *            : Return parameter; array size must be numRewardStructures + 1
	 * @param penaltyFunctions
	 *            : Return parameter; array size must be numRewardStructures + 1
	 * @throws QFunctionNotFoundException
	 */
	public static void createSoftUpperBounds(Set<AttributeConstraint<IQFunction<?, ?>>> attrSoftConstraints,
			QFunctionEncodingScheme qFunctionEncoding, Double[] softUpperBounds, IPenaltyFunction[] penaltyFunctions)
			throws QFunctionNotFoundException {
		createUpperBounds(attrSoftConstraints, qFunctionEncoding, softUpperBounds, penaltyFunctions);
	}

	/**
	 * Fill in the arrays upperBounds and penaltyFunctions.
	 * 
	 * Constraints are on the cost functions starting from index 1 in ExplicitMDP. Make sure to align the indices of the
	 * constraints to those of the cost functions in ExplicitMDP.
	 * 
	 * @param attrConstraints
	 *            : Upper-bound (hard or soft) constraints on QA values
	 * @param qFunctionEncoding
	 *            : QA-function encoding scheme
	 * @param upperBounds
	 *            : Return parameter; array size must be numRewardStructures + 1
	 * @param penaltyFunctions
	 *            : Return parameter; array size must be numRewardStructures + 1
	 * @throws QFunctionNotFoundException
	 */
	private static void createUpperBounds(Set<AttributeConstraint<IQFunction<?, ?>>> attrConstraints,
			QFunctionEncodingScheme qFunctionEncoding, Double[] upperBounds, IPenaltyFunction[] penaltyFunctions)
			throws QFunctionNotFoundException {
		// Set upper bounds to null for all cost-function indices that don't have constraints
		Arrays.fill(upperBounds, null);

		// Set penalty functions to null for all cost-function indices that don't have soft constraints
		Arrays.fill(penaltyFunctions, null);

		for (AttributeConstraint<IQFunction<?, ?>> attrConstraint : attrConstraints) {
			IQFunction<?, ?> qFunction = attrConstraint.getQFunction();
			double upperBound = attrConstraint.getUpperBound();
			int costFuncIndex = qFunctionEncoding.getRewardStructureIndex(qFunction);

			// Constraints are on the cost functions starting from index 1 in ExplicitMDP
			// Make sure to align the indices of the constraints to those of the cost functions in ExplicitMDP

			if (attrConstraint.isStrictBound()) {
				upperBounds[costFuncIndex] = TOLERANCE_FACTOR * upperBound;
			} else {
				upperBounds[costFuncIndex] = upperBound;
			}

			if (attrConstraint.isSoftConstraint()) {
				penaltyFunctions[costFuncIndex] = attrConstraint.getPenaltyFunction();
			}
		}
	}

	/**
	 * Add hard constraints on multiple cost functions.
	 * 
	 * For transition costs: sum_i,a(c^k_ia * x_ia) <= HUP_k, for all k.
	 * 
	 * For state costs: sum_i,a(c^k_i * x_ia) <= HUP_k, for all k.
	 * 
	 * @param upperBounds
	 *            : Hard upper bounds on the expected total (or average) costs
	 * @param explicitMDP
	 *            : ExplicitMDP
	 * @param xVars
	 *            : Occupation measure variables: x_ia
	 * @param model
	 *            : GRB model to which to add the constraints
	 * @throws GRBException
	 */
	public static void addHardCostConstraints(Double[] upperBounds, ExplicitMDP explicitMDP, GRBVar[][] xVars,
			GRBModel model) throws GRBException {
		// Constraints: sum_i,a(c^k_ia * x_ia) <= HUP_k, for all k
		// OR
		// sum_i,a(c^k_i * x_ia) <= HUB_k, for all k

		// Non-objective cost functions start at index 1 in ExplicitMDP
		for (int k = 1; k < upperBounds.length; k++) {
			if (upperBounds[k] == null) {
				// Skip -- there is no constraint on this cost function k
				continue;
			}

			addHardCostConstraint(k, upperBounds[k], explicitMDP, xVars, model);
		}
	}

	/**
	 * Add a hard constraint on the k-th cost: sum_i,a (x_ia * C_k(i,a)) <= HUP_k.
	 * 
	 * @param costFuncIndex
	 *            : Cost function index k
	 * @param upperBound
	 *            : Hard upper bound on the expected total (or average) k-th cost
	 * @param explicitMDP
	 *            : Explicit MDP
	 * @param xVars
	 *            : Occupation measure variables
	 * @param model
	 *            : GRB model to which to add the constraint
	 * @throws GRBException
	 */
	public static void addHardCostConstraint(int costFuncIndex, double upperBound, ExplicitMDP explicitMDP,
			GRBVar[][] xVars, GRBModel model) throws GRBException {
		addCostConstraint(costFuncIndex, upperBound, explicitMDP, xVars, null, model);
	}

	/**
	 * Add soft constraints on multiple cost functions.
	 * 
	 * @param softUpperBounds
	 *            : Soft upper bounds on the expected total (or average) costs
	 * @param hardUpperBounds
	 *            : Hard upper bounds on the expected total (or average) costs
	 * @param penaltyFunctions
	 *            : Penalty functions
	 * @param explicitMDP
	 *            : Explicit MDP
	 * @param xVars
	 *            : Occupation measure variables
	 * @param model
	 *            : GRB model to which to add the constraint
	 * @throws GRBException
	 */
	public static void addSoftCostConstraints(Double[] softUpperBounds, Double[] hardUpperBounds,
			IPenaltyFunction[] penaltyFunctions, ExplicitMDP explicitMDP, GRBVar[][] xVars, GRBModel model)
			throws GRBException {
		for (int k = 0; k < softUpperBounds.length; k++) {
			if (softUpperBounds[k] == null) {
				// Skip -- there is no soft constraint on the k-th cost function
			}

			addSoftCostConstraint(k, softUpperBounds[k], hardUpperBounds[k], penaltyFunctions[k], explicitMDP, xVars,
					model);
		}
	}

	/**
	 * Add a soft constraint on the k-th cost: sum_i,a (x_ia * C_k(i,a)) - v <= SUB_k, where 0 <= v <= HUP_k - SUP_k;
	 * and add a penalty term: k_p * penalty(v), to the objective.
	 * 
	 * @param costFuncIndex
	 *            : Cost function index k
	 * @param softUpperBound
	 *            : Soft upper bound on the expected total (or average) k-th cost
	 * @param hardUpperBound
	 *            : Hard upper bound on the expected total (or average) k-th cost; this is the value of the original
	 *            solution policy
	 * @param penaltyFunction
	 *            : Penalty function
	 * @param explicitMDP
	 *            : Explicit MDP
	 * @param xVars
	 *            : Occupation measure variables: x_ia
	 * @param model
	 *            : GRB model to which to add the constraint
	 * @throws GRBException
	 */
	public static void addSoftCostConstraint(int costFuncIndex, double softUpperBound, double hardUpperBound,
			IPenaltyFunction penaltyFunction, ExplicitMDP explicitMDP, GRBVar[][] xVars, GRBModel model)
			throws GRBException {
		double vMax = hardUpperBound - softUpperBound;
		GRBVar vVar = model.addVar(0.0, vMax, 0.0, GRB.CONTINUOUS, "v_" + costFuncIndex);
		addCostConstraint(costFuncIndex, softUpperBound, explicitMDP, xVars, vVar, model);

		if (penaltyFunction.isNonLinear()) {
			int m = penaltyFunction.getNumSamples();

			// Align the array indices to the sample indices (samples are labeled 1 to m)
			// 0-th index is not used in the following arrays

			// m samples of violation amounts
			Double[] vSamples = new Double[m + 1];

			// m samples of penalties
			Double[] penaltySamples = new Double[m + 1];

			// alpha_i variables, i = 1...m
			// alpha_0 is not used
			GRBVar[] alphaVars = GRBSolverUtils.createOptimizationVars("alpha_" + costFuncIndex, GRB.CONTINUOUS, m + 1,
					0.0, 1.0, model);

			// h_i variables, i = 0...m
			// h_0 and h_m are treated as constant 0
			GRBVar[] hVars = GRBSolverUtils.createOptimizationVars("h_" + costFuncIndex, GRB.BINARY, m + 1, 0.0, 1.0,
					model);

			// Violation and penalty samples
			for (int i = 1; i <= m; i++) {
				double stepSize = vMax / (m - 1);
				double vSample = stepSize * (i - 1);
				vSamples[i] = vSample;
				penaltySamples[i] = penaltyFunction.getPenalty(vSample);
			}

			// Add non-linear penalty term to the objective
			addNonlinearPenaltyTerm(m, penaltySamples, penaltyFunction.getScalingConst(), alphaVars, model);

			// Add constraints for approximating non-linear penalty function
			addNonlinearPenaltyConstraints(costFuncIndex, m, vSamples, hVars, alphaVars, vVar, model);
		} else {
			// Add linear penalty term to the objective
			addLinearPenaltyTerm(penaltyFunction.getScalingConst(), vVar, model);
		}
	}

	/**
	 * Add a constraint on the k-th cost:
	 * 
	 * (a) for hard constraint: sum_i,a (x_ia * C_k(i,a)) <= HUB_k, and
	 * 
	 * (b) for soft constraint: sum_i,a (x_ia * C_k(i,a)) - v <= SUB_k.
	 * 
	 * @param costFuncIndex
	 *            : Cost function index k
	 * @param upperBound
	 *            : Upper bound on the expected total (or average) k-th cost
	 * @param explicitMDP
	 *            : Explicit MDP
	 * @param xVars
	 *            : Occupation measure variables: x_ia
	 * @param vVar
	 *            : Violation variable: v (null for hard constraint)
	 * @param model
	 *            : GRB model to which to add the constraint
	 * @throws GRBException
	 */
	private static void addCostConstraint(int costFuncIndex, double upperBound, ExplicitMDP explicitMDP,
			GRBVar[][] xVars, GRBVar vVar, GRBModel model) throws GRBException {
		int n = explicitMDP.getNumStates();
		int m = explicitMDP.getNumActions();

		// Expression: sum_i,a (x_ia * C_k(i,a))
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

		if (vVar != null) {
			// For soft constraint: sum_i,a (x_ia * C_k(i,a)) - v
			constraintLinExpr.addTerm(-1, vVar);
		}

		String constraintName = "constraintC_" + costFuncIndex + (vVar == null ? "_hard" : "_soft");

		// Add constraint: [...] <= UB_k
		model.addConstr(constraintLinExpr, GRB.LESS_EQUAL, upperBound, constraintName);
	}

	/**
	 * Add a linear penalty term for a SINGLE soft constraint: k_p * v, to the objective function of the model.
	 * 
	 * @param scalingConst
	 *            : Scaling constant k_p of the penalty term
	 * @param vVar
	 *            : Violation variable
	 * @param model
	 *            : GRB model to add the penalty term to its objective function
	 * @throws GRBException
	 */
	private static void addLinearPenaltyTerm(double scalingConst, GRBVar vVar, GRBModel model) throws GRBException {
		// Primary objective is at index 0
		model.getObjective(0).addTerm(scalingConst, vVar);
	}

	/**
	 * Add a non-linear penalty term for a SINGLE soft constraint: k_p * sum_{i=1 to m} alpha_i * penalty_i, to the
	 * objective function. This is a piecewise linear approximation of a non-linear penalty function.
	 * 
	 * @param numSamples
	 *            : Number of sampled (v, penalty) points
	 * @param penaltySamples
	 *            : [ penalty_0 (not used), penalty_1, ..., penalty_m ]
	 * @param scalingConst
	 *            : Scaling constant k_p of the penalty term
	 * @param alphaVars
	 *            : [ alpha_0 (not used), alpha_1, ..., alpha_m ]
	 * @param model
	 *            : GRB model to add the penalty term to its objective function
	 * @throws GRBException
	 */
	private static void addNonlinearPenaltyTerm(int numSamples, Double[] penaltySamples, double scalingConst,
			GRBVar[] alphaVars, GRBModel model) throws GRBException {
		for (int i = 1; i <= numSamples; i++) {
			// Primary objective is at index 0
			model.getObjective(0).addTerm(penaltySamples[i] * scalingConst, alphaVars[i]);
		}
	}

	/**
	 * Add the following constraints for piecewise linear approximation of a non-linear penalty function for a SINGLE
	 * soft constraint:
	 * 
	 * (1) sum_{i=1 to m-1} h_i = 1
	 * 
	 * (2) alpha_i <= h_{i-1} + h_i, for i = 1,...,m
	 * 
	 * (3) sum_{i=1 to m} alpha_i = 1
	 * 
	 * (4) v = sum_{i=1 to m} alpha_i * v_i
	 * 
	 * @param costFuncIndex
	 *            : Index of the associated cost function; for constraint naming
	 * @param numSamples
	 *            : Number of sampled (v, penalty) points
	 * @param vSamples
	 *            : Violation amount samples
	 * @param hVars
	 *            : Binary variables: [h_0 (not used), h_1, ..., h_{m-1}, h_m (not used)], where h_0 = h_m = 0
	 * @param alphaVars
	 *            : Continuous variables: [ alpha_0 (not used), alpha_1, ..., alpha_m ]
	 * @param vVar
	 *            : Violation variable
	 * @param model
	 *            : GRB model to which to add the constraints
	 * @throws GRBException
	 */
	private static void addNonlinearPenaltyConstraints(int costFuncIndex, int numSamples, Double[] vSamples,
			GRBVar[] hVars, GRBVar[] alphaVars, GRBVar vVar, GRBModel model) throws GRBException {
		addHConstraint(costFuncIndex, numSamples, hVars, model);
		addAlphaHConstraints(costFuncIndex, numSamples, hVars, alphaVars, model);
		addAlphaConstraint(costFuncIndex, numSamples, alphaVars, model);
		addAlphaVConstraint(costFuncIndex, numSamples, vSamples, alphaVars, vVar, model);
	}

	/**
	 * Add constraint: sum_{i=1 to m-1} h_i = 1.
	 * 
	 * Only one h_i takes the value 1.
	 * 
	 * @param costFuncIndex
	 *            : Index of the associated cost function; for constraint naming
	 * @param numSamples
	 * @param hVars
	 * @param model
	 * @throws GRBException
	 */
	private static void addHConstraint(int costFuncIndex, int numSamples, GRBVar[] hVars, GRBModel model)
			throws GRBException {
		// Constraint: sum_{i=1 to m-1} h_i = 1

		// Expression: sum_{i=1 to m-1} h_i
		GRBLinExpr hConstraintLinExpr = new GRBLinExpr();
		for (int i = 1; i <= numSamples - 1; i++) {
			hConstraintLinExpr.addTerm(1, hVars[i]);
		}

		// Add constraint: sum_{i=1 to m-1} h_i = 1
		model.addConstr(hConstraintLinExpr, GRB.EQUAL, 1, "constraint_h_" + costFuncIndex);
	}

	/**
	 * Add constraint: alpha_i <= h_{i-1} + h_i, for i = 1,...,m.
	 * 
	 * Only one pair of alpha_i and alpha_i+1, associated with h_i, can be non-zero.
	 * 
	 * @param costFuncIndex
	 *            : Index of the associated cost function; for constraint naming
	 * @param numSamples
	 * @param hVars
	 * @param alphaVars
	 * @param model
	 * @throws GRBException
	 */
	private static void addAlphaHConstraints(int costFuncIndex, int numSamples, GRBVar[] hVars, GRBVar[] alphaVars,
			GRBModel model) throws GRBException {
		// Constraints: alpha_i <= h_{i-1} + h_i, for i = 1,...,m

		for (int i = 1; i <= numSamples; i++) {
			String constraintName = "constraint_alphah_" + costFuncIndex + "_" + i;

			// LHS expression: alpha_i
			GRBLinExpr alphaLinExpr = new GRBLinExpr();
			alphaLinExpr.addTerm(1, alphaVars[i]);

			// RHS expression: h_{i-1} + h_i
			GRBLinExpr hLinExpr = new GRBLinExpr();

			if (i > 1) {
				hLinExpr.addTerm(1, hVars[i - 1]);
			} // Dummy variable: h_0 = 0

			if (i < numSamples) {
				hLinExpr.addTerm(1, hVars[i]);
			} // Dummy variable: h_m = 0

			// Add constraint: alpha_i <= h_{i-1} + h_i
			model.addConstr(alphaLinExpr, GRB.LESS_EQUAL, hLinExpr, constraintName);
		}
	}

	/**
	 * Add constraint: sum_{i=1 to m} alpha_i = 1.
	 * 
	 * The non-zero pair of alpha_i and alpha_i+1 are the coefficients of convex combination of v_i and v_i+1.
	 * 
	 * @param costFuncIndex
	 *            : Index of the associated cost function; for constraint naming
	 * @param numSamples
	 * @param alphaVars
	 * @param model
	 * @throws GRBException
	 */
	private static void addAlphaConstraint(int costFuncIndex, int numSamples, GRBVar[] alphaVars, GRBModel model)
			throws GRBException {
		// Constraint: sum_{i=1 to m} alpha_i = 1

		// Expression: sum_{i=1 to m} alpha_i
		GRBLinExpr alphaConstraintLinExpr = new GRBLinExpr();
		for (int i = 1; i <= numSamples; i++) {
			alphaConstraintLinExpr.addTerm(1, alphaVars[i]);
		}

		// Add constraint: sum_{i=1 to m} alpha_i = 1
		model.addConstr(alphaConstraintLinExpr, GRB.EQUAL, 1, "constraint_alpha_" + costFuncIndex);
	}

	/**
	 * Add constraint: v = sum_{i=1 to m} alpha_i * v_i.
	 * 
	 * The non-zero pair of alpha_i and alpha_i+1 are the coefficients of convex combination of v_i and v_i+1.
	 * 
	 * @param costFuncIndex
	 *            : Index of the associated cost function; for constraint naming
	 * @param numSamples
	 * @param vSamples
	 * @param alphaVars
	 * @param vVar
	 * @param model
	 * @throws GRBException
	 */
	private static void addAlphaVConstraint(int costFuncIndex, int numSamples, Double[] vSamples, GRBVar[] alphaVars,
			GRBVar vVar, GRBModel model) throws GRBException {
		// Constraint: v = sum_{i=1 to m} alpha_i * v_i

		// LHS expression: v
		GRBLinExpr vLinExpr = new GRBLinExpr();
		vLinExpr.addTerm(1, vVar);

		// RHS expression: sum_{i=1 to m} alpha_i * v_i
		GRBLinExpr alphavLinExpr = new GRBLinExpr();
		for (int i = 1; i <= numSamples; i++) {
			alphavLinExpr.addTerm(vSamples[i], alphaVars[i]);
		}

		// Add constraint: v = sum_{i=1 to m} alpha_i * v_i
		model.addConstr(vLinExpr, GRB.EQUAL, alphavLinExpr, "constraint_alphav_" + costFuncIndex);
	}
}
