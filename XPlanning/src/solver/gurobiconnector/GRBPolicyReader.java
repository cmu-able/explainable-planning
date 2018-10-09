package solver.gurobiconnector;

import java.io.IOException;
import java.util.Map;

import language.domain.models.IAction;
import language.exceptions.VarNotFoundException;
import language.mdp.StateVarTuple;
import language.policy.Policy;
import solver.common.ExplicitMDP;
import solver.prismconnector.PrismTranslatorUtils;
import solver.prismconnector.explicitmodel.PrismExplicitModelReader;

public class GRBPolicyReader {

	private PrismExplicitModelReader mPrismExplicitModelReader;

	public GRBPolicyReader(PrismExplicitModelReader prismExplicitModelReader) {
		mPrismExplicitModelReader = prismExplicitModelReader;
	}

	public Policy readPolicyFromPolicyMatrix(double[][] policyMatrix, ExplicitMDP explicitMDP)
			throws VarNotFoundException, IOException {
		Map<Integer, StateVarTuple> stateIndices = mPrismExplicitModelReader.readStatesFromFile();

		Policy policy = new Policy();

		for (int i = 0; i < policyMatrix.length; i++) {
			for (int a = 0; a < policyMatrix[i].length; a++) {
				String sanitizedActionName = explicitMDP.getActionNameAtIndex(a);

				if (policyMatrix[i][a] > 0 && !PrismExplicitModelReader.isAuxiliaryAction(sanitizedActionName)) {
					// Probability of taking action a in state i is non-zero
					// Skip any helper action

					String actionName = PrismTranslatorUtils.desanitizeNameString(sanitizedActionName);

					StateVarTuple sourceState = stateIndices.get(i);
					IAction action = mPrismExplicitModelReader.getValueEncodingScheme().getActionSpace()
							.getAction(actionName);
					policy.put(sourceState, action);

					// Move on to the next state
					break;
				}
			}
		}
		return policy;
	}
}
