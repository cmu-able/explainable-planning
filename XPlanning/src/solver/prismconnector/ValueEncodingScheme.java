package solver.prismconnector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import language.exceptions.ActionNotFoundException;
import language.exceptions.QFunctionNotFoundException;
import language.exceptions.VarNotFoundException;
import language.mdp.ActionSpace;
import language.mdp.StateSpace;
import language.metrics.IQFunction;
import language.objectives.IAdditiveCostFunction;
import language.qfactors.ActionDefinition;
import language.qfactors.IAction;
import language.qfactors.IStateVarBoolean;
import language.qfactors.IStateVarInt;
import language.qfactors.IStateVarValue;
import language.qfactors.StateVarDefinition;

/**
 * {@link ValueEncodingScheme} is an encoding scheme for representing the values of each state variable as PRISM's
 * supported types. In the case of 3-parameter reward function: R(s,a,s'), this is also an encoding scheme for actions.
 * 
 * @author rsukkerd
 *
 */
public class ValueEncodingScheme {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Map<StateVarDefinition<IStateVarValue>, Map<IStateVarValue, Integer>> mStateVarEncodings = new HashMap<>();
	private Map<String, Map<Boolean, ? extends IStateVarBoolean>> mBooleanVarLookups = new HashMap<>();
	private Map<String, Map<Integer, ? extends IStateVarInt>> mIntVarLookups = new HashMap<>();
	private Map<IAction, Integer> mActionEncoding = new HashMap<>();
	private QFunctionEncodingScheme mQFunctionEncoding = new QFunctionEncodingScheme();
	private StateSpace mStateSpace;
	private ActionSpace mActionSpace;
	private boolean mThreeParamRewards;

	public ValueEncodingScheme(StateSpace stateSpace) {
		mStateSpace = stateSpace;
		mThreeParamRewards = false;
		encodeStates(stateSpace);
	}

	public ValueEncodingScheme(StateSpace stateSpace, ActionSpace actionSpace) {
		mStateSpace = stateSpace;
		mActionSpace = actionSpace;
		mThreeParamRewards = true;
		encodeStates(stateSpace);
		encodeActions(actionSpace);
	}

	private void encodeStates(StateSpace stateSpace) {
		for (StateVarDefinition<IStateVarValue> stateVarDef : stateSpace) {
			IStateVarValue sampleValue = stateVarDef.getPossibleValues().iterator().next();

			if (sampleValue instanceof IStateVarBoolean) {
				// Boolean->IStateVarBoolean lookup table for recovering boolean variables
				Set<IStateVarBoolean> possibleValues = downcastSet(stateVarDef.getPossibleValues());
				Map<Boolean, IStateVarBoolean> booleanVarLookup = buildBooleanVarLookup(possibleValues);
				mBooleanVarLookups.put(stateVarDef.getName(), booleanVarLookup);
			} else if (sampleValue instanceof IStateVarInt) {
				// Integer->IStateVarInt lookup table for recovering int variables
				Set<IStateVarInt> possibleValues = downcastSet(stateVarDef.getPossibleValues());
				Map<Integer, IStateVarInt> intVarLookup = buildIntVarLookup(possibleValues);
				mIntVarLookups.put(stateVarDef.getName(), intVarLookup);
			} else {
				// Build int-encoding for variable types NOT supported by PRISM language
				Map<IStateVarValue, Integer> encoding = buildIntEncoding(stateVarDef.getPossibleValues());
				mStateVarEncodings.put(stateVarDef, encoding);
			}
		}
	}

	private <T, E extends T> Set<E> downcastSet(Set<T> originalSet) {
		Set<E> resultSet = new HashSet<>();
		for (T value : originalSet) {
			resultSet.add((E) value);
		}
		return resultSet;
	}

	private void encodeActions(ActionSpace actionSpace) {
		Set<IAction> allActions = new HashSet<>();
		for (ActionDefinition<IAction> actionDef : actionSpace) {
			allActions.addAll(actionDef.getActions());
		}
		mActionEncoding.putAll(buildIntEncoding(allActions));
	}

	private <E> Map<E, Integer> buildIntEncoding(Set<E> possibleValues) {
		Map<E, Integer> encoding = new HashMap<>();
		int e = 0;
		for (E value : possibleValues) {
			encoding.put(value, e);
			e++;
		}
		return encoding;
	}

	private <E extends IStateVarBoolean> Map<Boolean, E> buildBooleanVarLookup(Set<E> possibleValues) {
		Map<Boolean, E> mapping = new HashMap<>();
		for (E value : possibleValues) {
			mapping.put(value.getValue(), value);
		}
		return mapping;
	}

	private <E extends IStateVarInt> Map<Integer, E> buildIntVarLookup(Set<E> possibleValues) {
		Map<Integer, E> mapping = new HashMap<>();
		for (E value : possibleValues) {
			mapping.put(value.getValue(), value);
		}
		return mapping;
	}

