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
 * {@link TeamFormationActionDescription} is the action description for the "teamFormation" effect class of an instance
 * of {@link ChangeFormAction} action type. It uses a {@link FormulaActionDescription} that uses
 * {@link TeamFormationFormula}.
 * 
 * In the future, the constructor of this type may read an input formula for the "teamFormation" effect of ChangeForm
 * and create a {@link TeamFormationFormula} accordingly.
 * 
 * @author rsukkerd
 *
 */
public class TeamFormationActionDescription implements IActionDescription<ChangeFormAction> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private FormulaActionDescription<ChangeFormAction> mTeamFormActionDesc;

	public TeamFormationActionDescription(ActionDefinition<ChangeFormAction> changeFormDef,
			Precondition<ChangeFormAction> precondition, StateVarDefinition<TeamFormation> teamFormDef,
			StateVarDefinition<TeamDestroyed> destroyedSrcDef) {
		// Discriminant class (i.e., discriminant variables)
		DiscriminantClass discrClass = new DiscriminantClass();
		discrClass.add(teamFormDef);
		discrClass.add(destroyedSrcDef);

		// Effect class (i.e., effect variables)
		EffectClass effectClass = new EffectClass();
		effectClass.add(teamFormDef);

		// Probabilistic transition formula of "teamFormation" effect class, of ChangeForm action
		TeamFormationFormula teamFormFormula = new TeamFormationFormula(teamFormDef);

		// Formula action description of "teamFormation" effect class, of ChangeForm actions
		mTeamFormActionDesc = new FormulaActionDescription<>(changeFormDef, precondition, discrClass, effectClass,
				teamFormFormula);
	}

	@Override
	public Set<ProbabilisticTransition<ChangeFormAction>> getProbabilisticTransitions(ChangeFormAction action)
			throws XMDPException {
		return mTeamFormActionDesc.getProbabilisticTransitions(action);
	}

	@Override
	public ProbabilisticEffect getProbabilisticEffect(Discriminant discriminant, ChangeFormAction action)
			throws XMDPException {
		return mTeamFormActionDesc.getProbabilisticEffect(discriminant, action);
	}

	@Override
	public ActionDefinition<ChangeFormAction> getActionDefinition() {
		return mTeamFormActionDesc.getActionDefinition();
	}

	@Override
	public DiscriminantClass getDiscriminantClass() {
		return mTeamFormActionDesc.getDiscriminantClass();
	}

	@Override
	public EffectClass getEffectClass() {
		return mTeamFormActionDesc.getEffectClass();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof TeamFormationActionDescription)) {
			return false;
		}
		TeamFormationActionDescription actionDesc = (TeamFormationActionDescription) obj;
		return actionDesc.mTeamFormActionDesc.equals(mTeamFormActionDesc);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mTeamFormActionDesc.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
