package examples.mobilerobot.metrics;

import examples.mobilerobot.models.Location;
import examples.mobilerobot.models.MoveToAction;
import examples.mobilerobot.models.Occlusion;
import examples.mobilerobot.models.RobotSpeed;
import language.domain.metrics.ITransitionStructure;
import language.domain.metrics.Transition;
import language.domain.metrics.TransitionStructure;
import language.domain.models.ActionDefinition;
import language.domain.models.IStateVarValue;
import language.domain.models.StateVar;
import language.domain.models.StateVarDefinition;
import language.exceptions.AttributeNameNotFoundException;
import language.exceptions.VarNotFoundException;
import language.mdp.StateVarClass;

public class CollisionDomain implements ITransitionStructure<MoveToAction> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private StateVarDefinition<Location> mrLocSrcDef;
	private StateVarDefinition<RobotSpeed> mrSpeedSrcDef;

	private TransitionStructure<MoveToAction> mDomain = new TransitionStructure<>();

	public CollisionDomain(StateVarDefinition<Location> rLocSrcDef, StateVarDefinition<RobotSpeed> rSpeedSrcDef,
			ActionDefinition<MoveToAction> moveToDef) {
		mrLocSrcDef = rLocSrcDef;
		mrSpeedSrcDef = rSpeedSrcDef;

		mDomain.addSrcStateVarDef(rLocSrcDef);
		mDomain.addSrcStateVarDef(rSpeedSrcDef);
		mDomain.setActionDef(moveToDef);
	}

	public RobotSpeed getRobotSpeed(Transition<MoveToAction, CollisionDomain> transition) throws VarNotFoundException {
		return transition.getSrcStateVarValue(RobotSpeed.class, mrSpeedSrcDef);
	}

	public Occlusion getOcclusion(Transition<MoveToAction, CollisionDomain> transition)
			throws VarNotFoundException, AttributeNameNotFoundException {
		Location srcLoc = transition.getSrcStateVarValue(Location.class, mrLocSrcDef);
		StateVar<Location> rLocSrc = mrLocSrcDef.getStateVar(srcLoc);
		return transition.getAction().getOcclusion(rLocSrc);
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