	void appendCostFunction(IAdditiveCostFunction objectiveFunction) {
		mQFunctionEncoding.appendCostFunction(objectiveFunction);
	}

	void appendQFunction(IQFunction<?, ?> qFunction) {
		mQFunctionEncoding.appendQFunction(qFunction);
	}

	public boolean isThreeParamRewards() {
		return mThreeParamRewards;
	}

	public StateSpace getStateSpace() {
		return mStateSpace;
	}

	public ActionSpace getActionSpace() {
		return mActionSpace;
	}

	public IStateVarBoolean lookupStateVarBoolean(String stateVarName, Boolean boolValue) {
		return mBooleanVarLookups.get(stateVarName).get(boolValue);
	}

	public IStateVarInt lookupStateVarInt(String stateVarName, Integer intValue) {
		return mIntVarLookups.get(stateVarName).get(intValue);
	}

	public boolean hasEncodedIntValue(StateVarDefinition<? extends IStateVarValue> stateVarDef) {
		return mStateVarEncodings.containsKey(stateVarDef);
	}

	public <E extends IStateVarValue> Integer getEncodedIntValue(StateVarDefinition<E> stateVarDef, E value)
			throws VarNotFoundException {
		if (!mStateVarEncodings.containsKey(stateVarDef)) {
			throw new VarNotFoundException(stateVarDef);
		}
		return mStateVarEncodings.get(stateVarDef).get(value);
	}

	public Integer getEncodedIntValue(IAction action) throws ActionNotFoundException {
		if (!mActionEncoding.containsKey(action)) {
			throw new ActionNotFoundException(action);
		}
		return mActionEncoding.get(action);
	}

	public Integer getMaximumEncodedIntValue(StateVarDefinition<? extends IStateVarValue> stateVarDef)
			throws VarNotFoundException {
		if (!mStateVarEncodings.containsKey(stateVarDef)) {
			throw new VarNotFoundException(stateVarDef);
		}
		Map<IStateVarValue, Integer> encoding = mStateVarEncodings.get(stateVarDef);
		return encoding.size() - 1;
	}

	public Integer getMaximumEncodedIntAction() {
		return mActionEncoding.size() - 1;
	}

	public <E extends IStateVarValue> E decodeStateVarValue(Class<E> valueType, String stateVarName,
			Integer encodedIntValue) throws VarNotFoundException {
		for (Entry<StateVarDefinition<IStateVarValue>, Map<IStateVarValue, Integer>> entry : mStateVarEncodings
				.entrySet()) {
			StateVarDefinition<IStateVarValue> stateVarDef = entry.getKey();
			Map<IStateVarValue, Integer> encoding = entry.getValue();

			if (stateVarDef.getName().equals(stateVarName)) {
				for (Entry<IStateVarValue, Integer> e : encoding.entrySet()) {
					IStateVarValue value = e.getKey();
					Integer encodedValue = e.getValue();

					if (encodedValue.equals(encodedIntValue)) {
						return valueType.cast(value);
					}
				}
			}
		}
		throw new VarNotFoundException(stateVarName);
	}

	public int getRewardStructureIndex(IQFunction<?, ?> qFunction) throws QFunctionNotFoundException {
		return mQFunctionEncoding.getRewardStructureIndex(qFunction);
	}

	public int getRewardStructureIndex(IAdditiveCostFunction objectiveFunction) {
		return mQFunctionEncoding.getRewardStructureIndex(objectiveFunction);
	}

	public QFunctionEncodingScheme getQFunctionEncodingScheme() {
		return mQFunctionEncoding;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof ValueEncodingScheme)) {
			return false;
		}
		ValueEncodingScheme scheme = (ValueEncodingScheme) obj;
		return scheme.mStateVarEncodings.equals(mStateVarEncodings)
				&& scheme.mBooleanVarLookups.equals(mBooleanVarLookups) && scheme.mIntVarLookups.equals(mIntVarLookups)
				&& scheme.mActionEncoding.equals(mActionEncoding)
				&& scheme.mQFunctionEncoding.equals(mQFunctionEncoding) && scheme.mStateSpace.equals(mStateSpace)
				&& scheme.mActionSpace.equals(mActionSpace) && scheme.mThreeParamRewards == mThreeParamRewards;
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mStateVarEncodings.hashCode();
			result = 31 * result + mBooleanVarLookups.hashCode();
			result = 31 * result + mIntVarLookups.hashCode();
			result = 31 * result + mActionEncoding.hashCode();
			result = 31 * result + mQFunctionEncoding.hashCode();
			result = 31 * result + mStateSpace.hashCode();
			result = 31 * result + mActionSpace.hashCode();
			result = 31 * result + Boolean.hashCode(mThreeParamRewards);
			hashCode = result;
		}
		return hashCode;
	}

}
