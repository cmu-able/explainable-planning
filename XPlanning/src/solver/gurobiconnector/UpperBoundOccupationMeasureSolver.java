package solver.gurobiconnector;

import java.nio.channels.IllegalSelectorException;
import java.util.Set;

import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;
import solver.common.ExplicitMDP;

public class UpperBoundOccupationMeasureSolver {

	private UpperBoundOccupationMeasureSolver() {
		throw new IllegalSelectorException();
	}

	/**
	 * Solve for X >= x_ia for all i, a, where x_ia is the occupation measure corresponding to a policy of a given MDP.
	 * 
	 * @param explicitMDP
	 * @return Upper bound of occupation measure
	 * @throws GRBException
	 */
	public static double getUpperBoundOccupationMeasure(ExplicitMDP explicitMDP) throws GRBException {
		double[][] xResults = solveMaximumOccupationMeasure(explicitMDP);
		int n = explicitMDP.getNumStates();
		int m = explicitMDP.getNumActions();
		// From the constraint: x_ia >=0 for all i, a
		double maxX = 0;
		for (int i = 0; i < n; i++) {
			for (int a = 0; a < m; a++) {
				// Exclude any x_ia value when action a is not applicable in state i
				if (explicitMDP.isActionApplicable(i, a)) {
					maxX = Math.max(maxX, xResults[i][a]);
				}
			}
		}
		return maxX;
	}

	/**
	 * Solve: maximize_x sum_i,a (x_ia) subject to:
	 * 
	 * (C1) x_ia >= 0, for all i, a
	 * 
	 * (C2) out(i) - in(i) = 0, for all i in S \ (G and s0)
	 * 
	 * (C3) out(s0) - in(s0) = 1
	 * 
	 * (C4) sum_{sg in G} (in(sg)) = 1,
	 * 
	 * where:
	 * 
	 * in(i) = sum_j,a (x_ja * P(i|j,a)) and
	 * 
	 * out(i) = sum_a (x_ia).
	 * 
	 * @param explicitMDP
	 * @return Occupation measure
	 * @throws GRBException
	 */
	public static double[][] solveMaximumOccupationMeasure(ExplicitMDP explicitMDP) throws GRBException {
		GRBEnv env = new GRBEnv();
		GRBModel model = new GRBModel(env);

		int n = explicitMDP.getNumStates();
		int m = explicitMDP.getNumActions();

		// Variables: x_ia
		// Lower bound on variables: x_ia >= 0
		GRBVar[][] xVars = new GRBVar[n][m];
		for (int i = 0; i < n; i++) {
			for (int a = 0; a < m; a++) {
				// Add all variables x_ia to the model, but for action a that is not applicable in state i, the variable
				// x_ia will be excluded from the objective and constraints
				String xVarName = "x_" + i + a;
				xVars[i][a] = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, xVarName);
			}
		}

		setOptimizationObjective(xVars, model, explicitMDP);
		addConstraintsC2(xVars, model, explicitMDP);
		addConstraintC3(xVars, model, explicitMDP);
		addConstraintC4(xVars, model, explicitMDP);

		// Solve optimization problem for x_ia
		model.optimize();

		double[][] xResults = model.get(GRB.DoubleAttr.X, xVars);

		// Dispose of model and environment
		model.dispose();
		env.dispose();

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
	 * Add constraints C2: out(i) - in(i) = 0, for all i in S \ (G and s0).
	 * 
	 * @param xVars
	 * @param model
	 * @param explicitMDP
	 * @throws GRBException
	 */
	private static void addConstraintsC2(GRBVar[][] xVars, GRBModel model, ExplicitMDP explicitMDP)
			throws GRBException {
		int n = explicitMDP.getNumStates();
		Set<Integer> goals = explicitMDP.getGoalStates();
		int initialState = explicitMDP.getInitialState();

		for (int i = 0; i < n; i++) {
			if (goals.contains(i) || initialState == i) {
				// Exclude goal states G and initial state s0
				continue;
			}

			String constraintName = "constraintC2_" + i;
			// out(i) - in(i) = 0
			GRBLinExpr constraintLinExpr = new GRBLinExpr();

			// Expression += out(i)
			addOutTerm(i, 1, constraintLinExpr, xVars, explicitMDP);

			// Expression -= in(i)
			addInTerm(i, -1, constraintLinExpr, xVars, explicitMDP);

			// Add constraint
			model.addConstr(constraintLinExpr, GRB.EQUAL, 0, constraintName);
		}
	}

