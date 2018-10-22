package examples.clinicscheduling.models;

import java.util.List;
import java.util.Set;

import language.domain.models.Action;
import language.domain.models.IAction;
import language.domain.models.IActionAttribute;
import language.domain.models.IStateVarValue;
import language.domain.models.StateVar;
import language.exceptions.AttributeNameNotFoundException;

public class ScheduleAction implements IAction {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Action mAction;
	private ABP mNewABP;
	private ClientCount mNumNewClientsToService;

	public ScheduleAction(ABP newABP, ClientCount numNewClientsToService) {
		mAction = new Action("schedule", newABP, numNewClientsToService);
		mNewABP = newABP;
		mNumNewClientsToService = numNewClientsToService;
	}

	public ABP getNewABP() {
		return mNewABP;
	}

	public ClientCount getNumNewClientsToService() {
		return mNumNewClientsToService;
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
		throw new UnsupportedOperationException();
	}

	@Override
	public IActionAttribute getDerivedAttributeValue(String name, Set<StateVar<? extends IStateVarValue>> srcStateVars)
			throws AttributeNameNotFoundException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof ScheduleAction)) {
			return false;
		}
		ScheduleAction schedule = (ScheduleAction) obj;
		return schedule.mAction.equals(mAction) && schedule.mNewABP.equals(mNewABP)
				&& schedule.mNumNewClientsToService.equals(mNumNewClientsToService);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mAction.hashCode();
			result = 31 * result + mNewABP.hashCode();
			result = 31 * result + mNumNewClientsToService.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

	@Override
	public String toString() {
		return mAction.toString();
	}

}
