package factors;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * {@link DerivedActionAttribute} holds a set of derived attribute values associated with an action. Each value corresponds to applying the action to a particular state.
 * 
 * @author rsukkerd
 *
 */
public class DerivedActionAttribute {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private String mName;
	private Map<Set<IStateVarValue>, IActionAttribute> mValues;

	public DerivedActionAttribute(String name) {
		mName = name;
	}

	public void putDerivedAttributeValue(IActionAttribute value, IStateVarValue... srcStateVars) {
		Set<IStateVarValue> varSet = new HashSet<>();
		for (IStateVarValue var : srcStateVars) {
			varSet.add(var);
		}
		mValues.put(varSet, value);
	}

	public String getAttributeName() {
		return mName;
	}

	public IActionAttribute getDerivedAttributeValue(IStateVarValue... srcStateVars) {
		Set<IStateVarValue> varSet = new HashSet<>();
		for (IStateVarValue var : srcStateVars) {
			varSet.add(var);
		}
		return mValues.get(varSet);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof DerivedActionAttribute)) {
			return false;
		}
		DerivedActionAttribute attribute = (DerivedActionAttribute) obj;
		return attribute.mName.equals(mName) && attribute.mValues.equals(mValues);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mName.hashCode();
			result = 31 * result + mValues.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
