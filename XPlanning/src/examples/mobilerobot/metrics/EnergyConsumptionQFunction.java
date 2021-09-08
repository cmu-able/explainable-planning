package examples.mobilerobot.metrics;

import examples.mobilerobot.models.Distance;
import examples.mobilerobot.models.HeadlampState;
import examples.mobilerobot.models.Location;
import examples.mobilerobot.models.MoveToAction;
import examples.mobilerobot.models.RobotSpeed;
import language.domain.metrics.IStandardMetricQFunction;
import language.domain.metrics.Transition;
import language.exceptions.AttributeNameNotFoundException;
import language.exceptions.VarNotFoundException;
/**
 * Calculates the energy consumption of the robot on a single transition. For now,
 * let's keep this simple. Let's assume the robot has a constant consumption rate based on the
 * time to traverse the segment, which is calculated from the speed and the distance traveled by
 * the MovetoAction.
 * 
 * Let's assume it consumes 20mw per hour when stationary and increases by 1.5mw per hour for 
 * every 0.1m/s increase in speed.
 *
 */
public class EnergyConsumptionQFunction implements IStandardMetricQFunction<MoveToAction, EnergyConsumptionDomain> {

	public static final String NAME = "energyConsumption";
	
	public static final double BASE_DISCHARGE_RATE = 20.0;
	public static final double SPEED_INCREASE_DISCHARGE_RATE = 1.5;
	public static final double HEADLAMP_DISCHARGE_RATE = 20.0;

	private EnergyConsumptionDomain mDomain;
	
	public EnergyConsumptionQFunction(EnergyConsumptionDomain domain) {
		this.mDomain = domain;
		
	}
	
	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public EnergyConsumptionDomain getTransitionStructure() {
		return mDomain;
	}

	@Override
	public double getValue(Transition<MoveToAction, EnergyConsumptionDomain> transition)
			throws VarNotFoundException, AttributeNameNotFoundException {
		// Calculate the distance based on the location and the movetoaction
		MoveToAction moveTo = transition.getAction();
		Location comingFrom = transition.getSrcStateVarValue(Location.class, mDomain.getLocationStateVar());
		Distance distance = moveTo.getDistance(mDomain.getLocationStateVar().getStateVar(comingFrom));
		
		RobotSpeed speed = transition.getSrcStateVarValue(RobotSpeed.class, mDomain.getSpeedStateVar());
		
		// If the headlamp is on, then need to discharge at a higher rate
		HeadlampState hl = transition.getSrcStateVarValue(HeadlampState.class, mDomain.getHeadlampStateVar());
		
		double consumption = distance.getDistance() / speed.getSpeed() * 
				(BASE_DISCHARGE_RATE 
						+ speed.getSpeed() * SPEED_INCREASE_DISCHARGE_RATE * 10
						+ (hl.getValue() ? HEADLAMP_DISCHARGE_RATE : 0));
		
		return consumption;
	}
	
	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof EnergyConsumptionQFunction)) {
			return false;
		}
		EnergyConsumptionQFunction qFunction = (EnergyConsumptionQFunction) obj;
		return qFunction.mDomain.equals(mDomain);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mDomain.hashCode();
			hashCode = result;
		}
		return result;
	}

}
