package examples.mobilerobot.metrics;

import examples.mobilerobot.factors.Area;
import examples.mobilerobot.factors.MoveToAction;
import exceptions.AttributeNameNotFoundException;
import exceptions.VarNotFoundException;
import metrics.IEvent;
import metrics.Transition;

public class IntrusiveMoveEvent implements IEvent<MoveToAction, IntrusivenessDomain> {

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
	public IntrusivenessDomain getQFunctionDomain() {
		return mDomain;
	}

	@Override
	public boolean hasEventOccurred(Transition<MoveToAction, IntrusivenessDomain> transition)
			throws VarNotFoundException, AttributeNameNotFoundException {
		return mDomain.getArea(transition).equals(getArea());
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
