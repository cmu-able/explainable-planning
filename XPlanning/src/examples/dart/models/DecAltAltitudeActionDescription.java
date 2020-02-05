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
 * {@link DecAltAltitudeActionDescription} is the action description for the "teamAltitude" effect class of an instance
 * of {@link DecAltAction} action type. It uses a {@link FormulaActionDescription} that uses
 * {@link DecAltAltitudeFormula}.
 * 
 * In the future, the constructor of this type may read an input formula for the "teamAltitude" effect of DecAlt and
 * create a {@link DecAltAltitudeFormula} accordingly.
 * 
 * @author rsukkerd
 *
 */
public class DecAltAltitudeActionDescription implements IActionDescription<DecAltAction> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private FormulaActionDescription<DecAltAction> mDecAltAltitudeActionDesc;

	public DecAltAltitudeActionDescription(ActionDefinition<DecAltAction> decAltDef,
			Precondition<DecAltAction> precondition, StateVarDefinition<TeamAltitude> teamAltDef,
			StateVarDefinition<TeamDestroyed> destroyedSrcDef) {
		// Discriminant class (i.e., discriminant variables)
		DiscriminantClass discrClass = new DiscriminantClass();
		discrClass.add(teamAltDef);
		discrClass.add(destroyedSrcDef);

		// Effect class (i.e., effect variables)
		EffectClass effectClass = new EffectClass();
		effectClass.add(teamAltDef);

		// Probabilistic transition formula of "teamAltitude" effect class, of DecAlt action
		DecAltAltitudeFormula decAltAltFormula = new DecAltAltitudeFormula(teamAltDef);

		// Formula action description of "teamAltitude" effect class, of DecAlt actions
		mDecAltAltitudeActionDesc = new FormulaActionDescription<>(decAltDef, precondition, discrClass, effectClass,
				decAltAltFormula);
	}

	@Override
	public Set<ProbabilisticTransition<DecAltAction>> getProbabilisticTransitions(DecAltAction action)
			throws XMDPException {
		return mDecAltAltitudeActionDesc.getProbabilisticTransitions(action);
	}

	@Override
	public ProbabilisticEffect getProbabilisticEffect(Discriminant discriminant, DecAltAction action)
			throws XMDPException {
		return mDecAltAltitudeActionDesc.getProbabilisticEffect(discriminant, action);
	}

	@Override
	public ActionDefinition<DecAltAction> getActionDefinition() {
		return mDecAltAltitudeActionDesc.getActionDefinition();
	}

	@Override
	public DiscriminantClass getDiscriminantClass() {
		return mDecAltAltitudeActionDesc.getDiscriminantClass();
	}

	@Override
	public EffectClass getEffectClass() {
		return mDecAltAltitudeActionDesc.getEffectClass();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof DecAltAltitudeActionDescription)) {
			return false;
		}
		DecAltAltitudeActionDescription actionDesc = (DecAltAltitudeActionDescription) obj;
		return actionDesc.mDecAltAltitudeActionDesc.equals(mDecAltAltitudeActionDesc);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mDecAltAltitudeActionDesc.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
