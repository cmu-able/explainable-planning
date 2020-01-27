package examples.dart.models;

import java.util.List;
import java.util.Set;

import language.domain.models.Action;
import language.domain.models.IActionAttribute;
import language.domain.models.IStateVarValue;
import language.domain.models.StateVar;
import language.exceptions.AttributeNameNotFoundException;

/**
 * {@link FlyAction} is the action that flies horizontally for 1 segment and does not change altitude of the team.
 * 
 * @author rsukkerd
 *
 */
public class FlyAction implements IDurativeAction {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Action mAction;

	public FlyAction() {
		mAction = new Action("fly");
	}

	@Override
	public String getName() {
		return mAction.getName();
	}

	@Override
	public String getNamePrefix() {
		return mAction.getNamePrefix();
	}

	@Override
	public List<IStateVarValue> getParameters() {
		return mAction.getParameters();
	}

	@Override
	public IActionAttribute getAttributeValue(String name) throws AttributeNameNotFoundException {
		return mAction.getAttributeValue(name);
	}

	@Override
	public IActionAttribute getDerivedAttributeValue(String name, Set<StateVar<? extends IStateVarValue>> srcStateVars)
			throws AttributeNameNotFoundException {
		return mAction.getDerivedAttributeValue(name, srcStateVars);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof FlyAction)) {
			return false;
		}
		FlyAction fly = (FlyAction) obj;
		return fly.mAction.equals(mAction);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mAction.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

	@Override
	public String toString() {
		return mAction.toString();
	}

}
