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
 * {@link DecAltSegmentActionDescription} is the action description for the "route segment" effect class of an instance
 * of {@link DecAltAction} action type. It uses a {@link FormulaActionDescription} that uses
 * {@link RouteSegmentFormula}.
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
public class DecAltSegmentActionDescription implements IActionDescription<DecAltAction> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private FormulaActionDescription<DecAltAction> mDecAltSegmentActionDesc;

	public DecAltSegmentActionDescription(ActionDefinition<DecAltAction> decAltDef,
			Precondition<DecAltAction> precondition, StateVarDefinition<RouteSegment> segmentDef) {
		// Discriminant class (i.e., discriminant variables)
		DiscriminantClass discrClass = new DiscriminantClass();
		discrClass.add(segmentDef);

		// Effect class (i.e., effect variables)
		EffectClass effectClass = new EffectClass();
		effectClass.add(segmentDef);

		// Probabilistic transition formula of "route segment" effect class, of a durative action
		RouteSegmentFormula<DecAltAction> segmentFormula = new RouteSegmentFormula<>(segmentDef);

		// Formula action description of "route segment" effect class, of DecAlt actions
		mDecAltSegmentActionDesc = new FormulaActionDescription<>(decAltDef, precondition, discrClass, effectClass,
				segmentFormula);
	}

	@Override
	public Set<ProbabilisticTransition<DecAltAction>> getProbabilisticTransitions(DecAltAction action)
			throws XMDPException {
		return mDecAltSegmentActionDesc.getProbabilisticTransitions(action);
	}

	@Override
	public ProbabilisticEffect getProbabilisticEffect(Discriminant discriminant, DecAltAction action)
			throws XMDPException {
		return mDecAltSegmentActionDesc.getProbabilisticEffect(discriminant, action);
	}

	@Override
	public ActionDefinition<DecAltAction> getActionDefinition() {
		return mDecAltSegmentActionDesc.getActionDefinition();
	}

	@Override
	public DiscriminantClass getDiscriminantClass() {
		return mDecAltSegmentActionDesc.getDiscriminantClass();
	}

	@Override
	public EffectClass getEffectClass() {
		return mDecAltSegmentActionDesc.getEffectClass();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof DecAltSegmentActionDescription)) {
			return false;
		}
		DecAltSegmentActionDescription actionDesc = (DecAltSegmentActionDescription) obj;
		return actionDesc.mDecAltSegmentActionDesc.equals(mDecAltSegmentActionDesc);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mDecAltSegmentActionDesc.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
