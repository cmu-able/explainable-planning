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
	private FactoredPSO mActionPSO;

	public MoveToPSO(MoveToAction moveTo, Precondition precondition, RobotLocationActionDescription rLocActionDesc,
			RobotBumpedActionDescription rBumpedActionDesc) {
		mMoveTo = moveTo;
		mActionPSO = new FactoredPSO(precondition);
		mActionPSO.addActionDescription(rLocActionDesc);
		mActionPSO.addActionDescription(rBumpedActionDesc);
	}

	public MoveToAction getAction() {
		return mMoveTo;
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
		if (!(obj instanceof MoveToPSO)) {
			return false;
		}
		MoveToPSO pso = (MoveToPSO) obj;
		return pso.mMoveTo.equals(mMoveTo) && pso.mActionPSO.equals(mActionPSO);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mMoveTo.hashCode();
			result = 31 * result + mActionPSO.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
