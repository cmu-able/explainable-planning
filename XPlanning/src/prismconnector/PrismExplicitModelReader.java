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
import mdp.StateVarTuple;
import mdp.StateSpace;
import policy.Policy;

public class PrismExplicitModelReader {

	private ValueEncodingScheme mEncodings;
	private PrismExplicitModelPointer mExplicitModelPtr;

	public PrismExplicitModelReader(ValueEncodingScheme encodings, PrismExplicitModelPointer explicitModelPtr) {
		mEncodings = encodings;
		mExplicitModelPtr = explicitModelPtr;
	}

	/**
	 * 
	 * @return Mapping from integer values indexing states to the corresponding states
	 * @throws IOException
	 * @throws VarNotFoundException
	 */
	public Map<Integer, StateVarTuple> readStatesFromFile() throws IOException, VarNotFoundException {
		File staFile = mExplicitModelPtr.getStatesFile();
		Map<Integer, StateVarTuple> indices = new HashMap<>();

		List<String> allLines = readLinesFromFile(staFile);

		// Pattern: ({var1Name},{var2Name},...,{varNName})
		String header = allLines.get(0);
		String varNamesStr = header.substring(1, header.length() - 1);
		String[] varNames = varNamesStr.split(",");

		List<String> body = allLines.subList(1, allLines.size());

		// Pattern: {index}:({var1Value},{var2Value},...,{varNValue})
		for (String line : body) {
			StateVarTuple state = new StateVarTuple();

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
					StateVar<IStateVarValue> stateVar = definition.getStateVar(value);
					state.addStateVar(stateVar);
				}
			}

			indices.put(index, state);
		}
		return indices;
	}

	/**
	 * 
	 * @param stateIndices
	 *            : Mapping from integer values indexing states to the corresponding states
	 * @return A policy extracted from the "adversary" file
	 * @throws IOException
	 */
	public Policy readPolicyFromFile(Map<Integer, StateVarTuple> stateIndices) throws IOException {
		File traFile = mExplicitModelPtr.getTransitionsFile();
		Policy policy = new Policy();

		List<String> allLines = readLinesFromFile(traFile);
		List<String> body = allLines.subList(1, allLines.size());

		// Pattern: *source* {destination} {index of action} {probability} *action name*
		for (String line : body) {
			String[] tokens = line.split(" ");
			String sourceStr = tokens[0];
			String actionName = tokens[4];
			Integer sourceIndex = Integer.parseInt(sourceStr);
			ActionSpace actionSpace = mEncodings.getActionSpace();

			StateVarTuple sourceState = stateIndices.get(sourceIndex);
			IAction action = actionSpace.getAction(actionName);
			policy.put(sourceState, action);
		}
		return policy;
	}

	/**
	 * 
	 * @return A policy extracted from the "adversary" file
	 * @throws IOException
	 * @throws VarNotFoundException
	 */
	public Policy readPolicyFromFiles() throws IOException, VarNotFoundException {
		Map<Integer, StateVarTuple> stateIndices = readStatesFromFile();
		return readPolicyFromFile(stateIndices);
	}

	private List<String> readLinesFromFile(File file) throws IOException {
		List<String> lines = new ArrayList<>();

		try (FileReader fileReader = new FileReader(file);
				BufferedReader buffReader = new BufferedReader(fileReader);) {
			String line;
			while ((line = buffReader.readLine()) != null) {
				lines.add(line);
			}
		}
		return lines;
	}

	private boolean isSpecialVariable(String varName) {
		return varName.endsWith(PrismTranslatorUtilities.SRC_SUFFIX) || varName.equals("action")
				|| varName.equals("readyToCopy");
	}
}
