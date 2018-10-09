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
	private AdvanceBookingPolicy mNewABP;
	private ClientCount mNumClientsToService;

	public ScheduleAction(AdvanceBookingPolicy newABP, ClientCount numClientsToService) {
		mAction = new Action("schedule", newABP, numClientsToService);
		mNewABP = newABP;
		mNumClientsToService = numClientsToService;
	}

	public AdvanceBookingPolicy getNewAdvanceBookingPolicy() {
		return mNewABP;
	}

	public ClientCount getNumClientsToService() {
		return mNumClientsToService;
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
				&& schedule.mNumClientsToService.equals(mNumClientsToService);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mAction.hashCode();
			result = 31 * result + mNewABP.hashCode();
			result = 31 * result + mNumClientsToService.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