	/**
	 * Add a constraint C3: out(s0) - in(s0) = 1.
	 * 
	 * @param xVars
	 * @param model
	 * @param explicitMDP
	 * @throws GRBException
	 */
	private static void addConstraintC3(GRBVar[][] xVars, GRBModel model, ExplicitMDP explicitMDP) throws GRBException {
		int initialState = explicitMDP.getInitialState();
		String constraintName = "constraintC3";
		// out(s0) - in(s0) = 1
		GRBLinExpr constraintLinExpr = new GRBLinExpr();

		// Expression += out(s0)
		addOutTerm(initialState, 1, constraintLinExpr, xVars, explicitMDP);

		// Expression -= in(s0)
		addInTerm(initialState, -1, constraintLinExpr, xVars, explicitMDP);

		// Add constraint
		model.addConstr(constraintLinExpr, GRB.EQUAL, 1, constraintName);
	}

	/**
	 * Add a constraint C4: sum_{sg in G} (in(sg)) = 1.
	 * 
	 * @param xVars
	 * @param model
	 * @param explicitMDP
	 * @throws GRBException
	 */
	private static void addConstraintC4(GRBVar[][] xVars, GRBModel model, ExplicitMDP explicitMDP) throws GRBException {
		String constraintName = "constraintC4";
		// sum_{sg in G} (in(sg)) = 1
		GRBLinExpr constraintLinExpr = new GRBLinExpr();

		for (Integer goal : explicitMDP.getGoalStates()) {
			// Expression += in(sg)
			addInTerm(goal, 1, constraintLinExpr, xVars, explicitMDP);
		}

		// Add constraint
		model.addConstr(constraintLinExpr, GRB.EQUAL, 1, constraintName);
	}

	/**
	 * Add coeff * in(i) term to a given linear expression, where in(i) = sum_j,a (x_ja * P(i|j,a)).
	 * 
	 * @param i
	 * @param coeff
	 * @param linExpr
	 * @param xVars
	 * @param explicitMDP
	 */
	private static void addInTerm(int i, double coeff, GRBLinExpr linExpr, GRBVar[][] xVars, ExplicitMDP explicitMDP) {
		int n = explicitMDP.getNumStates();
		int m = explicitMDP.getNumActions();

		// in(i) = sum_j,a (x_ja * P(i|j,a))
		// Expression += coeff * in(i)
		for (int j = 0; j < n; j++) {
			for (int a = 0; a < m; a++) {
				// Exclude any x_ja term when action a is not applicable in state j
				if (explicitMDP.isActionApplicable(j, a)) {
					double prob = explicitMDP.getTransitionProbability(j, a, i);
					linExpr.addTerm(coeff * prob, xVars[j][a]);
				}
			}
		}
	}

	/**
	 * Add coeff * out(i) term to a given linear expression, where out(i) = sum_a (x_ia).
	 * 
	 * @param i
	 * @param coeff
	 * @param linExpr
	 * @param xVars
	 * @param explicitMDP
	 */
	private static void addOutTerm(int i, double coeff, GRBLinExpr linExpr, GRBVar[][] xVars, ExplicitMDP explicitMDP) {
		int m = explicitMDP.getNumActions();

		// out(i) = sum_a (x_ia)
		// Expression += coeff * out(i)
		for (int a = 0; a < m; a++) {
			// Exclude any x_ia term when action a is not applicable in state i
			if (explicitMDP.isActionApplicable(i, a)) {
				linExpr.addTerm(coeff, xVars[i][a]);
			}
		}
	}

	private static void addConstraints(int n, int m, GRBVar[][] xVars, GRBModel model, ExplicitMDP explicitMDP)
			throws GRBException {
		// Initial state distribution
		double[] alpha = new double[n];
		int iniState = explicitMDP.getInitialState();
		alpha[iniState] = 1.0;

		double gamma = ExplicitMDP.DEFAULT_DISCOUNT_FACTOR;

		// Constraints: sum_a(x_ja) - gamma * sum_i,a(x_ia * p_iaj) = alpha_j, for all j
		for (int j = 0; j < n; j++) {
			String constraintName = "constraint_" + j;
			GRBLinExpr constraintLinExpr = new GRBLinExpr();

			// sum_a(x_ja)
			for (int a = 0; a < m; a++) {
				// Exclude any x_ia term when action a is not applicable in state i
				if (explicitMDP.isActionApplicable(j, a)) {
					constraintLinExpr.addTerm(1.0, xVars[j][a]);
				}
			}

			// - gamma * sum_i,a(x_ia * p_iaj)
			double coeff = -1 * gamma;
			for (int i = 0; i < n; i++) {
				for (int a = 0; a < m; a++) {
					// Exclude any x_ia term when action a is not applicable in state i
					if (explicitMDP.isActionApplicable(i, a)) {
						double p = explicitMDP.getTransitionProbability(i, a, j);
						double termCoeff = coeff * p;

						// - gamma * p_iaj * x_ia
						constraintLinExpr.addTerm(termCoeff, xVars[i][a]);
					}
				}
			}

			// Add constraint
			model.addConstr(constraintLinExpr, GRB.EQUAL, alpha[j], constraintName);
		}
	}
}
