package language.domain.models;

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

	private StateVarDefinition<E> mDefinition;
	private E mValue;

	/**
	 * This constructor is only called by {@link StateVarDefinition}.
	 * 
	 * @param definition
	 *            : State variable definition
	 * @param value
	 *            : State variable value
	 */
	StateVar(StateVarDefinition<E> definition, E value) {
		mDefinition = definition;
		mValue = value;
	}

	public StateVarDefinition<E> getDefinition() {
		return mDefinition;
	}

	public String getName() {
		return mDefinition.getName();
	}

	public E getValue() {
		return mValue;
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
		return var.mDefinition.equals(mDefinition) && var.mValue.equals(mValue);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mDefinition.hashCode();
			result = 31 * result + mValue.hashCode();
			hashCode = result;
		}
		return result;
	}

	@Override
	public String toString() {
		return getName() + "=" + getValue();
	}

}
