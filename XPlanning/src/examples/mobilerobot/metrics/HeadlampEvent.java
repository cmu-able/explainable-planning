package examples.mobilerobot.metrics;

import examples.mobilerobot.models.Location;
import examples.mobilerobot.models.SetHeadlampAction;
import language.domain.metrics.IEvent;
import language.domain.metrics.Transition;
import language.exceptions.AttributeNameNotFoundException;
import language.exceptions.VarNotFoundException;

public class HeadlampEvent implements IEvent<SetHeadlampAction, HeadlampDomain>{

	public static final String NAME = "headlampChange";
	private String mName;
	private HeadlampDomain mDomain;
	private boolean mOn;

	public HeadlampEvent(String name, HeadlampDomain domain, boolean on) {
		mName = name;
		mDomain = domain;
		this.mOn = on;
	}

	@Override
	public String getName() {
		return mName;
	}

	@Override
	public HeadlampDomain getTransitionStructure() {
		return mDomain;
	}

	@Override
	public double getEventProbability(Transition<SetHeadlampAction, HeadlampDomain> transition)
			throws VarNotFoundException, AttributeNameNotFoundException {
		boolean headlamp = mDomain.getHealampOn(transition);
		return headlamp == mOn ? 1 : 0;
	}
	
}
