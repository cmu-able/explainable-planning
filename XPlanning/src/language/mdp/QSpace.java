package language.mdp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import language.domain.metrics.IQFunction;
import language.domain.metrics.ITransitionStructure;
import language.domain.models.IAction;

/**
 * {@link QSpace} is a set of {@link IQFunction}s of an MDP.
 * 
 * @author rsukkerd
 *
 */
public class QSpace implements Iterable<IQFunction<IAction, ITransitionStructure<IAction>>> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Map<String, IQFunction<?, ?>> mQFunctions = new HashMap<>();

	public QSpace() {
		// mQFunctions initially empty
	}

	public void addQFunction(IQFunction<?, ?> qFunction) {
		mQFunctions.put(qFunction.getName(), qFunction);
	}

	public boolean contains(IQFunction<?, ?> qFunction) {
		return mQFunctions.containsValue(qFunction);
	}

	public <E extends IQFunction<?, ?>> E getQFunction(Class<E> qFunctionType, String name) {
		IQFunction<?, ?> qFunction = mQFunctions.get(name);
		return qFunctionType.cast(qFunction);
	}

	public int getNumQFunctions() {
		return mQFunctions.size();
	}

	@Override
	public Iterator<IQFunction<IAction, ITransitionStructure<IAction>>> iterator() {
		return new Iterator<IQFunction<IAction, ITransitionStructure<IAction>>>() {

			private Iterator<IQFunction<?, ?>> iter = mQFunctions.values().iterator();

			@Override
			public boolean hasNext() {
				return iter.hasNext();
			}

			@Override
			public IQFunction<IAction, ITransitionStructure<IAction>> next() {
				return (IQFunction<IAction, ITransitionStructure<IAction>>) iter.next();
			}

			@Override
			public void remove() {
				iter.remove();
			}
		};
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof QSpace)) {
			return false;
		}
		QSpace qSpace = (QSpace) obj;
		return qSpace.mQFunctions.equals(mQFunctions);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mQFunctions.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
