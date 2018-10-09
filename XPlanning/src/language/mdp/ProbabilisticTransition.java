package language.mdp;

import language.domain.models.IAction;

/**
 * {@link ProbabilisticTransition} represents a probabilistic transition Pr(s'|s,a).
 * 
 * @author rsukkerd
 *
 * @param <E>
 */
public class ProbabilisticTransition<E extends IAction> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private ProbabilisticEffect mProbEffect;
	private Discriminant mDiscriminant;
	private E mAction;

	public ProbabilisticTransition(ProbabilisticEffect probEffect, Discriminant discriminant, E action) {
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

	public E getAction() {
		return mAction;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof ProbabilisticTransition<?>)) {
			return false;
		}
		ProbabilisticTransition<?> probTrans = (ProbabilisticTransition<?>) obj;
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
