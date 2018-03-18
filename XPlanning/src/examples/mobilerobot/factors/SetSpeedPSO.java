package examples.mobilerobot.factors;

import java.util.Map;
import java.util.Set;

import exceptions.EffectClassNotFoundException;
import factors.StateVarDefinition;
import mdp.EffectClass;
import mdp.IActionDescription;
import mdp.IFactoredPSO;

/**
 * {@link SetSpeedPSO} is a factored PSO representation of a {@link SetSpeedAction}
 * 
 * @author rsukkerd
 *
 */
public class SetSpeedPSO implements IFactoredPSO {

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
	 * Full action descriptions for all independent effect classes of this action
	 */
	private Map<EffectClass, IActionDescription> mActionDescriptions;

	public SetSpeedPSO(SetSpeedAction setSpeed, StateVarDefinition<RobotSpeed> rSpeedDef) {
		mSetSpeed = setSpeed;
		mrSpeedDef = rSpeedDef;
		RobotSpeedActionDescription rSpeedDesc = new RobotSpeedActionDescription(setSpeed, rSpeedDef);
		mActionDescriptions.put(rSpeedDesc.getEffectClass(), rSpeedDesc);
	}

	@Override
	public Set<EffectClass> getIndependentEffectClasses() {
		return mActionDescriptions.keySet();
	}

	@Override
	public IActionDescription getActionDescription(EffectClass effectClass) throws EffectClassNotFoundException {
		if (!mActionDescriptions.containsKey(effectClass)) {
			throw new EffectClassNotFoundException(effectClass);
		}
		return mActionDescriptions.get(effectClass);
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
				&& pso.mActionDescriptions.equals(mActionDescriptions);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mSetSpeed.hashCode();
			result = 31 * result + mrSpeedDef.hashCode();
			result = 31 * result + mActionDescriptions.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
