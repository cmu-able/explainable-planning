package examples.mobilerobot.models;

import language.domain.models.Action;
import language.domain.models.StateVar;

public class SetHeadlampAction extends Action {

	private StateVar<HeadlampState> mrHeadlampState;

	public SetHeadlampAction(StateVar<HeadlampState> rHeadlampState) {
		super("setHeadlamp", rHeadlampState.getValue());
		mrHeadlampState = rHeadlampState;
	}
	
	public HeadlampState getValue() {
		return mrHeadlampState.getValue();
	}

}
