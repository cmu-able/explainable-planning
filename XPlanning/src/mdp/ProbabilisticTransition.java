package mdp;

import factors.IAction;

/**
 * {@link ProbabilisticTransition} represents a probabilistic transition Pr(s'|s,a).
 * 
 * @author rsukkerd
 *
 */
public class ProbabilisticTransition {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private ProbabilisticEffect mProbEffect;
	private Discriminant mDiscriminant;
	private IAction mAction;

	public ProbabilisticTransition(ProbabilisticEffect probEffect, Discriminant discriminant, IAction action) {
		mProbEffect = probEffect;
		mDiscriminant = discriminant;
		mAction = action;
	}

	public ProbabilisticEffect getProbabilisticEffect() {
		return mProbEffect;
	}

	public Discriminant getDiscriminant() {
		return mDiscriminant;
	}

	public IAction getAction() {
		return mAction;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof ProbabilisticTransition)) {
			return false;
		}
		ProbabilisticTransition probTrans = (ProbabilisticTransition) obj;
		return probTrans.mProbEffect.equals(mProbEffect) && probTrans.mDiscriminant.equals(mDiscriminant)
				&& probTrans.mAction.equals(mAction);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mProbEffect.hashCode();
			result = 31 * result + mDiscriminant.hashCode();
			result = 31 * result + mAction.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
