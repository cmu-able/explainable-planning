package examples.mobilerobot.metrics;

import examples.mobilerobot.models.MoveToAction;
import examples.mobilerobot.models.RobotBumped;
import examples.mobilerobot.models.RobotSpeed;
import language.domain.metrics.ITransitionStructure;
import language.domain.metrics.Transition;
import language.domain.metrics.TransitionStructure;
import language.domain.models.ActionDefinition;
import language.domain.models.IStateVarValue;
import language.domain.models.StateVarDefinition;
import language.exceptions.VarNotFoundException;
import language.mdp.StateVarClass;

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
	public StateVarClass getSrcStateVarClass() {
		return mDomain.getSrcStateVarClass();
	}

	@Override
	public StateVarClass getDestStateVarClass() {
		return mDomain.getDestStateVarClass();
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
