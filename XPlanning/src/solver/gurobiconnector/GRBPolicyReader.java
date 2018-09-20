package solver.gurobiconnector;

import java.io.IOException;
import java.util.Map;

import language.exceptions.VarNotFoundException;
import language.mdp.StateVarTuple;
import language.policy.Policy;
import language.qfactors.IAction;
import solver.common.ExplicitMDP;
import solver.prismconnector.PrismTranslatorUtils;
import solver.prismconnector.explicitmodel.PrismExplicitModelReader;

public class GRBPolicyReader {

	private PrismExplicitModelReader mPrismExplicitModelReader;

	public GRBPolicyReader(PrismExplicitModelReader prismExplicitModelReader) {
		mPrismExplicitModelReader = prismExplicitModelReader;
	}

	public Policy readPolicyFromExplicitPolicy(double[][] explicitPolicy, ExplicitMDP explicitMDP)
			throws VarNotFoundException, IOException {
		if (explicitPolicy.length == 0) {
			// No solution found
			return null;
		}

		Map<Integer, StateVarTuple> stateIndices = mPrismExplicitModelReader.readStatesFromFile();

		Policy policy = new Policy();

		for (int i = 0; i < explicitPolicy.length; i++) {
			for (int a = 0; a < explicitPolicy[i].length; a++) {
				String sanitizedActionName = explicitMDP.getActionNameAtIndex(a);

				if (explicitPolicy[i][a] > 0 && !PrismExplicitModelReader.isAuxiliaryAction(sanitizedActionName)) {
					// Probability of taking action a in state i is non-zero
					// Skip any helper action

					String actionName = PrismTranslatorUtils.desanitizeNameString(sanitizedActionName);

					StateVarTuple sourceState = stateIndices.get(i);
					IAction action = mPrismExplicitModelReader.getActionSpace().getAction(actionName);
					policy.put(sourceState, action);

					// Move on to the next state
					break;
				}
			}
		}
		return policy;
	}
}
