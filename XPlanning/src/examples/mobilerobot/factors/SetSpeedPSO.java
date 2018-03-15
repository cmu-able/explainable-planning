package examples.mobilerobot.factors;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import factors.StateVar;
import factors.StateVarDefinition;
import mdp.Discriminant;
import mdp.EffectClass;
import mdp.IFactoredPSO;
import mdp.ProbabilisticEffect;

/**
 * {@link SetSpeedPSO} is a factored PSO representation of a {@link SetSpeedAction}
 * 
 * @author rsukkerd
 *
 */
public class SetSpeedPSO implements IFactoredPSO {

	private static final double SUCC_SET_SPEED_PROB = 1.0;

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private SetSpeedAction mSetSpeed;

	/**
	 * State variable that is affected by this action.
	 */
	private StateVarDefinition<RobotSpeed> mrSpeedDef;

	/**
	 * Independent effect classes
	 */
	private Set<EffectClass> mEffectClasses;

	public SetSpeedPSO(SetSpeedAction setSpeed, StateVarDefinition<RobotSpeed> rSpeedDef) {
		mSetSpeed = setSpeed;
		mrSpeedDef = rSpeedDef;
		EffectClass speedEffectClass = new EffectClass(setSpeed);
		speedEffectClass.add(rSpeedDef);
		mEffectClasses.add(speedEffectClass);
	}

	public Map<StateVar<RobotSpeed>, Double> getRobotSpeedEffects() {
		Map<StateVar<RobotSpeed>, Double> speedEffects = new HashMap<>();
		Set<RobotSpeed> possibleSpeeds = mrSpeedDef.getPossibleValues();
		for (RobotSpeed speed : possibleSpeeds) {
			StateVar<RobotSpeed> rSpeedDest = new StateVar<>("rSpeed", speed);
			double prob = getRobotSpeedProbability(rSpeedDest);
			speedEffects.put(rSpeedDest, prob);
		}
		return speedEffects;
	}

	private double getRobotSpeedProbability(StateVar<RobotSpeed> rSpeedDest) {
		if (rSpeedDest.getValue().equals(mSetSpeed.getTargetSpeed())) {
			return SUCC_SET_SPEED_PROB;
		}
		return 0;
	}

	@Override
	public Set<EffectClass> getIndependentEffectClasses() {
		return mEffectClasses;
	}

	@Override
	public Map<Discriminant, ProbabilisticEffect> getActionDescription(EffectClass effectClass) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof SetSpeedPSO)) {
			return false;
		}
		SetSpeedPSO pso = (SetSpeedPSO) obj;
		return pso.mSetSpeed.equals(mSetSpeed) && pso.mrSpeedDef.equals(mrSpeedDef)
				&& pso.mEffectClasses.equals(mEffectClasses);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mSetSpeed.hashCode();
			result = 31 * result + mrSpeedDef.hashCode();
			result = 31 * result + mEffectClasses.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
