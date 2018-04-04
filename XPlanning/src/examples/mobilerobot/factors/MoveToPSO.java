package examples.mobilerobot.factors;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import exceptions.EffectClassNotFoundException;
import factors.IStateVarValue;
import factors.StateVarDefinition;
import mdp.Discriminant;
import mdp.DiscriminantClass;
import mdp.EffectClass;
import mdp.IActionDescription;
import mdp.IFactoredPSO;
import mdp.Precondition;

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
	 * Precondition of this action
	 */
	private Precondition mPrecondition;

	/**
	 * Full action descriptions for all independent effect classes of this action
	 */
	private Map<EffectClass, IActionDescription> mActionDescriptions;

	public MoveToPSO(MoveToAction moveTo, Precondition precondition, RobotLocationActionDescription rLocActionDesc,
			RobotBumpedActionDescription rBumpedActionDesc) {
		mMoveTo = moveTo;
		mPrecondition = precondition;
		mActionDescriptions = new HashMap<>();
		mActionDescriptions.put(rLocActionDesc.getEffectClass(), rLocActionDesc);
		mActionDescriptions.put(rBumpedActionDesc.getEffectClass(), rBumpedActionDesc);
	}

	@Override
	public Precondition getPrecondition() {
		return mPrecondition;
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
	public DiscriminantClass getDiscriminantClass(StateVarDefinition<IStateVarValue> stateVarDef) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<IStateVarValue> getPossibleImpact(StateVarDefinition<IStateVarValue> stateVarDef,
			Discriminant discriminant) {
		// TODO Auto-generated method stub
		return null;
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
		return pso.mMoveTo.equals(mMoveTo) && pso.mPrecondition.equals(mPrecondition)
				&& pso.mActionDescriptions.equals(mActionDescriptions);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mMoveTo.hashCode();
			result = 31 * result + mPrecondition.hashCode();
			result = 31 * result + mActionDescriptions.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
