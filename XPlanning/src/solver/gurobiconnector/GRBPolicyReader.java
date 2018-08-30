package solver.gurobiconnector;

import java.io.IOException;
import java.util.Map;

import language.exceptions.VarNotFoundException;
import language.mdp.ActionSpace;
import language.mdp.StateVarTuple;
import language.policy.Policy;
import language.qfactors.IAction;
import solver.prismconnector.PrismTranslatorUtils;
import solver.prismconnector.ValueEncodingScheme;
import solver.prismconnector.explicitmodel.PrismExplicitModelPointer;
import solver.prismconnector.explicitmodel.PrismExplicitModelReader;

public class GRBPolicyReader {

	private PrismExplicitModelReader mPrismExplicitModelReader;

	public GRBPolicyReader(PrismExplicitModelPointer prismExplicitModelPtr, ValueEncodingScheme encodings) {
		mPrismExplicitModelReader = new PrismExplicitModelReader(prismExplicitModelPtr, encodings);
	}

	public Policy readPolicyFromExplicitPolicy(double[][] explicitPolicy, ExplicitMDP explicitMDP)
			throws VarNotFoundException, IOException {
		Map<Integer, StateVarTuple> stateIndices = mPrismExplicitModelReader.readStatesFromFile();
		ActionSpace actionSpace = mPrismExplicitModelReader.getValueEncodingScheme().getActionSpace();

		Policy policy = new Policy();

		for (int i = 0; i < explicitPolicy.length; i++) {
			for (int a = 0; a < explicitPolicy[i].length; a++) {
				String sanitizedActionName = explicitMDP.getActionNameAtIndex(a);

				if (explicitPolicy[i][a] > 0 && !PrismExplicitModelReader.isAuxiliaryAction(sanitizedActionName)) {
					// Probability of taking action a in state i is non-zero
					// Skip any helper action

					String actionName = PrismTranslatorUtils.desanitizeNameString(sanitizedActionName);

					StateVarTuple sourceState = stateIndices.get(i);
					IAction action = actionSpace.getAction(actionName);
					policy.put(sourceState, action);

					// Move on to the next state
					break;
				}
			}
		}
		return policy;
	}
}
