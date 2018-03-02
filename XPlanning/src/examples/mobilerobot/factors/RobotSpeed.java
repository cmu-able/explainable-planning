package examples.mobilerobot.factors;

import exceptions.AttributeNameNotFoundException;
import factors.IStateVarAttribute;
import factors.IStateVarValue;

public class RobotSpeed implements IStateVarValue {

	private double mSpeed;
	
	public RobotSpeed(double speed) {
		mSpeed = speed;
	}
	
	public double getSpeed() {
		return mSpeed;
	}
	
	@Override
	public IStateVarAttribute getAttributeValue(String name) throws AttributeNameNotFoundException {
		throw new AttributeNameNotFoundException(name);
	}

}
