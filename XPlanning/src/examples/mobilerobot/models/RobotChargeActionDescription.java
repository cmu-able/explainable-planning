package examples.mobilerobot.models;

import language.domain.models.ActionDefinition;
import language.domain.models.StateVarDefinition;
import language.mdp.DiscriminantClass;
import language.mdp.FormulaActionDescription;
import language.mdp.Precondition;

public class RobotChargeActionDescription extends FormulaActionDescription<ChargeAction> {
	
	public RobotChargeActionDescription(ActionDefinition<ChargeAction> actionDefinition,
			Precondition<ChargeAction> precondition, StateVarDefinition<Location> rLocDef) {
		super(actionDefinition, precondition, null, null, null);
		DiscriminantClass discrClass = new DiscriminantClass();
		discrClass.add(rLocDef);
	}

}
