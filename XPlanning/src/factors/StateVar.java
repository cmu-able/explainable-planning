package factors;

/**
 * {@link StateVar} represents a state variable with a specific value.
 * 
 * @author rsukkerd
 *
 */
public class StateVar {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private String mName;
	private IStateVarValue mValue;

	public StateVar(String name, IStateVarValue value) {
		mName = name;
		mValue = value;
	}

	public String getName() {
		return mName;
	}

	public IStateVarValue getValue() {
		return mValue;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof StateVar)) {
			return false;
		}
		StateVar var = (StateVar) obj;
		return var.mName.equals(mName) && var.mValue.equals(mValue);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mName.hashCode();
			result = 31 * result + mValue.hashCode();
			hashCode = result;
		}
		return result;
	}
}
