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
 * {@link FlyDestroyedActionDescription} is the action description for the "teamDestroyed" effect class of an instance
 * of {@link FlyAction} action type. It uses a {@link FormulaActionDescription} that uses {@link TeamDestroyedFormula}.
 * 
 * In the future, the constructor of this type may read an input formula for the "teamDestroyed" effect and create a
 * {@link TeamDestroyedFormula} accordingly.
 * 
 * Note: All actions of type {@link IDurativeAction} have the same structure of action description for the
 * "teamDestroyed" effect class. The only difference is the {@link ActionDefinition} and its {@link Precondition}.
 * 
 * @author rsukkerd
 *
 */
public class FlyDestroyedActionDescription implements IActionDescription<FlyAction> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private FormulaActionDescription<FlyAction> mFlyDestroyedActionDesc;

	public FlyDestroyedActionDescription(ActionDefinition<FlyAction> flyDef, Precondition<FlyAction> precondition,
			StateVarDefinition<TeamAltitude> altSrcDef, StateVarDefinition<TeamFormation> formSrcDef,
			StateVarDefinition<TeamECM> ecmSrcDef, StateVarDefinition<RouteSegment> segmentSrcDef,
			StateVarDefinition<TeamDestroyed> destroyedDestDef) {
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
		TeamDestroyedFormula<FlyAction> destroyedFormula = new TeamDestroyedFormula<>(altSrcDef, formSrcDef, ecmSrcDef,
				segmentSrcDef, destroyedDestDef);

		// Formula action description of "teamDestroyed" effect class, of Fly actions
		mFlyDestroyedActionDesc = new FormulaActionDescription<>(flyDef, precondition, discrClass, effectClass,
				destroyedFormula);
	}

	@Override
	public Set<ProbabilisticTransition<FlyAction>> getProbabilisticTransitions(FlyAction action) throws XMDPException {
		return mFlyDestroyedActionDesc.getProbabilisticTransitions(action);
	}

	@Override
	public ProbabilisticEffect getProbabilisticEffect(Discriminant discriminant, FlyAction action)
			throws XMDPException {
		return mFlyDestroyedActionDesc.getProbabilisticEffect(discriminant, action);
	}

	@Override
	public ActionDefinition<FlyAction> getActionDefinition() {
		return mFlyDestroyedActionDesc.getActionDefinition();
	}

	@Override
	public DiscriminantClass getDiscriminantClass() {
		return mFlyDestroyedActionDesc.getDiscriminantClass();
	}

	@Override
	public EffectClass getEffectClass() {
		return mFlyDestroyedActionDesc.getEffectClass();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof FlyDestroyedActionDescription)) {
			return false;
		}
		FlyDestroyedActionDescription actionDesc = (FlyDestroyedActionDescription) obj;
		return actionDesc.mFlyDestroyedActionDesc.equals(mFlyDestroyedActionDesc);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mFlyDestroyedActionDesc.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
