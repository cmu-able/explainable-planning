package factors;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * {@link DerivedActionAttribute} holds a set of derived attribute values associated with an action.
 * Each value corresponds to applying the action to a particular state.
 * 
 * @author rsukkerd
 *
 */
public class DerivedActionAttribute {

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

}
