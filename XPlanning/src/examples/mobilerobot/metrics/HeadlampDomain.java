package examples.mobilerobot.metrics;

import examples.mobilerobot.models.HeadlampState;
import examples.mobilerobot.models.SetHeadlampAction;
import language.domain.metrics.Transition;
import language.domain.metrics.TransitionStructure;
import language.domain.models.ActionDefinition;
import language.domain.models.StateVarDefinition;
import language.exceptions.VarNotFoundException;

public class HeadlampDomain extends TransitionStructure<SetHeadlampAction> {
	
	private StateVarDefinition<HeadlampState> mrHeadlampDef;

	public HeadlampDomain(ActionDefinition<SetHeadlampAction> setHeadlampDef, StateVarDefinition<HeadlampState> rHeadlampDef) {
		this.mrHeadlampDef = rHeadlampDef;
		setActionDef(setHeadlampDef);
		addDestStateVarDef(rHeadlampDef);
	}
	
	public boolean getHealampOn(Transition<SetHeadlampAction, HeadlampDomain> transition) throws VarNotFoundException {
		HeadlampState on = transition.getDestStateVarValue(HeadlampState.class, mrHeadlampDef);
		return on.getValue();
	}
}
