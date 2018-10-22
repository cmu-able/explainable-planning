package examples.mobilerobot.models;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import language.domain.models.Action;
import language.domain.models.IAction;
import language.domain.models.IActionAttribute;
import language.domain.models.IStateVarValue;
import language.domain.models.StateVar;
import language.exceptions.AttributeNameNotFoundException;

/**
 * {@link MoveToAction} is a type of actions that move the robot to specified destinations. It has an associated
 * distance.
 * 
 * @author rsukkerd
 *
 */
public class MoveToAction implements IAction {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Action mAction;
	private StateVar<Location> mrLocDest;

	public MoveToAction(StateVar<Location> rLocDest) {
		mAction = new Action("moveTo", rLocDest.getValue());
		mrLocDest = rLocDest;
	}

	public void putDistanceValue(Distance distance, StateVar<Location> rLocSrc) {
		Set<StateVar<? extends IStateVarValue>> varSet = new HashSet<>();
		varSet.add(rLocSrc);
		mAction.putDerivedAttributeValue("distance", distance, varSet);
	}

	public void putOcclusionValue(Occlusion occlusion, StateVar<Location> rLocSrc) {
		Set<StateVar<? extends IStateVarValue>> varSet = new HashSet<>();
		varSet.add(rLocSrc);
		mAction.putDerivedAttributeValue("occlusion", occlusion, varSet);
	}

	public Location getDestination() {
		return mrLocDest.getValue();
	}

	public Distance getDistance(StateVar<Location> rLocSrc) throws AttributeNameNotFoundException {
		Set<StateVar<? extends IStateVarValue>> varSet = new HashSet<>();
		varSet.add(rLocSrc);
		return (Distance) getDerivedAttributeValue("distance", varSet);
	}

	public Occlusion getOcclusion(StateVar<Location> rLocSrc) throws AttributeNameNotFoundException {
		Set<StateVar<? extends IStateVarValue>> varSet = new HashSet<>();
		varSet.add(rLocSrc);
		return (Occlusion) getDerivedAttributeValue("occlusion", varSet);
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
		if (!(obj instanceof MoveToAction)) {
			return false;
		}
		MoveToAction moveTo = (MoveToAction) obj;
		return moveTo.mAction.equals(mAction) && moveTo.mrLocDest.equals(mrLocDest);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mAction.hashCode();
			result = 31 * result + mrLocDest.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

	@Override
	public String toString() {
		return mAction.toString();
	}
}
