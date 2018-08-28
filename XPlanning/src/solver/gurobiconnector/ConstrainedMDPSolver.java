package solver.gurobiconnector;

import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBModel;

public class ConstrainedMDPSolver {

	public double[][] solveOccupationMeasure(ExplicitMDP explicitMDP) throws GRBException {
		GRBEnv env = new GRBEnv();
		GRBModel model = new GRBModel(env);

		int n = explicitMDP.getNumStates();
		int m = explicitMDP.getNumActions();

		// Initial state distribution
		double[] alpha = new double[n];
		int iniState = explicitMDP.getInitialState();
		alpha[iniState] = 1.0;

		// TODO
		return null;
	}
}
