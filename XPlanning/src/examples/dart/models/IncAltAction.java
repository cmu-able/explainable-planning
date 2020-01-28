package examples.dart.models;

import java.util.List;
import java.util.Set;

import language.domain.models.Action;
import language.domain.models.IActionAttribute;
import language.domain.models.IStateVarValue;
import language.domain.models.StateVar;
import language.exceptions.AttributeNameNotFoundException;

/**
 * {@link IncAltAction} is the type of Increase-Altitude action by a specified altitude-change level. This action also
 * flies forward by 1 segment.
 * 
 * @author rsukkerd
 *
 */
public class IncAltAction implements IDurativeAction {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Action mAction;
	private TeamAltitude mteamAltChange;

	public IncAltAction(TeamAltitude teamAltChange) {
		mAction = new Action("incAlt", teamAltChange);
		mteamAltChange = teamAltChange;
	}

	public TeamAltitude getAltitudeChange() {
		return mteamAltChange;
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
		if (!(obj instanceof IncAltAction)) {
			return false;
		}
		IncAltAction incAlt = (IncAltAction) obj;
		return incAlt.mAction.equals(mAction) && incAlt.mteamAltChange.equals(mteamAltChange);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mAction.hashCode();
			result = 31 * result + mteamAltChange.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

	@Override
	public String toString() {
		return mAction.toString();
	}

}
