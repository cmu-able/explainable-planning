package examples.mobilerobot.metrics;

import java.util.Set;

import examples.mobilerobot.factors.MoveToAction;
import examples.mobilerobot.factors.RobotBumped;
import examples.mobilerobot.factors.RobotSpeed;
import exceptions.VarNotFoundException;
import factors.ActionDefinition;
import factors.IStateVarValue;
import factors.StateVarDefinition;
import metrics.IQFunctionDomain;
import metrics.QFunctionDomain;
import metrics.Transition;

public class CollisionDomain implements IQFunctionDomain<MoveToAction> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private StateVarDefinition<RobotSpeed> mrSpeedSrcDef;
	private StateVarDefinition<RobotBumped> mrBumpedDestDef;

	private QFunctionDomain<MoveToAction> mDomain = new QFunctionDomain<>();

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
