package solver.gurobiconnector;

import java.util.Set;

import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;
import solver.common.ExplicitMDP;

public class GRBSolverUtils {

	private GRBSolverUtils() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Create n x m matrix of optimization variables: x_ia, where x_ia >=0 for all i, a, and n and m are the numbers of
	 * states and actions, respectively.
	 * 
	 * @param explicitMDP
	 *            : Explicit MDP
	 * @param model
	 *            : GRB model to which to add the variables x_ia
	 * @return n x m matrix of optimization variables: x_ia, where x_ia >=0 for all i, a
	 * @throws GRBException
	 */
	public static GRBVar[][] createOccupationMeasureVars(ExplicitMDP explicitMDP, GRBModel model) throws GRBException {
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
		return xVars;
	}

	/**
	 * Add the flow-conservation constraints C2: out(i) - in(i) = 0, for all i in S \ (G and s0).
	 * 
	 * @param explicitMDP
	 *            : Explicit MDP
	 * @param xVars
	 *            : Occupation measure variables
	 * @param model
	 *            : GRB model to which to add the flow-conservation constraints
	 * @throws GRBException
	 */
	public static void addFlowConservationConstraints(ExplicitMDP explicitMDP, GRBVar[][] xVars, GRBModel model)
			throws GRBException {
		int n = explicitMDP.getNumStates();
		Set<Integer> goals = explicitMDP.getGoalStates();
		int iniState = explicitMDP.getInitialState();

		for (int i = 0; i < n; i++) {
			if (goals.contains(i) || iniState == i) {
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
	 * Add the source flow constraint C3: out(s0) - in(s0) = 1.
	 * 
	 * @param explicitMDP
	 *            : Explicit MDP
	 * @param xVars
	 *            : Occupation measure variables
	 * @param model
	 *            : GRB model to which to add the source flow constraint
	 * @throws GRBException
	 */
	public static void addSourceFlowConstraint(ExplicitMDP explicitMDP, GRBVar[][] xVars, GRBModel model)
			throws GRBException {
		int iniState = explicitMDP.getInitialState();

		String constraintName = "constraintC3";
		// out(s0) - in(s0) = 1
		GRBLinExpr constraintLinExpr = new GRBLinExpr();

		// Expression += out(s0)
		addOutTerm(iniState, 1, constraintLinExpr, xVars, explicitMDP);

		// Expression -= in(s0)
		addInTerm(iniState, -1, constraintLinExpr, xVars, explicitMDP);

		// Add constraint
		model.addConstr(constraintLinExpr, GRB.EQUAL, 1, constraintName);
	}

	/**
	 * Add the sinks flow constraint C4: sum_{sg in G} (in(sg)) = 1.
	 * 
	 * @param explicitMDP
	 *            : Explicit MDP
	 * @param xVars
	 *            : Occupation measure variables
	 * @param model
	 *            : GRB model to which to add the sinks flow constraint
	 * @throws GRBException
	 */
	public static void addSinksFlowConstraint(ExplicitMDP explicitMDP, GRBVar[][] xVars, GRBModel model)
			throws GRBException {
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
}
