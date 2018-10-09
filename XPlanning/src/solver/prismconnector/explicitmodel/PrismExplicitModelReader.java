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

import language.domain.models.IAction;
import language.domain.models.IStateVarBoolean;
import language.domain.models.IStateVarInt;
import language.domain.models.IStateVarValue;
import language.domain.models.StateVar;
import language.domain.models.StateVarDefinition;
import language.exceptions.VarNotFoundException;
import language.mdp.StateVarTuple;
import language.policy.Policy;
import solver.prismconnector.PrismTranslatorUtils;
import solver.prismconnector.ValueEncodingScheme;

public class PrismExplicitModelReader {

	private static final Set<String> HELPER_VAR_NAMES = new HashSet<>(Arrays.asList("barrier", "computeGo"));
	private static final Set<String> PRISM_VAR_NAMES = new HashSet<>(Arrays.asList("_da"));
	private static final Set<String> HELPER_ACTIONS = new HashSet<>(Arrays.asList("compute", "next", "end"));
	private static final Set<String> PRISM_ACTIONS = new HashSet<>(Arrays.asList("_ec"));
	private static final String INT_REGEX = "[0-9]+";
	private static final String BOOLEAN_REGEX = "(true|false)";

	private PrismExplicitModelPointer mExplicitModelPtr;
	private ValueEncodingScheme mEncodings;

	public PrismExplicitModelReader(PrismExplicitModelPointer prismExplicitModelPtr, ValueEncodingScheme encodings) {
		mEncodings = encodings;
		mExplicitModelPtr = prismExplicitModelPtr;
	}

	public PrismExplicitModelPointer getPrismExplicitModelPointer() {
		return mExplicitModelPtr;
	}

	public ValueEncodingScheme getValueEncodingScheme() {
		return mEncodings;
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

				if (isAuxiliaryVariable(varName)) {
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

			if (isAuxiliaryAction(sanitizedActionName)) {
				// Skip -- this is a helper action
				continue;
			}

			String actionName = PrismTranslatorUtils.desanitizeNameString(sanitizedActionName);
			Integer sourceIndex = Integer.parseInt(sourceStr);

			StateVarTuple sourceState = stateIndices.get(sourceIndex);
			IAction action = mEncodings.getActionSpace().getAction(actionName);
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

	public static boolean isAuxiliaryVariable(String varName) {
		return HELPER_VAR_NAMES.contains(varName) || PRISM_VAR_NAMES.contains(varName);
	}

	public static boolean isAuxiliaryAction(String actionName) {
		return HELPER_ACTIONS.contains(actionName) || PRISM_ACTIONS.contains(actionName);
	}
}
