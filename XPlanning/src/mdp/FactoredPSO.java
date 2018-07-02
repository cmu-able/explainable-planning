package mdp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import exceptions.EffectClassNotFoundException;
import exceptions.VarNotFoundException;
import exceptions.XMDPException;
import factors.ActionDefinition;
import factors.IAction;
import factors.IStateVarValue;
import factors.StateVarDefinition;

/**
 * {@link FactoredPSO} is a generic "factored" probabilistic STRIPS operator (PSO) representation.
 * 
 * Reference: Using Abstractions for Decision-Theoretic Planning with Time Constraints, Boutilier & Dearden, 1994
 * 
 * @author rsukkerd
 *
 * @param <E>
 */
public class FactoredPSO<E extends IAction> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	/**
	 * Action definition of this type
	 */
	private ActionDefinition<E> mActionDef;

	/**
	 * Preconditions of actions of this type
	 */
	private Precondition<E> mPrecondition;

	/**
	 * Full action descriptions for all independent effect classes of this type of action
	 */
	private Map<EffectClass, IActionDescription<E>> mActionDescriptions = new HashMap<>();

	public FactoredPSO(ActionDefinition<E> actionDef, Precondition<E> precondition) {
		mActionDef = actionDef;
		mPrecondition = precondition;
	}

	public void addActionDescription(IActionDescription<E> actionDesc) {
		mActionDescriptions.put(actionDesc.getEffectClass(), actionDesc);
	}

	public ActionDefinition<E> getActionDefinition() {
		return mActionDef;
	}

	public Precondition<E> getPrecondition() {
		return mPrecondition;
	}

	public Set<EffectClass> getIndependentEffectClasses() {
		return mActionDescriptions.keySet();
	}

	public IActionDescription<E> getActionDescription(EffectClass effectClass) throws EffectClassNotFoundException {
		if (!mActionDescriptions.containsKey(effectClass)) {
			throw new EffectClassNotFoundException(effectClass);
		}
		return mActionDescriptions.get(effectClass);
	}

	public DiscriminantClass getDiscriminantClass(StateVarDefinition<IStateVarValue> stateVarDef)
			throws VarNotFoundException {
		for (Entry<EffectClass, IActionDescription<E>> e : mActionDescriptions.entrySet()) {
			EffectClass effectClass = e.getKey();
			IActionDescription<E> actionDesc = e.getValue();
			if (effectClass.contains(stateVarDef)) {
				return actionDesc.getDiscriminantClass();
			}
		}
		throw new VarNotFoundException(stateVarDef);
	}

	public Set<IStateVarValue> getPossibleImpact(StateVarDefinition<IStateVarValue> stateVarDef,
			Discriminant discriminant, E action) throws XMDPException {
		for (Entry<EffectClass, IActionDescription<E>> e : mActionDescriptions.entrySet()) {
			EffectClass effectClass = e.getKey();
			IActionDescription<E> actionDesc = e.getValue();
			if (effectClass.contains(stateVarDef)) {
				ProbabilisticEffect probEffect = actionDesc.getProbabilisticEffect(discriminant, action);
				Set<IStateVarValue> possibleImpact = new HashSet<>();
				for (Entry<Effect, Double> en : probEffect) {
					Effect effect = en.getKey();
					IStateVarValue value = effect.getEffectValue(IStateVarValue.class, stateVarDef);
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
		if (!(obj instanceof FactoredPSO<?>)) {
			return false;
		}
		FactoredPSO<?> pso = (FactoredPSO<?>) obj;
		return pso.mActionDef.equals(mActionDef) && pso.mPrecondition.equals(mPrecondition)
				&& pso.mActionDescriptions.equals(mActionDescriptions);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mActionDef.hashCode();
			result = 31 * result + mPrecondition.hashCode();
			result = 31 * result + mActionDescriptions.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
