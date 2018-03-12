package factors;

import java.util.Set;

/**
 * {@link StateVar} represents a generic state variable, whose value type is a subtype of {@link IStateVarValue}.
 * 
 * @author rsukkerd
 *
 */
public class StateVar<E extends IStateVarValue> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private String mName;
	private E mValue;
	private Set<E> mPossibleValues;

	public StateVar(String name, E value, Set<E> possibleValues) {
		mName = name;
		mValue = value;
		mPossibleValues = possibleValues;
	}

	public String getName() {
		return mName;
	}

	public E getValue() {
		return mValue;
	}

	public Set<E> getPossibleValues() {
		return mPossibleValues;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof StateVar<?>)) {
			return false;
		}
		StateVar<?> var = (StateVar<?>) obj;
		return var.mName.equals(mName) && var.mValue.equals(mValue) && var.mPossibleValues.equals(mPossibleValues);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mName.hashCode();
			result = 31 * result + mValue.hashCode();
			result = 31 * result + mPossibleValues.hashCode();
			hashCode = result;
		}
		return result;
	}
}
