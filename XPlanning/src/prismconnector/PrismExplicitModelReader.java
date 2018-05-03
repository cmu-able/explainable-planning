package prismconnector;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import exceptions.VarNotFoundException;
import factors.IAction;
import factors.IStateVarValue;
import factors.StateVar;
import factors.StateVarDefinition;
import mdp.ActionSpace;
import mdp.State;
import mdp.StateSpace;
import policy.Policy;

public class PrismExplicitModelReader {

	private static final String VAR_NAME_PATTERN = "[A-Za-z_][A-Za-z0-9_]*";

	private ValueEncodingScheme mEncodings;

	public PrismExplicitModelReader(ValueEncodingScheme encodings) {
		mEncodings = encodings;
	}

	public Map<Integer, State> readStatesFile(String str) throws VarNotFoundException {
		Map<Integer, State> indices = new HashMap<>();

		String[] allLines = str.split("\n");

		// Pattern: ({var1Name},{var2Name},...,{varNName})
		String header = allLines[0];

		String varNamesStr = header.substring(1, header.length() - 1);

		String[] varNames = varNamesStr.split(",");

		String[] body = Arrays.copyOfRange(allLines, 1, allLines.length);

		// Pattern: {index}:({var1Value},{var2Value},...,{varNValue})
		for (String line : body) {
			State state = new State();

			String[] indexStateStr = line.split(":");
			String indexStr = indexStateStr[0];
			String valuesStr = indexStateStr[1].substring(1, indexStateStr[1].length() - 1);
			Integer index = Integer.parseInt(indexStr);
			String[] values = valuesStr.split(",");

			for (int i = 0; i < varNames.length; i++) {
				String varName = varNames[i];
				String valueStr = values[i];

				if (!isSpecialVariable(varName)) {
					Integer encodedIntValue = Integer.parseInt(valueStr);
					StateSpace stateSpace = mEncodings.getStateSpace();
					StateVarDefinition<IStateVarValue> definition = stateSpace.getStateVarDefinition(varName);
					IStateVarValue value = mEncodings.decodeStateVarValue(IStateVarValue.class, varName,
							encodedIntValue);
					StateVar<IStateVarValue> stateVar = new StateVar<>(definition, value);
					state.addStateVar(stateVar);
				}
			}

			indices.put(index, state);
		}
		return indices;
	}

	private boolean isSpecialVariable(String varName) {
		return varName.endsWith(PrismTranslatorUtilities.SRC_SUFFIX) || varName.equals("action")
				|| varName.equals("readyToCopy");
	}

	public Policy readTransitionsFile(String str, Map<Integer, State> stateIndices) {
		Policy policy = new Policy();

		String[] allLines = str.split("\n");
		String[] body = Arrays.copyOfRange(allLines, 1, allLines.length);

		// Pattern: {source} {destination} {index of action} {probability} {action name}
		for (String line : body) {
			String[] tokens = line.split(" ");
			String sourceStr = tokens[0];
			String actionName = tokens[4];
			Integer sourceIndex = Integer.parseInt(sourceStr);
			ActionSpace actionSpace = mEncodings.getActionSpace();

			State sourceState = stateIndices.get(sourceIndex);
			IAction action = actionSpace.getAction(actionName);
			policy.put(sourceState, action);
		}
		// TODO
		return policy;
	}
}
