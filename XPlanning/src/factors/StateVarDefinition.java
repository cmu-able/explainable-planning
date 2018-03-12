package factors;

import java.util.Set;

/**
 * {@link StateVarDefinition} defines a set of possible values of a specific state variable.
 * 
 * @author rsukkerd
 *
 * @param <E>
 */
public class StateVarDefinition<E extends IStateVarValue> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private String mName;
	private Set<E> mPossibleValues;

	public StateVarDefinition(String name, Set<E> possibleValues) {
		mName = name;
		mPossibleValues = possibleValues;
	}

	public String getName() {
		return mName;
	}

	public Set<E> getPossibleValues() {
		return mPossibleValues;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof StateVarDefinition<?>)) {
			return false;
		}
		StateVarDefinition<?> varDef = (StateVarDefinition<?>) obj;
		return varDef.mName.equals(mName) && varDef.mPossibleValues.equals(mPossibleValues);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mName.hashCode();
			result = 31 * result + mPossibleValues.hashCode();
			hashCode = result;
		}
		return result;
	}
}
