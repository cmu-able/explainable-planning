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
 * {@link IncAltAltitudeActionDescription} is the action description for the "teamAltitude" effect class of an instance
 * of {@link IncAltAction} action type. It uses a {@link FormulaActionDescription} that uses
 * {@link IncAltAltitudeFormula}.
 * 
 * In the future, the constructor of this type may read an input formula for the "teamAltitude" effect of IncAlt and
 * create a {@link IncAltAltitudeFormula} accordingly.
 * 
 * @author rsukkerd
 *
 */
public class IncAltAltitudeActionDescription implements IActionDescription<IncAltAction> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private FormulaActionDescription<IncAltAction> mIncAltAltitudeActionDesc;

	public IncAltAltitudeActionDescription(ActionDefinition<IncAltAction> incAltDef,
			Precondition<IncAltAction> precondition, StateVarDefinition<TeamAltitude> teamAltDef,
			StateVarDefinition<TeamDestroyed> destroyedSrcDef) {
		// Discriminant class (i.e., discriminant variables)
		DiscriminantClass discrClass = new DiscriminantClass();
		discrClass.add(teamAltDef);
		discrClass.add(destroyedSrcDef);

		// Effect class (i.e., effect variables)
		EffectClass effectClass = new EffectClass();
		effectClass.add(teamAltDef);

		// Probabilistic transition formula of "teamAltitude" effect class, of IncAlt action
		IncAltAltitudeFormula incAltAltFormula = new IncAltAltitudeFormula(teamAltDef);

		// Formula action description of "teamAltitude" effect class, of IncAlt actions
		mIncAltAltitudeActionDesc = new FormulaActionDescription<>(incAltDef, precondition, discrClass, effectClass,
				incAltAltFormula);
	}

	@Override
	public Set<ProbabilisticTransition<IncAltAction>> getProbabilisticTransitions(IncAltAction action)
			throws XMDPException {
		return mIncAltAltitudeActionDesc.getProbabilisticTransitions(action);
	}

	@Override
	public ProbabilisticEffect getProbabilisticEffect(Discriminant discriminant, IncAltAction action)
			throws XMDPException {
		return mIncAltAltitudeActionDesc.getProbabilisticEffect(discriminant, action);
	}

	@Override
	public ActionDefinition<IncAltAction> getActionDefinition() {
		return mIncAltAltitudeActionDesc.getActionDefinition();
	}

	@Override
	public DiscriminantClass getDiscriminantClass() {
		return mIncAltAltitudeActionDesc.getDiscriminantClass();
	}

	@Override
	public EffectClass getEffectClass() {
		return mIncAltAltitudeActionDesc.getEffectClass();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof IncAltAltitudeActionDescription)) {
			return false;
		}
		IncAltAltitudeActionDescription actionDesc = (IncAltAltitudeActionDescription) obj;
		return actionDesc.mIncAltAltitudeActionDesc.equals(mIncAltAltitudeActionDesc);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mIncAltAltitudeActionDesc.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
