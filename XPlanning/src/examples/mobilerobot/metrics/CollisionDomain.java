package examples.mobilerobot.metrics;

import java.util.Set;

import examples.mobilerobot.qfactors.MoveToAction;
import examples.mobilerobot.qfactors.RobotBumped;
import examples.mobilerobot.qfactors.RobotSpeed;
import language.domain.models.ActionDefinition;
import language.domain.models.IStateVarValue;
import language.domain.models.StateVarDefinition;
import language.exceptions.VarNotFoundException;
import language.metrics.ITransitionStructure;
import language.metrics.TransitionStructure;
import language.metrics.Transition;

public class CollisionDomain implements ITransitionStructure<MoveToAction> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private StateVarDefinition<RobotSpeed> mrSpeedSrcDef;
	private StateVarDefinition<RobotBumped> mrBumpedDestDef;

	private TransitionStructure<MoveToAction> mDomain = new TransitionStructure<>();

	public CollisionDomain(StateVarDefinition<RobotSpeed> rSpeedSrcDef, ActionDefinition<MoveToAction> moveToDef,
			StateVarDefinition<RobotBumped> rBumpedDestDef) {
		mrSpeedSrcDef = rSpeedSrcDef;
		mrBumpedDestDef = rBumpedDestDef;

		mDomain.addSrcStateVarDef(rSpeedSrcDef);
		mDomain.setActionDef(moveToDef);
		mDomain.addDestStateVarDef(rBumpedDestDef);
	}

	public RobotSpeed getRobotSpeed(Transition<MoveToAction, CollisionDomain> transition) throws VarNotFoundException {
		return transition.getSrcStateVarValue(RobotSpeed.class, mrSpeedSrcDef);
	}

	public RobotBumped getRobotBumped(Transition<MoveToAction, CollisionDomain> transition)
			throws VarNotFoundException {
		return transition.getDestStateVarValue(RobotBumped.class, mrBumpedDestDef);
	}

	@Override
	public Set<StateVarDefinition<IStateVarValue>> getSrcStateVarDefs() {
		return mDomain.getSrcStateVarDefs();
	}

	@Override
	public Set<StateVarDefinition<IStateVarValue>> getDestStateVarDefs() {
		return mDomain.getDestStateVarDefs();
	}

	@Override
	public ActionDefinition<MoveToAction> getActionDef() {
		return mDomain.getActionDef();
	}

	@Override
	public boolean containsSrcStateVarDef(StateVarDefinition<? extends IStateVarValue> srcVarDef) {
		return mDomain.containsSrcStateVarDef(srcVarDef);
	}

	@Override
	public boolean containsDestStateVarDef(StateVarDefinition<? extends IStateVarValue> destVarDef) {
		return mDomain.containsDestStateVarDef(destVarDef);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof CollisionDomain)) {
			return false;
		}
		CollisionDomain domain = (CollisionDomain) obj;
		return domain.mDomain.equals(mDomain);
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
