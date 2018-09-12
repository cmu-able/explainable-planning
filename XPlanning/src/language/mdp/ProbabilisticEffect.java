package language.mdp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import language.exceptions.EffectNotFoundException;
import language.exceptions.IncompatibleEffectClassException;

/**
 * {@link ProbabilisticEffect} is a distribution over the changed state variables as a result of an action.
 * 
 * @author rsukkerd
 *
 */
public class ProbabilisticEffect implements Iterable<Entry<Effect, Double>> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Map<Effect, Double> mProbEffect = new HashMap<>();
	private EffectClass mEffectClass;

	public ProbabilisticEffect(EffectClass effectClass) {
		mEffectClass = effectClass;
	}

	public void put(Effect effect, double prob) throws IncompatibleEffectClassException {
		if (!sanityCheck(effect)) {
			throw new IncompatibleEffectClassException(effect.getEffectClass());
		}
		mProbEffect.put(effect, prob);
	}

	public void putAll(ProbabilisticEffect probEffect) throws IncompatibleEffectClassException {
		if (!sanityCheck(probEffect)) {
			throw new IncompatibleEffectClassException(probEffect.getEffectClass());
		}
		mProbEffect.putAll(probEffect.mProbEffect);
	}

	public void putAll(ProbabilisticEffect... probEffects) throws IncompatibleEffectClassException {
		EffectClass emptyEffectClass = new EffectClass();
		ProbabilisticEffect runningProbEffect = new ProbabilisticEffect(emptyEffectClass);
		for (ProbabilisticEffect probEffect : probEffects) {
			runningProbEffect = putAllHelper(runningProbEffect, probEffect);
		}
	}

	private ProbabilisticEffect putAllHelper(ProbabilisticEffect probEffectA, ProbabilisticEffect probEffectB)
			throws IncompatibleEffectClassException {
		EffectClass aggrEffectClass = new EffectClass();
		aggrEffectClass.addAll(probEffectA.getEffectClass());
		aggrEffectClass.addAll(probEffectB.getEffectClass());
		ProbabilisticEffect aggrProbEffect = new ProbabilisticEffect(aggrEffectClass);

		for (Entry<Effect, Double> eA : probEffectA) {
			Effect effectA = eA.getKey();
			Double probA = eA.getValue();
			for (Entry<Effect, Double> eB : probEffectB) {
				Effect effectB = eB.getKey();
				Double probB = eB.getValue();

				Effect aggrEffect = new Effect(aggrEffectClass);
				aggrEffect.addAll(effectA);
				aggrEffect.addAll(effectB);
				double jointProb = probA * probB;
				aggrProbEffect.put(aggrEffect, jointProb);
			}
		}
		return aggrProbEffect;
	}

	private boolean sanityCheck(Effect effect) {
		return effect.getEffectClass().equals(mEffectClass);
	}

	private boolean sanityCheck(ProbabilisticEffect probEffect) {
		return probEffect.getEffectClass().equals(mEffectClass);
	}

	public double getProbability(Effect effect) throws EffectNotFoundException {
		if (!mProbEffect.containsKey(effect)) {
			throw new EffectNotFoundException(effect);
		}
		return mProbEffect.get(effect);
	}

	public double getMarginalProbability(StateVarTuple subEffect) {
		double marginalProb = 0;
		for (Entry<Effect, Double> e : mProbEffect.entrySet()) {
			Effect effect = e.getKey();
			double jointProb = e.getValue();

			if (effect.contains(subEffect)) {
				marginalProb += jointProb;
			}
		}
		return marginalProb;
	}

	public EffectClass getEffectClass() {
		return mEffectClass;
	}

	@Override
	public Iterator<Entry<Effect, Double>> iterator() {
		return mProbEffect.entrySet().iterator();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof ProbabilisticEffect)) {
			return false;
		}
		ProbabilisticEffect probEffect = (ProbabilisticEffect) obj;
		return probEffect.mProbEffect.equals(mProbEffect) && probEffect.mEffectClass.equals(mEffectClass);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mProbEffect.hashCode();
			result = 31 * result + mEffectClass.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
