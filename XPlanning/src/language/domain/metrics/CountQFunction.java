package language.domain.metrics;

import language.domain.models.IAction;
import language.exceptions.AttributeNameNotFoundException;
import language.exceptions.VarNotFoundException;

/**
 * {@link CountQFunction} is a Q_i function that characterizes a QA i by the number of occurrences of a particular type
 * of event.
 * 
 * @author rsukkerd
 *
 * @param <E>
 * @param <T>
 * @param <S>
 *            : Type of event
 */
public class CountQFunction<E extends IAction, T extends ITransitionStructure<E>, S extends IEvent<E, T>>
		implements IQFunction<E, T> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private S mEvent;

	public CountQFunction(S event) {
		mEvent = event;
	}

	public S getEvent() {
		return mEvent;
	}

	@Override
	public String getName() {
		return mEvent.getName();
	}

	@Override
	public T getTransitionStructure() {
		return mEvent.getTransitionStructure();
	}

	@Override
	public double getValue(Transition<E, T> transition) throws VarNotFoundException, AttributeNameNotFoundException {
		return mEvent.getEventProbability(transition);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof CountQFunction<?, ?, ?>)) {
			return false;
		}
		CountQFunction<?, ?, ?> qFunction = (CountQFunction<?, ?, ?>) obj;
		return qFunction.mEvent.equals(mEvent);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mEvent.hashCode();
			hashCode = result;
		}
		return result;
	}

}
