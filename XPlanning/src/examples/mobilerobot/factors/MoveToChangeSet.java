package examples.mobilerobot.factors;

import factors.IChangeSet;
import factors.StateVar;

/**
 * {@link MoveToChangeSet} is a change set of a {@link MoveToAction}.
 * 
 * @author rsukkerd
 *
 */
public class MoveToChangeSet implements IChangeSet {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private StateVar<Location> mLocationChange;
	private StateVar<RobotBumped> mBumpedChange;

	public MoveToChangeSet(StateVar<Location> locationChange, StateVar<RobotBumped> bumpedChange) {
		mLocationChange = locationChange;
		mBumpedChange = bumpedChange;
	}

	public StateVar<Location> getLocationChange() {
		return mLocationChange;
	}

	public StateVar<RobotBumped> getRobotBumpedChange() {
		return mBumpedChange;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof MoveToChangeSet)) {
			return false;
		}
		MoveToChangeSet changeSet = (MoveToChangeSet) obj;
		return changeSet.mLocationChange.equals(mLocationChange) && changeSet.mBumpedChange.equals(mBumpedChange);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = result * 31 + mLocationChange.hashCode();
			result = result * 31 + mBumpedChange.hashCode();
			hashCode = result;
		}
		return result;
	}
}
