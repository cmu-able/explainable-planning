package language.objectives;

import language.domain.metrics.IQFunction;

/**
 * {@link AttributeConstraint} represents a constraint on the expected total (or average) value of a particular QA of a
 * policy. The constraint can be either hard or soft, if it is the latter, a penalty function must be provided.
 * 
 * @author rsukkerd
 *
 * @param <E>
 */
public class AttributeConstraint<E extends IQFunction<?, ?>> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private E mQFunction;
	private double mUpperBound;
	private boolean mIsStrictBound;
	private boolean mIsSoftConstraint;
	private IPenaltyFunction mPenaltyFunction;

	public AttributeConstraint(E qFunction, double hardUpperBound, boolean isStrictBound) {
		mQFunction = qFunction;
		mUpperBound = hardUpperBound;
		mIsStrictBound = isStrictBound;
		mIsSoftConstraint = false;
	}

	public AttributeConstraint(E qFunction, double softUpperBound, IPenaltyFunction penaltyFunction) {
		mQFunction = qFunction;
		mUpperBound = softUpperBound;
		mIsSoftConstraint = true;
		mPenaltyFunction = penaltyFunction;
	}

	public E getQFunction() {
		return mQFunction;
	}

	public double getUpperBound() {
		return mUpperBound;
	}

	public boolean isStrictBound() {
		return mIsStrictBound;
	}

	public boolean isSoftConstraint() {
		return mIsSoftConstraint;
	}

	public IPenaltyFunction getPenaltyFunction() {
		if (!mIsSoftConstraint) {
			throw new IllegalStateException("Hard constraint does not have a penalty function");
		}
		return mPenaltyFunction;
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
		return constraint.mQFunction.equals(mQFunction) && Double.compare(constraint.mUpperBound, mUpperBound) == 0
				&& constraint.mIsStrictBound == mIsStrictBound && constraint.mIsSoftConstraint == mIsSoftConstraint
				&& (constraint.mPenaltyFunction == mPenaltyFunction
						|| constraint.mPenaltyFunction != null && constraint.mPenaltyFunction.equals(mPenaltyFunction));
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mQFunction.hashCode();
			result = 31 * result + Double.hashCode(mUpperBound);
			result = 31 * result + Boolean.hashCode(mIsStrictBound);
			result = 31 * result + Boolean.hashCode(mIsSoftConstraint);
			result = 31 * result + (mPenaltyFunction == null ? 0 : mPenaltyFunction.hashCode());
			hashCode = result;
		}
		return result;
	}

}
