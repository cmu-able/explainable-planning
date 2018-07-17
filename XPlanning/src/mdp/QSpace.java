package mdp;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import factors.IAction;
import metrics.IQFunction;
import metrics.IQFunctionDomain;

public class QSpace implements Iterable<IQFunction<IAction, IQFunctionDomain<IAction>>> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Set<IQFunction<? extends IAction, ? extends IQFunctionDomain<? extends IAction>>> mQFunctions = new HashSet<>();

	public QSpace() {
		// mQFunctions initially empty
	}

	public <E extends IAction, T extends IQFunctionDomain<E>> void addQFunction(IQFunction<E, T> qFunction) {
		mQFunctions.add(qFunction);
	}

	public <E extends IAction, T extends IQFunctionDomain<E>> boolean contains(IQFunction<E, T> qFunction) {
		return mQFunctions.contains(qFunction);
	}

	@Override
	public Iterator<IQFunction<IAction, IQFunctionDomain<IAction>>> iterator() {
		return new Iterator<IQFunction<IAction, IQFunctionDomain<IAction>>>() {

			private Iterator<IQFunction<? extends IAction, ? extends IQFunctionDomain<? extends IAction>>> iter = mQFunctions
					.iterator();

			@Override
			public boolean hasNext() {
				return iter.hasNext();
			}

			@Override
			public IQFunction<IAction, IQFunctionDomain<IAction>> next() {
				return (IQFunction<IAction, IQFunctionDomain<IAction>>) iter.next();
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
