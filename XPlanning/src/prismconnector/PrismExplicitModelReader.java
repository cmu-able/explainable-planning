package prismconnector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

	private ValueEncodingScheme mEncodings;
	private String mModelPath;

	public PrismExplicitModelReader(ValueEncodingScheme encodings, String modelPath) {
		mEncodings = encodings;
		mModelPath = modelPath;
	}

	/**
	 * 
	 * @param filename
	 *            : Name of a states .sta file
	 * @return Mapping from integer values indexing states to the corresponding states
	 * @throws IOException
	 * @throws VarNotFoundException
	 */
	public Map<Integer, State> readStatesFile(String filename) throws IOException, VarNotFoundException {
		Map<Integer, State> indices = new HashMap<>();

		List<String> allLines = readLinesFromFile(filename);

		// Pattern: ({var1Name},{var2Name},...,{varNName})
		String header = allLines.get(0);
		String varNamesStr = header.substring(1, header.length() - 1);
		String[] varNames = varNamesStr.split(",");

		List<String> body = allLines.subList(1, allLines.size());

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

	/**
	 * 
	 * @param filename
	 *            : Name of an "adversary" .tra file
	 * @param stateIndices
	 *            : Mapping from integer values indexing states to the corresponding states
	 * @return A policy extracted from the "adversary" file
	 * @throws IOException
	 */
	public Policy readTransitionsFile(String filename, Map<Integer, State> stateIndices) throws IOException {
		Policy policy = new Policy();

		List<String> allLines = readLinesFromFile(filename);
		List<String> body = allLines.subList(1, allLines.size());

		// Pattern: *source* {destination} {index of action} {probability} *action name*
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
		return policy;
	}

	public List<String> readLinesFromFile(String filename) throws IOException {
		List<String> lines = new ArrayList<>();
		File file = new File(mModelPath, filename);

		try (FileReader fileReader = new FileReader(file);
				BufferedReader buffReader = new BufferedReader(fileReader);) {
			String line;
			while ((line = buffReader.readLine()) != null) {
				lines.add(line);
			}
		}
		return lines;
	}
}
