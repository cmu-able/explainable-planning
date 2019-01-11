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

	public enum BOUND_TYPE {
		UPPER_BOUND, STRICT_UPPER_BOUND, LOWER_BOUND, STRICT_LOWER_BOUND
	}

	private E mQFunction;
	private BOUND_TYPE mBoundType;
	private double mBoundValue;
	private boolean mIsSoftConstraint;
	private IPenaltyFunction mPenaltyFunction;

	public AttributeConstraint(E qFunction, BOUND_TYPE boundType, double hardBoundValue) {
		mQFunction = qFunction;
		mBoundType = boundType;
		mBoundValue = hardBoundValue;
		mIsSoftConstraint = false;
	}

	public AttributeConstraint(E qFunction, BOUND_TYPE boundType, double softBoundValue,
			IPenaltyFunction penaltyFunction) {
		mQFunction = qFunction;
		mBoundType = boundType;
		mBoundValue = softBoundValue;
		mIsSoftConstraint = true;
		mPenaltyFunction = penaltyFunction;
	}

	public E getQFunction() {
		return mQFunction;
	}

	public BOUND_TYPE getBoundType() {
		return mBoundType;
	}

	public double getBoundValue() {
		return mBoundValue;
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
		return constraint.mQFunction.equals(mQFunction) && constraint.mBoundType == mBoundType
				&& Double.compare(constraint.mBoundValue, mBoundValue) == 0
				&& constraint.mIsSoftConstraint == mIsSoftConstraint && (constraint.mPenaltyFunction == mPenaltyFunction
						|| constraint.mPenaltyFunction != null && constraint.mPenaltyFunction.equals(mPenaltyFunction));
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mQFunction.hashCode();
			result = 31 * result + mBoundType.hashCode();
			result = 31 * result + Double.hashCode(mBoundValue);
			result = 31 * result + Boolean.hashCode(mIsSoftConstraint);
			result = 31 * result + (mPenaltyFunction == null ? 0 : mPenaltyFunction.hashCode());
			hashCode = result;
		}
		return result;
	}

}
