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
 * {@link IncAltDestroyedActionDescription} is the action description for the "teamDestroyed" effect class of an
 * instance of {@link IncAltAction} action type. It uses a {@link FormulaActionDescription} that uses
 * {@link TeamDestroyedFormula}.
 *
 * In the future, the constructor of this type may read an input formula for the "teamDestroyed" effect and create a
 * {@link TeamDestroyedFormula} accordingly.
 * 
 * Note: All actions of type {@link IDurativeAction} have the same structure of action description for the
 * "teamDestroyed" effect class.
 * 
 * @author rsukkerd
 *
 */
public class IncAltDestroyedActionDescription implements IActionDescription<IncAltAction> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private FormulaActionDescription<IncAltAction> mIncAltDestroyedActionDesc;

	public IncAltDestroyedActionDescription(ActionDefinition<IncAltAction> incAltDef,
			Precondition<IncAltAction> precondition, StateVarDefinition<TeamAltitude> altSrcDef,
			StateVarDefinition<TeamFormation> formSrcDef, StateVarDefinition<TeamECM> ecmSrcDef,
			StateVarDefinition<RouteSegment> segmentSrcDef, StateVarDefinition<TeamDestroyed> destroyedDestDef) {
		// Discriminant class (i.e., discriminant variables)
		DiscriminantClass discrClass = new DiscriminantClass();
		discrClass.add(altSrcDef);
		discrClass.add(formSrcDef);
		discrClass.add(ecmSrcDef);
		discrClass.add(segmentSrcDef);

		// Effect class (i.e., effect variables)
		EffectClass effectClass = new EffectClass();
		effectClass.add(destroyedDestDef);

		// Probabilistic transition formula of "teamDestroyed" effect class, of a durative action
		TeamDestroyedFormula<IncAltAction> destroyedFormula = new TeamDestroyedFormula<>(altSrcDef, formSrcDef,
				ecmSrcDef, segmentSrcDef, destroyedDestDef);

		// Formular action description of "teamDestroyed" effect class, of IncAlt actions
		mIncAltDestroyedActionDesc = new FormulaActionDescription<>(incAltDef, precondition, discrClass, effectClass,
				destroyedFormula);
	}

	@Override
	public Set<ProbabilisticTransition<IncAltAction>> getProbabilisticTransitions(IncAltAction action)
			throws XMDPException {
		return mIncAltDestroyedActionDesc.getProbabilisticTransitions(action);
	}

	@Override
	public ProbabilisticEffect getProbabilisticEffect(Discriminant discriminant, IncAltAction action)
			throws XMDPException {
		return mIncAltDestroyedActionDesc.getProbabilisticEffect(discriminant, action);
	}

	@Override
	public ActionDefinition<IncAltAction> getActionDefinition() {
		return mIncAltDestroyedActionDesc.getActionDefinition();
	}

	@Override
	public DiscriminantClass getDiscriminantClass() {
		return mIncAltDestroyedActionDesc.getDiscriminantClass();
	}

	@Override
	public EffectClass getEffectClass() {
		return mIncAltDestroyedActionDesc.getEffectClass();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof IncAltDestroyedActionDescription)) {
			return false;
		}
		IncAltDestroyedActionDescription actionDesc = (IncAltDestroyedActionDescription) obj;
		return actionDesc.mIncAltDestroyedActionDesc.equals(mIncAltDestroyedActionDesc);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mIncAltDestroyedActionDesc.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
