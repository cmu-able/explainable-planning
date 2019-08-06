package examples.mobilerobot.metrics;

import examples.mobilerobot.models.Area;
import examples.mobilerobot.models.MoveToAction;
import language.domain.metrics.IEvent;
import language.domain.metrics.Transition;
import language.exceptions.AttributeNameNotFoundException;
import language.exceptions.VarNotFoundException;

public class IntrusiveMoveEvent implements IEvent<MoveToAction, IntrusivenessDomain> {

	public static final String NAME = "intrusiveness";

	public static final String NON_INTRUSIVE_EVENT_NAME = "non-intrusive";
	public static final String SOMEWHAT_INTRUSIVE_EVENT_NAME = "somewhat-intrusive";
	public static final String VERY_INTRUSIVE_EVENT_NAME = "very-intrusive";

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private String mName;
	private IntrusivenessDomain mDomain;
	private Area mArea;

	public IntrusiveMoveEvent(String name, IntrusivenessDomain domain, Area area) {
		mName = name;
		mDomain = domain;
		mArea = area;
	}

	public Area getArea() {
		return mArea;
	}

	@Override
	public String getName() {
		return mName;
	}

	@Override
	public IntrusivenessDomain getTransitionStructure() {
		return mDomain;
	}

	@Override
	public double getEventProbability(Transition<MoveToAction, IntrusivenessDomain> transition)
			throws VarNotFoundException, AttributeNameNotFoundException {
		Area destArea = mDomain.getArea(transition);
		return destArea == getArea() ? 1 : 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof IntrusiveMoveEvent)) {
			return false;
		}
		IntrusiveMoveEvent event = (IntrusiveMoveEvent) obj;
		return event.mName.equals(mName) && event.mDomain.equals(mDomain) && event.mArea.equals(mArea);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mName.hashCode();
			result = 31 * result + mDomain.hashCode();
			result = 31 * result + mArea.hashCode();
			hashCode = result;
		}
		return result;
	}

}
