package examples.dart.models;

import java.util.Set;

import language.domain.models.ActionDefinition;
import language.domain.models.StateVarDefinition;
import language.exceptions.XMDPException;
import language.mdp.Discriminant;
import language.mdp.DiscriminantClass;
import language.mdp.EffectClass;
import language.mdp.FormulaActionDescription;
import language.mdp.IActionDescription;
import language.mdp.Precondition;
import language.mdp.ProbabilisticEffect;
import language.mdp.ProbabilisticTransition;

/**
 * {@link FlySegmentActionDescription} is the action description for the "route segment" effect class of an instance of
 * {@link FlyAction} action type. It uses a {@link FormulaActionDescription} that uses {@link RouteSegmentFormula}.
 * 
 * In the future, the constructor of this type may read an input formula for the "route segment" effect and create a
 * {@link RouteSegmentFormula} accordingly.
 * 
 * Note: All actions of type {@link IDurativeAction} have the same structure of action description for the "route
 * segment" effect class. The only difference is the {@link ActionDefinition} and its {@link Precondition}.
 * 
 * @author rsukkerd
 *
 */
public class FlySegmentActionDescription implements IActionDescription<FlyAction> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private FormulaActionDescription<FlyAction> mFlySegmentActionDesc;

	public FlySegmentActionDescription(ActionDefinition<FlyAction> flyDef, Precondition<FlyAction> precondition,
			StateVarDefinition<RouteSegment> segmentDef) {
		// Discriminant class (i.e., discriminant variables)
		DiscriminantClass discrClass = new DiscriminantClass();
		discrClass.add(segmentDef);

		// Effect class (i.e., effect variables)
		EffectClass effectClass = new EffectClass();
		effectClass.add(segmentDef);

		// Probabilistic transition formula of "route segment" effect class, of a durative action
		RouteSegmentFormula<FlyAction> segmentFormula = new RouteSegmentFormula<>(segmentDef);

		// Formula action description of "route segment" effect class, of Fly actions
		mFlySegmentActionDesc = new FormulaActionDescription<>(flyDef, precondition, discrClass, effectClass,
				segmentFormula);
	}

	@Override
	public Set<ProbabilisticTransition<FlyAction>> getProbabilisticTransitions(FlyAction action) throws XMDPException {
		return mFlySegmentActionDesc.getProbabilisticTransitions(action);
	}

	@Override
	public ProbabilisticEffect getProbabilisticEffect(Discriminant discriminant, FlyAction action)
			throws XMDPException {
		return mFlySegmentActionDesc.getProbabilisticEffect(discriminant, action);
	}

	@Override
	public ActionDefinition<FlyAction> getActionDefinition() {
		return mFlySegmentActionDesc.getActionDefinition();
	}

	@Override
	public DiscriminantClass getDiscriminantClass() {
		return mFlySegmentActionDesc.getDiscriminantClass();
	}

	@Override
	public EffectClass getEffectClass() {
		return mFlySegmentActionDesc.getEffectClass();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof FlySegmentActionDescription)) {
			return false;
		}
		FlySegmentActionDescription actionDesc = (FlySegmentActionDescription) obj;
		return actionDesc.mFlySegmentActionDesc.equals(mFlySegmentActionDesc);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mFlySegmentActionDesc.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
