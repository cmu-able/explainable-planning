package examples.mobilerobot.factors;

import factors.IStateVar;
import factors.IStateVarValue;
import factors.StateVar;

/**
 * {@link LocationStateVar} is a state variable whose value is of type {@link Location}.
 * 
 * @author rsukkerd
 *
 */
public class LocationStateVar implements IStateVar {

	private StateVar mStateVar;
	private Location mLocation;

	public LocationStateVar(String name, Location location) {
		mStateVar = new StateVar(name, location);
		mLocation = location;
	}

	public Location getLocation() {
		return mLocation;
	}

	@Override
	public String getName() {
		return mStateVar.getName();
	}

	@Override
	public IStateVarValue getValue() {
		return mStateVar.getValue();
	}

}
