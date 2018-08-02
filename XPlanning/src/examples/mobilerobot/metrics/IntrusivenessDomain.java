package examples.mobilerobot.metrics;

import java.util.Set;

import examples.mobilerobot.qfactors.Area;
import examples.mobilerobot.qfactors.Location;
import examples.mobilerobot.qfactors.MoveToAction;
import language.exceptions.AttributeNameNotFoundException;
import language.exceptions.VarNotFoundException;
import language.metrics.IQFunctionDomain;
import language.metrics.QFunctionDomain;
import language.metrics.Transition;
import language.qfactors.ActionDefinition;
import language.qfactors.IStateVarValue;
import language.qfactors.StateVarDefinition;

public class IntrusivenessDomain implements IQFunctionDomain<MoveToAction> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private StateVarDefinition<Location> mrLocDestDef;

	private QFunctionDomain<MoveToAction> mDomain = new QFunctionDomain<>();

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
