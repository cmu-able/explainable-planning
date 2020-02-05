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
 * {@link RouteSegmentActionDescription} is the composite action description for the "route segment" effect class of an
 * instance of {@link IDurativeAction} action type. It uses a {@link FormulaActionDescription} that uses
 * {@link RouteSegmentFormula}. The actions {@link IncAltAction}, {@link DecAltAction}, and {@link FlyAction} have the
 * same action description for the "route segment" effect class.
 * 
 * The {@link ActionDefinition} of this action description is a composite action definition of type
 * {@link IDurativeAction}. The corresponding {@link Precondition} is that the current segment must be less than the
 * horizon -- the common condition across all durative actions.
 * 
 * In the future, the constructor of this type may read an input formula for the "route segment" effect and create a
 * {@link RouteSegmentFormula} accordingly.
 * 
 * @author rsukkerd
 *
 */
public class RouteSegmentActionDescription implements IActionDescription<IDurativeAction> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private FormulaActionDescription<IDurativeAction> mSegmentActionDesc;

	public RouteSegmentActionDescription(ActionDefinition<IDurativeAction> durativeDef,
			Precondition<IDurativeAction> precondition, StateVarDefinition<RouteSegment> segmentDef,
			StateVarDefinition<TeamDestroyed> destroyedSrcDef) {
		// Discriminant class (i.e., discriminant variables)
		DiscriminantClass discrClass = new DiscriminantClass();
		discrClass.add(segmentDef);
		discrClass.add(destroyedSrcDef);

		// Effect class (i.e., effect variables)
		EffectClass effectClass = new EffectClass();
		effectClass.add(segmentDef);

		// Probabilistic transition formula of "route segment" effect class, of a durative action
		RouteSegmentFormula<IDurativeAction> segmentFormula = new RouteSegmentFormula<>(segmentDef);

		// Formula action description of "route segment" effect class, of durative actions
		mSegmentActionDesc = new FormulaActionDescription<>(durativeDef, precondition, discrClass, effectClass,
				segmentFormula);
	}

	@Override
	public Set<ProbabilisticTransition<IDurativeAction>> getProbabilisticTransitions(IDurativeAction action)
			throws XMDPException {
		return mSegmentActionDesc.getProbabilisticTransitions(action);
	}

	@Override
	public ProbabilisticEffect getProbabilisticEffect(Discriminant discriminant, IDurativeAction action)
			throws XMDPException {
		return mSegmentActionDesc.getProbabilisticEffect(discriminant, action);
	}

	@Override
	public ActionDefinition<IDurativeAction> getActionDefinition() {
		return mSegmentActionDesc.getActionDefinition();
	}

	@Override
	public DiscriminantClass getDiscriminantClass() {
		return mSegmentActionDesc.getDiscriminantClass();
	}

	@Override
	public EffectClass getEffectClass() {
		return mSegmentActionDesc.getEffectClass();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof RouteSegmentActionDescription)) {
			return false;
		}
		RouteSegmentActionDescription actionDesc = (RouteSegmentActionDescription) obj;
		return actionDesc.mSegmentActionDesc.equals(mSegmentActionDesc);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mSegmentActionDesc.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
