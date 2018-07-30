package examples.mobilerobot.metrics;

import java.util.Set;

import examples.mobilerobot.factors.Distance;
import examples.mobilerobot.factors.Location;
import examples.mobilerobot.factors.MoveToAction;
import examples.mobilerobot.factors.RobotSpeed;
import factors.ActionDefinition;
import factors.IStateVarValue;
import factors.StateVarDefinition;
import language.exceptions.AttributeNameNotFoundException;
import language.exceptions.VarNotFoundException;
import metrics.IQFunctionDomain;
import metrics.QFunctionDomain;
import metrics.Transition;

public class TravelTimeDomain implements IQFunctionDomain<MoveToAction> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private StateVarDefinition<Location> mrLocSrcDef;
	private StateVarDefinition<RobotSpeed> mrSpeedSrcDef;

	private QFunctionDomain<MoveToAction> mDomain = new QFunctionDomain<>();

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
