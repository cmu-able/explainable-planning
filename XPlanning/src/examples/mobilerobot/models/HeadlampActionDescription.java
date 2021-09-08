package examples.mobilerobot.models;

import language.domain.models.ActionDefinition;
import language.domain.models.IProbabilisticTransitionFormula;
import language.domain.models.StateVarDefinition;
import language.mdp.DiscriminantClass;
import language.mdp.EffectClass;
import language.mdp.FormulaActionDescription;
import language.mdp.Precondition;

public class HeadlampActionDescription extends FormulaActionDescription<SetHeadlampAction> {

	public HeadlampActionDescription(ActionDefinition<SetHeadlampAction> setHeadlampDef,
			Precondition<SetHeadlampAction> precondition, StateVarDefinition<HeadlampState> rHeadlampDef) {
		super(setHeadlampDef, precondition, 
				new DiscriminantClass(rHeadlampDef), new EffectClass(rHeadlampDef), 
				new HeadlampStateFormula(rHeadlampDef));
	}

}
