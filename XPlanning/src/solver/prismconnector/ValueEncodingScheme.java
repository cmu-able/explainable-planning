package solver.prismconnector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import language.domain.metrics.IQFunction;
import language.domain.models.IStateVarBoolean;
import language.domain.models.IStateVarInt;
import language.domain.models.IStateVarValue;
import language.domain.models.StateVarDefinition;
import language.exceptions.QFunctionNotFoundException;
import language.exceptions.VarNotFoundException;
import language.mdp.ActionSpace;
import language.mdp.QSpace;
import language.mdp.StateSpace;
import language.objectives.CostFunction;
import language.objectives.IAdditiveCostFunction;

/**
 * {@link ValueEncodingScheme} is an encoding scheme for: representing the values of each state variable as PRISM's
 * supported types, and indexing the QA functions according to the order of their corresponding reward structures in the
 * PRISM model.
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
	private QFunctionEncodingScheme mQFunctionEncoding;
	private StateSpace mStateSpace;
	private ActionSpace mActionSpace;

	public ValueEncodingScheme(StateSpace stateSpace, ActionSpace actionSpace, QSpace qSpace,
			CostFunction costFunction) {
		mStateSpace = stateSpace;
		mActionSpace = actionSpace;
		mQFunctionEncoding = new QFunctionEncodingScheme(costFunction, qSpace);
		encodeStates(stateSpace);
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

	public Integer getMaximumEncodedIntValue(StateVarDefinition<? extends IStateVarValue> stateVarDef)
			throws VarNotFoundException {
		if (!mStateVarEncodings.containsKey(stateVarDef)) {
			throw new VarNotFoundException(stateVarDef);
		}
		Map<IStateVarValue, Integer> encoding = mStateVarEncodings.get(stateVarDef);
		return encoding.size() - 1;
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

	public int getNumRewardStructures() {
		return mQFunctionEncoding.getNumRewardStructures();
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
				&& scheme.mQFunctionEncoding.equals(mQFunctionEncoding) && scheme.mStateSpace.equals(mStateSpace)
				&& scheme.mActionSpace.equals(mActionSpace);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mStateVarEncodings.hashCode();
			result = 31 * result + mBooleanVarLookups.hashCode();
			result = 31 * result + mIntVarLookups.hashCode();
			result = 31 * result + mQFunctionEncoding.hashCode();
			result = 31 * result + mStateSpace.hashCode();
			result = 31 * result + mActionSpace.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
