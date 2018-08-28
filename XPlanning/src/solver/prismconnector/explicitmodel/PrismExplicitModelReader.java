package solver.prismconnector.explicitmodel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import language.exceptions.VarNotFoundException;
import language.mdp.ActionSpace;
import language.mdp.StateVarTuple;
import language.policy.Policy;
import language.qfactors.IAction;
import language.qfactors.IStateVarBoolean;
import language.qfactors.IStateVarInt;
import language.qfactors.IStateVarValue;
import language.qfactors.StateVar;
import language.qfactors.StateVarDefinition;
import solver.prismconnector.PrismTranslatorHelper;
import solver.prismconnector.PrismTranslatorUtils;
import solver.prismconnector.ValueEncodingScheme;

public class PrismExplicitModelReader {

	private static final Set<String> HELPER_VAR_NAMES = new HashSet<>(
			Arrays.asList("action", "readyToCopy", "barrier"));
	private static final Set<String> PRISM_VAR_NAMES = new HashSet<>(Arrays.asList("_da"));
	private static final Set<String> HELPER_ACTIONS = new HashSet<>(Arrays.asList("compute", "next", "end"));
	private static final Set<String> PRISM_ACTIONS = new HashSet<>(Arrays.asList("_ec"));
	private static final String INT_REGEX = "[0-9]+";
	private static final String BOOLEAN_REGEX = "(true|false)";

	private ValueEncodingScheme mEncodings;
	private PrismExplicitModelPointer mExplicitModelPtr;

	public PrismExplicitModelReader(ValueEncodingScheme encodings, PrismExplicitModelPointer explicitModelPtr) {
		mEncodings = encodings;
		mExplicitModelPtr = explicitModelPtr;
	}

	public ValueEncodingScheme getValueEncodingScheme() {
		return mEncodings;
	}

	public PrismExplicitModelPointer getPrismExplicitModelPointer() {
		return mExplicitModelPtr;
	}

	/**
	 * Read states from a PRISM product states file (prod.sta) if exists; otherwise, from .sta file.
	 * 
	 * @return Mapping from integer values indexing states to the corresponding states
	 * @throws IOException
	 * @throws VarNotFoundException
	 */
	public Map<Integer, StateVarTuple> readStatesFromFile() throws IOException, VarNotFoundException {
		File staFile = mExplicitModelPtr.productStatesFileExists() ? mExplicitModelPtr.getProductStatesFile()
				: mExplicitModelPtr.getStatesFile();

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

				if (isSpecialVariable(varName)) {
					// Skip -- this is a helper variable
					continue;
				}

				StateVarDefinition<IStateVarValue> varDef = mEncodings.getStateSpace().getStateVarDefinition(varName);
				StateVar<? extends IStateVarValue> stateVar;

				if (valueStr.matches(BOOLEAN_REGEX)) {
					IStateVarBoolean value = mEncodings.lookupStateVarBoolean(varName, Boolean.parseBoolean(valueStr));
					stateVar = varDef.getStateVar(value);
				} else if (valueStr.matches(INT_REGEX) && !mEncodings.hasEncodedIntValue(varDef)) {
					IStateVarInt value = mEncodings.lookupStateVarInt(varName, Integer.parseInt(valueStr));
					stateVar = varDef.getStateVar(value);
				} else {
					Integer encodedIntValue = Integer.parseInt(valueStr);
					IStateVarValue value = mEncodings.decodeStateVarValue(IStateVarValue.class, varName,
							encodedIntValue);
					stateVar = varDef.getStateVar(value);
				}

				state.addStateVar(stateVar);
			}

			indices.put(index, state);
		}
		return indices;
	}

	/**
	 * Read a policy from a PRISM adversary output file (adv.tra), given a index-state mapping.
	 * 
	 * @param stateIndices
	 *            : Mapping from integer values indexing states to the corresponding states
	 * @return A policy extracted from the "adversary" file
	 * @throws IOException
	 */
	public Policy readPolicyFromFile(Map<Integer, StateVarTuple> stateIndices) throws IOException {
		File advFile = mExplicitModelPtr.getAdversaryFile();
		Policy policy = new Policy();

		List<String> allLines = readLinesFromFile(advFile);
		List<String> body = allLines.subList(1, allLines.size());

		// Pattern: *source* {destination} {probability} *action name*
		for (String line : body) {
			String[] tokens = line.split(" ");
			String sourceStr = tokens[0];
			String sanitizedActionName = tokens[3];

			if (HELPER_ACTIONS.contains(sanitizedActionName) || PRISM_ACTIONS.contains(sanitizedActionName)) {
				// Skip -- this is a helper action
				continue;
			}

			String actionName = PrismTranslatorUtils.desanitizeNameString(sanitizedActionName);
			Integer sourceIndex = Integer.parseInt(sourceStr);
			ActionSpace actionSpace = mEncodings.getActionSpace();

			StateVarTuple sourceState = stateIndices.get(sourceIndex);
			IAction action = actionSpace.getAction(actionName);
			policy.put(sourceState, action);
		}
		return policy;
	}

	/**
	 * Read a policy from PRISM .sta and adv.tra files.
	 * 
	 * @return A policy extracted from the "adversary" file and the "states" file
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
		return varName.endsWith(PrismTranslatorHelper.SRC_SUFFIX) || HELPER_VAR_NAMES.contains(varName)
				|| PRISM_VAR_NAMES.contains(varName);
	}
}
