package examples.dart.models;

import java.util.List;
import java.util.Set;

import language.domain.models.Action;
import language.domain.models.IAction;
import language.domain.models.IActionAttribute;
import language.domain.models.IStateVarValue;
import language.domain.models.StateVar;
import language.exceptions.AttributeNameNotFoundException;

/**
 * {@link ChangeFormAction} is the type of actions that change the formation of the team.
 * 
 * @author rsukkerd
 *
 */
public class ChangeFormAction implements IAction {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Action mAction;
	private TeamFormation mteamFormDest;

	public ChangeFormAction(TeamFormation formDest) {
		mAction = new Action("changeForm", formDest);
		mteamFormDest = formDest;
	}

	public TeamFormation getTargetFormation() {
		return mteamFormDest;
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
		if (!(obj instanceof ChangeFormAction)) {
			return false;
		}
		ChangeFormAction changeForm = (ChangeFormAction) obj;
		return changeForm.mAction.equals(mAction) && changeForm.mteamFormDest.equals(mteamFormDest);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mAction.hashCode();
			result = 31 * result + mteamFormDest.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

	@Override
	public String toString() {
		return mAction.toString();
	}

}
