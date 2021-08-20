package examples.mobilerobot.metrics;

import examples.mobilerobot.models.Location;
import examples.mobilerobot.models.MoveToAction;
import examples.mobilerobot.models.RobotSpeed;
import language.domain.metrics.TransitionStructure;
import language.domain.models.ActionDefinition;
import language.domain.models.StateVarDefinition;
/**
 * Defines the domain for the energy quality domain
 * 
 * The energy consumption quality is calculated based on the robot's location and speed,
 * gets information from the MoveToAction, and effects the robot's location (because MoveToAction does)
 * 
 *
 */
public class EnergyConsumptionDomain extends TransitionStructure<MoveToAction> {

	
	private StateVarDefinition<Location> mrLocDef;
	private StateVarDefinition<RobotSpeed> mrSpeedDef;

	public EnergyConsumptionDomain(StateVarDefinition<Location> rLocDef, StateVarDefinition<RobotSpeed> rSpeedDef,
			ActionDefinition<MoveToAction> moveToDef) {
		
		this.mrLocDef = rLocDef;
		this.mrSpeedDef = rSpeedDef;
		addSrcStateVarDef(rLocDef);
		addSrcStateVarDef(rSpeedDef);
		
		setActionDef(moveToDef);
		
		addDestStateVarDef(rLocDef);
	}
	
	public StateVarDefinition<Location> getLocationStateVar() {
		return mrLocDef;
	}
	
	public StateVarDefinition<RobotSpeed> getSpeedStateVar() {
		return mrSpeedDef;
	}

}
