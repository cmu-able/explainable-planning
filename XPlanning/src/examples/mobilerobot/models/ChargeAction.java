package examples.mobilerobot.models;

import java.util.List;
import java.util.Set;

import language.domain.models.Action;
import language.domain.models.IAction;
import language.domain.models.IActionAttribute;
import language.domain.models.IStateVarValue;
import language.domain.models.StateVar;
import language.exceptions.AttributeNameNotFoundException;

public class ChargeAction extends Action {
	
	public ChargeAction(StateVar<Location> rLoc) {
		super("charge", rLoc.getValue());
	}

	

}
