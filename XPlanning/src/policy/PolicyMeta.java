package policy;

import java.util.Objects;

import exceptions.NoSolutionException;

public class PolicyMeta {

	public enum SOLUTION_CODE {
		OK, CONSTRAINT_NOT_SATISFIED, GOAL_NOT_REACHED
	}

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private SOLUTION_CODE mSolutionCode;
	private Policy mPolicy;

	public PolicyMeta(SOLUTION_CODE solutionCode, Policy policy) {
		if (solutionCode == SOLUTION_CODE.OK && policy == null) {
			throw new IllegalArgumentException("Solution found: Policy must be non-null");
		}
		if (solutionCode != SOLUTION_CODE.OK && policy != null) {
			throw new IllegalArgumentException("Solution not found: Policy must be null");
		}
		mSolutionCode = solutionCode;
		mPolicy = policy;
	}

	public PolicyMeta(Policy policy) {
		if (policy == null) {
			throw new IllegalArgumentException("Policy must be non-null");
		}
		mPolicy = policy;
	}

	public SOLUTION_CODE getSolutionCode() {
		return mSolutionCode;
	}

	public boolean hasPolicy() {
		return mSolutionCode == SOLUTION_CODE.OK;
	}

	public Policy getPolicy() throws NoSolutionException {
		if (!hasPolicy()) {
			throw new NoSolutionException(mSolutionCode);
		}
		return mPolicy;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof PolicyMeta)) {
			return false;
		}
		PolicyMeta policyMeta = (PolicyMeta) obj;
		return Objects.equals(policyMeta.mSolutionCode, mSolutionCode) && Objects.equals(policyMeta.mPolicy, mPolicy);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + (mSolutionCode != null ? mSolutionCode.hashCode() : 0);
			result = 31 * result + (mPolicy != null ? mPolicy.hashCode() : 0);
			hashCode = result;
		}
		return hashCode;
	}

}
