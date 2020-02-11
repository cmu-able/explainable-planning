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
 * {@link TeamDestroyedActionDescription} is the composite action description for the "teamDestroyed" effect class of an
 * instance of {@link IDurativeAction} action type. It uses a {@link FormulaActionDescription} that uses
 * {@link TeamDestroyedFormula}. The actions {@link IncAltAction}, {@link DecAltAction}, and {@link FlyAction} have the
 * same action description for the "teamDestroyed" effect class.
 * 
 * The {@link ActionDefinition} of this action description is a composite action definition of type
 * {@link IDurativeAction}. The corresponding {@link Precondition} is that the current segment must be less than the
 * horizon -- the common condition across all durative actions.
 * 
 * In the future, the constructor of this type may read an input formula for the "teamDestroyed" effect and create a
 * {@link TeamDestroyedFormula} accordingly.
 * 
 * @author rsukkerd
 *
 */
public class TeamDestroyedActionDescription implements IActionDescription<IDurativeAction> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private FormulaActionDescription<IDurativeAction> mDestroyedActionDesc;

	public TeamDestroyedActionDescription(ActionDefinition<IDurativeAction> durativeDef,
			Precondition<IDurativeAction> precondition, StateVarDefinition<TeamAltitude> altSrcDef,
			StateVarDefinition<TeamFormation> formSrcDef, StateVarDefinition<TeamECM> ecmSrcDef,
			StateVarDefinition<RouteSegment> segmentSrcDef, StateVarDefinition<TeamDestroyed> destroyedDef,
			double threatRange, double psi) {
		// Discriminant class (i.e., discriminant variables)
		DiscriminantClass discrClass = new DiscriminantClass();
		discrClass.add(altSrcDef);
		discrClass.add(formSrcDef);
		discrClass.add(ecmSrcDef);
		discrClass.add(segmentSrcDef);
		discrClass.add(destroyedDef);

		// Effect class (i.e., effect variables)
		EffectClass effectClass = new EffectClass();
		effectClass.add(destroyedDef);

		// Probabilistic transition formula of "teamDestroyed" effect class, of a durative action
		TeamDestroyedFormula<IDurativeAction> destroyedFormula = new TeamDestroyedFormula<>(altSrcDef, formSrcDef,
				ecmSrcDef, segmentSrcDef, destroyedDef, threatRange, psi);

		// Formula action description of "teamDestroyed" effect class, of durative actions
		mDestroyedActionDesc = new FormulaActionDescription<>(durativeDef, precondition, discrClass, effectClass,
				destroyedFormula);
	}

	@Override
	public Set<ProbabilisticTransition<IDurativeAction>> getProbabilisticTransitions(IDurativeAction action)
			throws XMDPException {
		return mDestroyedActionDesc.getProbabilisticTransitions(action);
	}

	@Override
	public ProbabilisticEffect getProbabilisticEffect(Discriminant discriminant, IDurativeAction action)
			throws XMDPException {
		return mDestroyedActionDesc.getProbabilisticEffect(discriminant, action);
	}

	@Override
	public ActionDefinition<IDurativeAction> getActionDefinition() {
		return mDestroyedActionDesc.getActionDefinition();
	}

	@Override
	public DiscriminantClass getDiscriminantClass() {
		return mDestroyedActionDesc.getDiscriminantClass();
	}

	@Override
	public EffectClass getEffectClass() {
		return mDestroyedActionDesc.getEffectClass();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof TeamDestroyedActionDescription)) {
			return false;
		}
		TeamDestroyedActionDescription actionDesc = (TeamDestroyedActionDescription) obj;
		return actionDesc.mDestroyedActionDesc.equals(mDestroyedActionDesc);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mDestroyedActionDesc.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
