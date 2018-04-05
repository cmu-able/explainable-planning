package examples.mobilerobot.factors;

import java.util.Set;

import exceptions.DiscriminantNotFoundException;
import exceptions.EffectClassNotFoundException;
import exceptions.VarNotFoundException;
import factors.IStateVarValue;
import factors.StateVarDefinition;
import mdp.Discriminant;
import mdp.DiscriminantClass;
import mdp.EffectClass;
import mdp.FactoredPSO;
import mdp.IActionDescription;
import mdp.IFactoredPSO;
import mdp.Precondition;

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
	private FactoredPSO mActionPSO;

	public SetSpeedPSO(SetSpeedAction setSpeed, Precondition precondition,
			RobotSpeedActionDescription rSpeedActionDesc) {
		mSetSpeed = setSpeed;
		mActionPSO = new FactoredPSO(precondition);
		mActionPSO.addActionDescription(rSpeedActionDesc);
	}

	@Override
	public Precondition getPrecondition() {
		return mActionPSO.getPrecondition();
	}

	@Override
	public Set<EffectClass> getIndependentEffectClasses() {
		return mActionPSO.getIndependentEffectClasses();
	}

	@Override
	public IActionDescription getActionDescription(EffectClass effectClass) throws EffectClassNotFoundException {
		return mActionPSO.getActionDescription(effectClass);
	}

	@Override
	public DiscriminantClass getDiscriminantClass(StateVarDefinition<IStateVarValue> stateVarDef)
			throws VarNotFoundException {
		return mActionPSO.getDiscriminantClass(stateVarDef);
	}

	@Override
	public Set<IStateVarValue> getPossibleImpact(StateVarDefinition<IStateVarValue> stateVarDef,
			Discriminant discriminant) throws VarNotFoundException, DiscriminantNotFoundException {
		return mActionPSO.getPossibleImpact(stateVarDef, discriminant);
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
		return pso.mSetSpeed.equals(mSetSpeed) && pso.mActionPSO.equals(mActionPSO);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mSetSpeed.hashCode();
			result = 31 * result + mActionPSO.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
