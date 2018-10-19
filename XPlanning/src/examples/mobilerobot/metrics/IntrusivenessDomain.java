package examples.mobilerobot.metrics;

import examples.mobilerobot.models.Area;
import examples.mobilerobot.models.Location;
import examples.mobilerobot.models.MoveToAction;
import language.domain.metrics.ITransitionStructure;
import language.domain.metrics.Transition;
import language.domain.metrics.TransitionStructure;
import language.domain.models.ActionDefinition;
import language.domain.models.IStateVarValue;
import language.domain.models.StateVarDefinition;
import language.exceptions.AttributeNameNotFoundException;
import language.exceptions.VarNotFoundException;
import language.mdp.StateVarClass;

public class IntrusivenessDomain implements ITransitionStructure<MoveToAction> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private StateVarDefinition<Location> mrLocDestDef;

	private TransitionStructure<MoveToAction> mDomain = new TransitionStructure<>();

	public IntrusivenessDomain(ActionDefinition<MoveToAction> moveToDef, StateVarDefinition<Location> rLocDestDef) {
		mrLocDestDef = rLocDestDef;

		mDomain.setActionDef(moveToDef);
		mDomain.addDestStateVarDef(rLocDestDef);
	}

	public Area getArea(Transition<MoveToAction, IntrusivenessDomain> transition)
			throws VarNotFoundException, AttributeNameNotFoundException {
		Location locDest = transition.getDestStateVarValue(Location.class, mrLocDestDef);
		return locDest.getArea();
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
		if (!(obj instanceof IntrusivenessDomain)) {
			return false;
		}
		IntrusivenessDomain domain = (IntrusivenessDomain) obj;
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
