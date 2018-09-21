package solver.gurobiconnector;

import java.nio.channels.IllegalSelectorException;

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
		double upperBoundX = 0;
		for (int i = 0; i < n; i++) {
			for (int a = 0; a < m; a++) {
				// Exclude any x_ia value when action a is not applicable in state i
				if (explicitMDP.isActionApplicable(i, a)) {
					upperBoundX += xResults[i][a];
				}
			}
		}
		return upperBoundX;
	}

	/**
	 * Solve: maximize_x sum_i,a (x_ia) subject to:
	 * 
	 * (C1) x_ia >= 0, for all i, a
	 * 
	 * (C2) out(i) - gamma * in(i) = alpha_i, for all i in S
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

		// Create variables: x_ia
		// Lower bound on variables: x_ia >= 0
		GRBVar[][] xVars = GRBSolverUtils.createOccupationMeasureVars(explicitMDP, model);

		// Set optimization objective
		setOptimizationObjective(xVars, model, explicitMDP);

		// Add constraints
		GRBSolverUtils.addDiscountedFlowConservationConstraints(explicitMDP, xVars, model);

		// Solve optimization problem for x_ia
		model.optimize();

		double[][] xResults = model.get(GRB.DoubleAttr.X, xVars);

		// Dispose of model and environment
		model.dispose();
		env.dispose();

		if (!GRBSolverUtils.consistencyCheckDiscountedFlowConservationConstraints(xResults, explicitMDP)) {
			throw new IllegalStateException("Discounted flow-conservation constraints violated");
		}

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
}
