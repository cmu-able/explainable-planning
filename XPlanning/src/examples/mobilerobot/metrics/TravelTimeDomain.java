package examples.mobilerobot.metrics;

import examples.mobilerobot.models.Distance;
import examples.mobilerobot.models.Location;
import examples.mobilerobot.models.MoveToAction;
import examples.mobilerobot.models.Occlusion;
import examples.mobilerobot.models.RobotSpeed;
import language.domain.metrics.ITransitionStructure;
import language.domain.metrics.Transition;
import language.domain.metrics.TransitionStructure;
import language.domain.models.ActionDefinition;
import language.domain.models.IStateVarValue;
import language.domain.models.StateVarDefinition;
import language.exceptions.AttributeNameNotFoundException;
import language.exceptions.VarNotFoundException;
import language.mdp.StateVarClass;

public class TravelTimeDomain implements ITransitionStructure<MoveToAction> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private StateVarDefinition<Location> mrLocSrcDef;
	private StateVarDefinition<RobotSpeed> mrSpeedSrcDef;

	private TransitionStructure<MoveToAction> mDomain = new TransitionStructure<>();

	public TravelTimeDomain(StateVarDefinition<Location> rLocSrcDef, StateVarDefinition<RobotSpeed> rSpeedSrcDef,
			ActionDefinition<MoveToAction> moveToDef, StateVarDefinition<Location> rLocDestDef) {
		mrLocSrcDef = rLocSrcDef;
		mrSpeedSrcDef = rSpeedSrcDef;

		mDomain.addSrcStateVarDef(rLocSrcDef);
		mDomain.addSrcStateVarDef(rSpeedSrcDef);
		mDomain.setActionDef(moveToDef);
		mDomain.addDestStateVarDef(rLocDestDef);
	}

	public RobotSpeed getRobotSpeed(Transition<MoveToAction, TravelTimeDomain> transition) throws VarNotFoundException {
		return transition.getSrcStateVarValue(RobotSpeed.class, mrSpeedSrcDef);
	}

	public Distance getDistance(Transition<MoveToAction, TravelTimeDomain> transition)
			throws VarNotFoundException, AttributeNameNotFoundException {
		MoveToAction moveTo = transition.getAction();
		Location locSrc = transition.getSrcStateVarValue(Location.class, mrLocSrcDef);
		return moveTo.getDistance(mrLocSrcDef.getStateVar(locSrc));
	}

	public Occlusion getOcclusion(Transition<MoveToAction, TravelTimeDomain> transition)
			throws VarNotFoundException, AttributeNameNotFoundException {
		MoveToAction moveTo = transition.getAction();
		Location locSrc = transition.getSrcStateVarValue(Location.class, mrLocSrcDef);
		return moveTo.getOcclusion(mrLocSrcDef.getStateVar(locSrc));
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
		if (!(obj instanceof TravelTimeDomain)) {
			return false;
		}
		TravelTimeDomain domain = (TravelTimeDomain) obj;
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
