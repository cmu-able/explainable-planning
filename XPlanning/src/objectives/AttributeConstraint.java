package objectives;

import metrics.IQFunction;

/**
 * {@link AttributeConstraint} represents a constraint on the expected total value of a particular QA of a policy.
 * 
 * @author rsukkerd
 *
 * @param <E>
 */
public class AttributeConstraint<E extends IQFunction> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private E mQFunction;
	private double mExpTotalUpperBound;

	public AttributeConstraint(E qFunction, double expTotalUpperBound) {
		mQFunction = qFunction;
		mExpTotalUpperBound = expTotalUpperBound;
	}

	public E getQFunction() {
		return mQFunction;
	}

	public double getExpectedTotalUpperBound() {
		return mExpTotalUpperBound;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof AttributeConstraint<?>)) {
			return false;
		}
		AttributeConstraint<?> constraint = (AttributeConstraint<?>) obj;
		return constraint.mQFunction.equals(mQFunction)
				&& Double.compare(constraint.mExpTotalUpperBound, mExpTotalUpperBound) == 0;
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mQFunction.hashCode();
			result = 31 * result + Double.hashCode(mExpTotalUpperBound);
			hashCode = result;
		}
		return result;
	}

}
