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
 * {@link TeamECMActionDescription} is the action description for the "teamECM" effect class of an instance of
 * {@link SwitchECMAction} action type. It uses a {@link FormulaActionDescription} that uses {@link TeamECMFormula}.
 * 
 * In the future, the constructor of this type may read an input formula for the "teamECM" effect of SwitchECM and
 * create a {@link TeamECMFormula} accordingly.
 * 
 * @author rsukkerd
 *
 */
public class TeamECMActionDescription implements IActionDescription<SwitchECMAction> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private FormulaActionDescription<SwitchECMAction> mTeamECMActionDesc;

	public TeamECMActionDescription(ActionDefinition<SwitchECMAction> switchECMDef,
			Precondition<SwitchECMAction> precondition, StateVarDefinition<TeamECM> teamECMDef,
			StateVarDefinition<TeamDestroyed> destroyedSrcDef) {
		// Discriminant class (i.e., discriminant variables)
		DiscriminantClass discrClass = new DiscriminantClass();
		discrClass.add(teamECMDef);
		discrClass.add(destroyedSrcDef);

		// Effect class (i.e., effect variables)
		EffectClass effectClass = new EffectClass();
		effectClass.add(teamECMDef);

		// Probabilistic transition formula of "teamECM" effect class, of SwitchECM action
		TeamECMFormula teamECMFormula = new TeamECMFormula(teamECMDef);

		// Formula action description of "teamECM" effect class, of SwitchECM actions
		mTeamECMActionDesc = new FormulaActionDescription<>(switchECMDef, precondition, discrClass, effectClass,
				teamECMFormula);
	}

	@Override
	public Set<ProbabilisticTransition<SwitchECMAction>> getProbabilisticTransitions(SwitchECMAction action)
			throws XMDPException {
		return mTeamECMActionDesc.getProbabilisticTransitions(action);
	}

	@Override
	public ProbabilisticEffect getProbabilisticEffect(Discriminant discriminant, SwitchECMAction action)
			throws XMDPException {
		return mTeamECMActionDesc.getProbabilisticEffect(discriminant, action);
	}

	@Override
	public ActionDefinition<SwitchECMAction> getActionDefinition() {
		return mTeamECMActionDesc.getActionDefinition();
	}

	@Override
	public DiscriminantClass getDiscriminantClass() {
		return mTeamECMActionDesc.getDiscriminantClass();
	}

	@Override
	public EffectClass getEffectClass() {
		return mTeamECMActionDesc.getEffectClass();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof TeamECMActionDescription)) {
			return false;
		}
		TeamECMActionDescription actionDesc = (TeamECMActionDescription) obj;
		return actionDesc.mTeamECMActionDesc.equals(mTeamECMActionDesc);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mTeamECMActionDesc.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
