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
 * {@link IncAltSegmentActionDescription} is the action description for the "route segment" effect class of an instance
 * of {@link IncAltAction} action type. It uses a {@link FormulaActionDescription} that uses
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
public class IncAltSegmentActionDescription implements IActionDescription<IncAltAction> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private FormulaActionDescription<IncAltAction> mIncAltSegmentActionDesc;

	public IncAltSegmentActionDescription(ActionDefinition<IncAltAction> incAltDef,
			Precondition<IncAltAction> precondition, StateVarDefinition<RouteSegment> segmentDef) {
		// Discriminant class (i.e., discriminant variables)
		DiscriminantClass discrClass = new DiscriminantClass();
		discrClass.add(segmentDef);

		// Effect class (i.e., effect variables)
		EffectClass effectClass = new EffectClass();
		effectClass.add(segmentDef);

		// Probabilistic transition formula of "route segment" effect class, of a durative action
		RouteSegmentFormula<IncAltAction> segmentFormula = new RouteSegmentFormula<>(segmentDef);

		// Formula action description of "route segment" effect class, of IncAlt actions
		mIncAltSegmentActionDesc = new FormulaActionDescription<>(incAltDef, precondition, discrClass, effectClass,
				segmentFormula);
	}

	@Override
	public Set<ProbabilisticTransition<IncAltAction>> getProbabilisticTransitions(IncAltAction action)
			throws XMDPException {
		return mIncAltSegmentActionDesc.getProbabilisticTransitions(action);
	}

	@Override
	public ProbabilisticEffect getProbabilisticEffect(Discriminant discriminant, IncAltAction action)
			throws XMDPException {
		return mIncAltSegmentActionDesc.getProbabilisticEffect(discriminant, action);
	}

	@Override
	public ActionDefinition<IncAltAction> getActionDefinition() {
		return mIncAltSegmentActionDesc.getActionDefinition();
	}

	@Override
	public DiscriminantClass getDiscriminantClass() {
		return mIncAltSegmentActionDesc.getDiscriminantClass();
	}

	@Override
	public EffectClass getEffectClass() {
		return mIncAltSegmentActionDesc.getEffectClass();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof IncAltSegmentActionDescription)) {
			return false;
		}
		IncAltSegmentActionDescription actionDesc = (IncAltSegmentActionDescription) obj;
		return actionDesc.mIncAltSegmentActionDesc.equals(mIncAltSegmentActionDesc);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mIncAltSegmentActionDesc.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
