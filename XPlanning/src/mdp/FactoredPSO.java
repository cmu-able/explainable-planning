package mdp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import exceptions.DiscriminantNotFoundException;
import exceptions.EffectClassNotFoundException;
import exceptions.VarNotFoundException;
import factors.IStateVarValue;
import factors.StateVarDefinition;

/**
 * {@link FactoredPSO} is a generic factored PSO representation.
 * 
 * @author rsukkerd
 *
 */
public class FactoredPSO implements IFactoredPSO {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	/**
	 * Precondition of this action
	 */
	private Precondition mPrecondition;

	/**
	 * Full action descriptions for all independent effect classes of this action
	 */
	private Map<EffectClass, IActionDescription> mActionDescriptions;

	public FactoredPSO(Precondition precondition) {
		mPrecondition = precondition;
		mActionDescriptions = new HashMap<>();
	}

	public void addActionDescription(IActionDescription actionDesc) {
		mActionDescriptions.put(actionDesc.getEffectClass(), actionDesc);
	}

	@Override
	public Precondition getPrecondition() {
		return mPrecondition;
	}

	@Override
	public Set<EffectClass> getIndependentEffectClasses() {
		return mActionDescriptions.keySet();
	}

	@Override
	public IActionDescription getActionDescription(EffectClass effectClass) throws EffectClassNotFoundException {
		if (!mActionDescriptions.containsKey(effectClass)) {
			throw new EffectClassNotFoundException(effectClass);
		}
		return mActionDescriptions.get(effectClass);
	}

	@Override
	public DiscriminantClass getDiscriminantClass(StateVarDefinition<IStateVarValue> stateVarDef)
			throws VarNotFoundException {
		for (Entry<EffectClass, IActionDescription> e : mActionDescriptions.entrySet()) {
			EffectClass effectClass = e.getKey();
			IActionDescription actionDesc = e.getValue();
			if (effectClass.contains(stateVarDef)) {
				return actionDesc.getDiscriminantClass();
			}
		}
		throw new VarNotFoundException(stateVarDef);
	}

	@Override
	public Set<IStateVarValue> getPossibleImpact(StateVarDefinition<IStateVarValue> stateVarDef,
			Discriminant discriminant) throws VarNotFoundException, DiscriminantNotFoundException {
		for (Entry<EffectClass, IActionDescription> e : mActionDescriptions.entrySet()) {
			EffectClass effectClass = e.getKey();
			IActionDescription actionDesc = e.getValue();
			if (effectClass.contains(stateVarDef)) {
				ProbabilisticEffect probEffect = actionDesc.getProbabilisticEffect(discriminant);
				Set<IStateVarValue> possibleImpact = new HashSet<>();
				for (Entry<Effect, Double> en : probEffect) {
					Effect effect = en.getKey();
					IStateVarValue value = effect.getEffectValue(stateVarDef);
					possibleImpact.add(value);
				}
				return possibleImpact;
			}
		}
		throw new VarNotFoundException(stateVarDef);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof FactoredPSO)) {
			return false;
		}
		FactoredPSO pso = (FactoredPSO) obj;
		return pso.mPrecondition.equals(mPrecondition) && pso.mActionDescriptions.equals(mActionDescriptions);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mPrecondition.hashCode();
			result = 31 * result + mActionDescriptions.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
