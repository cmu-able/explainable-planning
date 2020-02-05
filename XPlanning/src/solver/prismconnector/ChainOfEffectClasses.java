package solver.prismconnector;

import java.util.ArrayList;
import java.util.List;

import language.domain.models.IAction;
import language.mdp.EffectClass;
import language.mdp.FactoredPSO;

/**
 * {@link ChainOfEffectClasses} is a "chain" of {@link EffectClass}-{@link FactoredPSO} pairs.
 * 
 * Each {@link EffectClass} is mapped to its corresponding {@link FactoredPSO}. Two (NOT necessarily unique) effect
 * classes are "chained" iff:
 * 
 * (1)~they are associated with different action types (i.e., belong to different unique {@link FactoredPSO}s), but they
 * have overlapping state variables, or
 * 
 * (2)~they are associated with the same action type (by definition, they do not overlap), but they overlap with other
 * effect classes of other action types that are "chained".
 * 
 * @author rsukkerd
 *
 */
public class ChainOfEffectClasses {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private List<EffectClass> mIndexedEffectClasses = new ArrayList<>();
	private List<FactoredPSO<IAction>> mIndexedActionPSOs = new ArrayList<>();

	public ChainOfEffectClasses() {
		// Chain is initially empty
	}

	public void addEffectClass(EffectClass effectClass, FactoredPSO<IAction> actionPSO) {
		mIndexedEffectClasses.add(effectClass);
		mIndexedActionPSOs.add(actionPSO);
	}

	public void addChain(ChainOfEffectClasses chain) {
		mIndexedEffectClasses.addAll(chain.mIndexedEffectClasses);
		mIndexedActionPSOs.addAll(chain.mIndexedActionPSOs);
	}

	public int getChainLength() {
		return mIndexedEffectClasses.size();
	}

	public EffectClass getEffectClass(int index) {
		return mIndexedEffectClasses.get(index);
	}

	public FactoredPSO<IAction> getFactoredPSO(int index) {
		return mIndexedActionPSOs.get(index);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof ChainOfEffectClasses)) {
			return false;
		}
		ChainOfEffectClasses chain = (ChainOfEffectClasses) obj;
		return chain.mIndexedEffectClasses.equals(mIndexedEffectClasses)
				&& chain.mIndexedActionPSOs.equals(mIndexedActionPSOs);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mIndexedEffectClasses.hashCode();
			result = 31 * result + mIndexedActionPSOs.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
