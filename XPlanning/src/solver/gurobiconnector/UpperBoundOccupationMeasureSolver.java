package solver.gurobiconnector;

import java.nio.channels.IllegalSelectorException;

import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;

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
	 * Solve: maximize sum_i,a(x_ia) subject to: (1) x_ia >= 0 for all i, a, and (2) sum_a(x_ja) - gamma * sum_i,a(x_ia
	 * * p_iaj) = alpha_j for all j.
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

		setOptimizationObjective(n, m, xVars, model, explicitMDP);

		addConstraints(n, m, xVars, model, explicitMDP);

		// Solve optimization problem for x_ia
		model.optimize();

		double[][] xResults = model.get(GRB.DoubleAttr.X, xVars);

		// Dispose of model and environment
		model.dispose();
		env.dispose();

		return xResults;
	}

	private static void setOptimizationObjective(int n, int m, GRBVar[][] xVars, GRBModel model,
			ExplicitMDP explicitMDP) throws GRBException {
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
