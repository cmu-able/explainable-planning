package examples.mobilerobot.factors;

import java.util.Map;
import java.util.Set;

import exceptions.AttributeNameNotFoundException;
import exceptions.EffectClassNotFoundException;
import factors.StateVar;
import factors.StateVarDefinition;
import mdp.EffectClass;
import mdp.IActionDescription;
import mdp.IFactoredPSO;

/**
 * {@link MoveToPSO} is a factored PSO representation of a {@link MoveToAction}.
 * 
 * @author rsukkerd
 *
 */
public class MoveToPSO implements IFactoredPSO {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private MoveToAction mMoveTo;

	/**
	 * State variables that are affected by this action.
	 */
	private StateVarDefinition<Location> mrLocDef;
	private StateVarDefinition<RobotBumped> mrBumpedDef;

	/**
	 * Full action descriptions for all independent effect classes of this action
	 */
	private Map<EffectClass, IActionDescription> mActionDescriptions;

	public MoveToPSO(MoveToAction moveTo, StateVarDefinition<Location> rLocDef,
			StateVarDefinition<RobotBumped> rBumpedDef, Set<StateVar<Location>> applicablerLocSrcs)
			throws AttributeNameNotFoundException {
		mMoveTo = moveTo;
		mrLocDef = rLocDef;
		mrBumpedDef = rBumpedDef;
		RobotLocationActionDescription rLocDesc = new RobotLocationActionDescription(moveTo, applicablerLocSrcs,
				rLocDef);
		RobotBumpedActionDescription rBumpedDesc = new RobotBumpedActionDescription(moveTo, applicablerLocSrcs,
				rBumpedDef);
		mActionDescriptions.put(rLocDesc.getEffectClass(), rLocDesc);
		mActionDescriptions.put(rBumpedDesc.getEffectClass(), rBumpedDesc);
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
		if (!(obj instanceof MoveToPSO)) {
			return false;
		}
		MoveToPSO pso = (MoveToPSO) obj;
		return pso.mMoveTo.equals(mMoveTo) && pso.mrLocDef.equals(mrLocDef) && pso.mrBumpedDef.equals(mrBumpedDef)
				&& pso.mActionDescriptions.equals(mActionDescriptions);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mMoveTo.hashCode();
			result = 31 * result + mrLocDef.hashCode();
			result = 31 * result + mrBumpedDef.hashCode();
			result = 31 * result + mActionDescriptions.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
