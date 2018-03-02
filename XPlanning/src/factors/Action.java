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
	
	private String mActionName;
	private List<IStateVarValue> mParameters;
	private Map<String, IActionAttribute> mAttributes;
	private Map<String, DerivedActionAttribute> mDerivedAttributes;
	
	public Action(String actionName, IStateVarValue... parameters) {
		mActionName = actionName;
		if (parameters.length > 0) {
			mParameters = new ArrayList<>();
			for (IStateVarValue param : parameters) {
				mParameters.add(param);
			}
		}
	}
	
	public void putAttributeValue(String name, IActionAttribute value) {
		mAttributes.put(name, value);
	}
	
	public void putDerivedAttributeValue(String name, IActionAttribute value, IStateVarValue... srcStateVars) {
		if (mDerivedAttributes == null) {
			mDerivedAttributes = new HashMap<>();
		} else if (!mDerivedAttributes.containsKey(name)) {
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
		if (mAttributes == null || !mAttributes.containsKey(name)) {
			throw new AttributeNameNotFoundException(name);
		}
		return mAttributes.get(name);
	}

	@Override
	public IActionAttribute getDerivedAttributeValue(String name, IStateVarValue... srcStateVars) 
			throws AttributeNameNotFoundException {
		if (mDerivedAttributes == null || !mDerivedAttributes.containsKey(name)) {
			throw new AttributeNameNotFoundException(name);
		}
		return mDerivedAttributes.get(name).getDerivedAttributeValue(srcStateVars);
	}

}
