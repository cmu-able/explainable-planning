package examples.dart.models;

import language.domain.models.IStateVarAttribute;
import language.domain.models.IStateVarValue;
import language.domain.models.StateVarValue;
import language.exceptions.AttributeNameNotFoundException;

/**
 * {@link TeamFormation} is the type of formation of the team. The possible values are: loose and tight.
 * 
 * @author rsukkerd
 *
 */
public class TeamFormation implements IStateVarValue {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private StateVarValue mValue;

	public TeamFormation(String formation) {
		mValue = new StateVarValue(formation);
	}

	public String getFormation() {
		return mValue.getIdentifier();
	}

	@Override
	public IStateVarAttribute getAttributeValue(String name) throws AttributeNameNotFoundException {
		return mValue.getAttributeValue(name);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof TeamFormation)) {
			return false;
		}
		TeamFormation formation = (TeamFormation) obj;
		return formation.mValue.equals(mValue);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mValue.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

	@Override
	public String toString() {
		return mValue.getIdentifier();
	}

}
