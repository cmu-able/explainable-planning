package solver.common;

import language.domain.metrics.IQFunction;
import language.objectives.AttributeConstraint;
import language.objectives.AttributeConstraint.BOUND_TYPE;
import language.objectives.IPenaltyFunction;

public class NonStrictConstraint {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private AttributeConstraint<? extends IQFunction<?, ?>> mAttrConstraint;
	private double mToleranceFactor;

	// Derived fields
	private BOUND_TYPE mBoundType;
	private double mBoundValue;

	public NonStrictConstraint(AttributeConstraint<? extends IQFunction<?, ?>> attrConstraint, double toleranceFactor) {
		mAttrConstraint = attrConstraint;
		mToleranceFactor = toleranceFactor;

		// Convert any strict bound to non-strict bound
		if (attrConstraint.getBoundType() == BOUND_TYPE.STRICT_UPPER_BOUND) {
			mBoundType = BOUND_TYPE.UPPER_BOUND;
			mBoundValue = toleranceFactor * attrConstraint.getBoundValue();
		} else if (attrConstraint.getBoundType() == BOUND_TYPE.STRICT_LOWER_BOUND) {
			mBoundType = BOUND_TYPE.LOWER_BOUND;
			mBoundValue = (1 + 1 - toleranceFactor) * attrConstraint.getBoundValue();
		} else {
			mBoundType = attrConstraint.getBoundType();
			mBoundValue = attrConstraint.getBoundValue();
		}
	}

	public BOUND_TYPE getBoundType() {
		return mBoundType;
	}

	public double getBoundValue() {
		return mBoundValue;
	}

	public boolean isSoftConstraint() {
		return mAttrConstraint.isSoftConstraint();
	}

	public IPenaltyFunction getPenaltyFunction() {
		return mAttrConstraint.getPenaltyFunction();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof NonStrictConstraint)) {
			return false;
		}
		NonStrictConstraint constraint = (NonStrictConstraint) obj;
		return constraint.mAttrConstraint.equals(mAttrConstraint)
				&& Double.compare(constraint.mToleranceFactor, mToleranceFactor) == 0;
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mAttrConstraint.hashCode();
			result = 31 * result + Double.hashCode(mToleranceFactor);
			hashCode = result;
		}
		return result;
	}
}
