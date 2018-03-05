package factors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	private List<IStateVarValue> mParameters;
	private Map<String, IActionAttribute> mAttributes;
	private Map<String, DerivedActionAttribute> mDerivedAttributes;

	public Action(String actionName, IStateVarValue... parameters) {
		mActionName = actionName;
		mParameters = new ArrayList<>();
		for (IStateVarValue param : parameters) {
			mParameters.add(param);
		}
		mAttributes = new HashMap<>();
		mDerivedAttributes = new HashMap<>();
	}

	public void putAttributeValue(String name, IActionAttribute value) {
		mAttributes.put(name, value);
	}

	public void putDerivedAttributeValue(String name, IActionAttribute value, IStateVarValue... srcStateVars) {
		if (!mDerivedAttributes.containsKey(name)) {
			DerivedActionAttribute derivedAttr = new DerivedActionAttribute(name);
			derivedAttr.putDerivedAttributeValue(value, srcStateVars);
			mDerivedAttributes.put(name, derivedAttr);
		} else {
			mDerivedAttributes.get(name).putDerivedAttributeValue(value, srcStateVars);
		}
	}

	public String getActionName() {
		return mActionName;
	}

	@Override
	public List<IStateVarValue> getParameters() {
		return mParameters;
	}

	@Override
	public IActionAttribute getAttributeValue(String name) throws AttributeNameNotFoundException {
		if (!mAttributes.containsKey(name)) {
			throw new AttributeNameNotFoundException(name);
		}
		return mAttributes.get(name);
	}

	@Override
	public IActionAttribute getDerivedAttributeValue(String name, IStateVarValue... srcStateVars) throws AttributeNameNotFoundException {
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
		return action.mActionName.equals(mActionName) && action.mParameters.equals(mParameters) && action.mAttributes.equals(mAttributes)
				&& action.mDerivedAttributes.equals(mDerivedAttributes);
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
