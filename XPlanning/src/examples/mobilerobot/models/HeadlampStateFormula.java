package examples.mobilerobot.models;

import language.domain.models.IProbabilisticTransitionFormula;
import language.domain.models.StateVar;
import language.domain.models.StateVarDefinition;
import language.exceptions.XMDPException;
import language.mdp.Discriminant;
import language.mdp.Effect;
import language.mdp.EffectClass;
import language.mdp.ProbabilisticEffect;

public class HeadlampStateFormula implements IProbabilisticTransitionFormula<SetHeadlampAction> {
	
	private StateVarDefinition<HeadlampState> mrHeadlampDef;
	private EffectClass mEffectClass;

	public HeadlampStateFormula (StateVarDefinition<HeadlampState> rHeadlampDef) {
		this.mrHeadlampDef = rHeadlampDef;
		this.mEffectClass = new EffectClass(rHeadlampDef);
	}

	@Override
	public ProbabilisticEffect formula(Discriminant discriminant, SetHeadlampAction action) throws XMDPException {
		ProbabilisticEffect pe = new ProbabilisticEffect(mEffectClass);
		Effect newHeadlampEffect = new Effect(mEffectClass);
		StateVar<HeadlampState> rHeadhlamp = mrHeadlampDef.getStateVar(action.getValue());
		newHeadlampEffect.add(rHeadhlamp);
		pe.put(newHeadlampEffect, 1.0);
		return pe;
	}
	
	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof HeadlampStateFormula)) {
			return false;
		}
		HeadlampStateFormula formula = (HeadlampStateFormula) obj;
		return formula.mrHeadlampDef.equals(mrHeadlampDef);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mrHeadlampDef.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
