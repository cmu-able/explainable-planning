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
 * {@link SwitchECMAction} is the type of actions that switch the ECM of the team.
 * 
 * @author rsukkerd
 *
 */
public class SwitchECMAction implements IAction {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Action mAction;
	private TeamECM mteamECMDest;

	public SwitchECMAction(TeamECM ecmDest) {
		mAction = new Action("switchECM", ecmDest);
		mteamECMDest = ecmDest;
	}

	public TeamECM getTargetECM() {
		return mteamECMDest;
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
		if (!(obj instanceof SwitchECMAction)) {
			return false;
		}
		SwitchECMAction switchECM = (SwitchECMAction) obj;
		return switchECM.mAction.equals(mAction) && switchECM.mteamECMDest.equals(mteamECMDest);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mAction.hashCode();
			result = 31 * result + mteamECMDest.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

	@Override
	public String toString() {
		return mAction.toString();
	}

}
