package language.domain.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import language.exceptions.AttributeNameNotFoundException;

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

	private String mActionNamePrefix;
	private String mActionName;
	private List<IStateVarValue> mParameters = new ArrayList<>();
	private Map<String, IActionAttribute> mAttributes = new HashMap<>();
	private Map<String, DerivedActionAttribute> mDerivedAttributes = new HashMap<>();

	public Action(String actionNamePrefix, IStateVarValue... parameters) {
		mActionNamePrefix = actionNamePrefix;
		StringBuilder builder = new StringBuilder();
		builder.append(actionNamePrefix);
		builder.append("(");
		boolean firstParam = true;
		for (IStateVarValue param : parameters) {
			if (firstParam) {
				firstParam = false;
			} else {
				builder.append(",");
			}
			builder.append(param);
			mParameters.add(param);
		}
		builder.append(")");
		mActionName = builder.toString();
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
	public String getName() {
		return mActionName;
	}

	@Override
	public String getNamePrefix() {
		return mActionNamePrefix;
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
		return action.mActionNamePrefix.equals(mActionNamePrefix) && action.mParameters.equals(mParameters)
				&& action.mAttributes.equals(mAttributes) && action.mDerivedAttributes.equals(mDerivedAttributes);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mActionNamePrefix.hashCode();
			result = 31 * result + mParameters.hashCode();
			result = 31 * result + mAttributes.hashCode();
			result = 31 * result + mDerivedAttributes.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

	@Override
	public String toString() {
		return mActionName;
	}

}
