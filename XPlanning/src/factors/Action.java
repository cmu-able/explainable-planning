package factors;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import exceptions.AttributeNameNotFoundException;

/**
 * {@link Action} defines a type of actions.
 * 
 * @author rsukkerd
 *
 */
public class Action implements IAction {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private String mActionName;
	private Map<String, StateVar<? extends IStateVarValue>> mParameters;
	private Map<String, IActionAttribute> mAttributes;
	private Map<String, DerivedActionAttribute> mDerivedAttributes;

	public Action(String actionName) {
		mActionName = actionName;
		mParameters = new HashMap<>();
		mAttributes = new HashMap<>();
		mDerivedAttributes = new HashMap<>();
	}

	public void addParameter(StateVar<? extends IStateVarValue> parameter) {
		mParameters.put(parameter.getName(), parameter);
	}

	public void putAttributeValue(String name, IActionAttribute value) {
		mAttributes.put(name, value);
	}

	public void putDerivedAttributeValue(String name, IActionAttribute value,
			Set<StateVar<? extends IStateVarValue>> srcStateVars) {
		if (!mDerivedAttributes.containsKey(name)) {
			DerivedActionAttribute derivedAttr = new DerivedActionAttribute(name);
			derivedAttr.putDerivedAttributeValue(value, srcStateVars);
			mDerivedAttributes.put(name, derivedAttr);
		} else {
			mDerivedAttributes.get(name).putDerivedAttributeValue(value, srcStateVars);
		}
	}

	@Override
	public IActionAttribute getAttributeValue(String name) throws AttributeNameNotFoundException {
		if (!mAttributes.containsKey(name)) {
			throw new AttributeNameNotFoundException(name);
		}
		return mAttributes.get(name);
	}

	@Override
	public IActionAttribute getDerivedAttributeValue(String name, Set<StateVar<? extends IStateVarValue>> srcStateVars)
			throws AttributeNameNotFoundException {
		if (!mDerivedAttributes.containsKey(name)) {
			throw new AttributeNameNotFoundException(name);
		}
		return mDerivedAttributes.get(name).getDerivedAttributeValue(srcStateVars);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof Action)) {
			return false;
		}
		Action action = (Action) obj;
		return action.mActionName.equals(mActionName) && action.mParameters.equals(mParameters)
				&& action.mAttributes.equals(mAttributes) && action.mDerivedAttributes.equals(mDerivedAttributes);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mActionName.hashCode();
			result = 31 * result + mParameters.hashCode();
			result = 31 * result + mAttributes.hashCode();
			result = 31 * result + mDerivedAttributes.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
