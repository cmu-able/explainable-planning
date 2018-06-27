package mdp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import exceptions.ActionNotFoundException;
import exceptions.DiscriminantNotFoundException;
import exceptions.EffectNotFoundException;
import exceptions.IncompatibleActionException;
import exceptions.IncompatibleDiscriminantClassException;
import exceptions.IncompatibleEffectClassException;
import exceptions.IncompatibleVarException;
import factors.ActionDefinition;
import factors.IAction;
import factors.IProbabilisticTransitionFormula;
import factors.IStateVarValue;
import factors.StateVarDefinition;

/**
 * 
 * {@link ActionDescription} is a generic action description of a specific {@link EffectClass}. An action description
 * maps a set of mutually exclusive discriminants to the corresponding probabilistic effects.
 * 
 * @author rsukkerd
 *
 * @param <E>
 */
public class ActionDescription<E extends IAction> implements IActionDescription<E> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private ActionDefinition<E> mActionDefinition;
	private Precondition<E> mPrecondition;
	private DiscriminantClass mDiscriminantClass = new DiscriminantClass();
	private EffectClass mEffectClass = new EffectClass();

	private Map<E, Set<ProbabilisticTransition<E>>> mActionDescriptions = new HashMap<>();
	private Map<E, Map<Discriminant, ProbabilisticEffect>> mLookupTable = new HashMap<>(); // For fast look-up

	// TODO
	private IProbabilisticTransitionFormula<E> mTransitionFormula;

	public ActionDescription(ActionDefinition<E> actionDefinition, Precondition<E> precondition) {
		mActionDefinition = actionDefinition;
		mPrecondition = precondition;
	}

	public void addDiscriminantVarDef(StateVarDefinition<? extends IStateVarValue> stateVarDef) {
		mDiscriminantClass.add(stateVarDef);
	}

	public void addEffectVarDef(StateVarDefinition<? extends IStateVarValue> stateVarDef) {
		mEffectClass.add(stateVarDef);
	}

	public void addProbabilisticTransitionFormula(IProbabilisticTransitionFormula<E> formula) {
		mTransitionFormula = formula;
	}

	public void put(ProbabilisticEffect probEffect, Discriminant discriminant, E action)
			throws IncompatibleActionException, IncompatibleDiscriminantClassException,
			IncompatibleEffectClassException {
		ProbabilisticTransition<E> probTrans = new ProbabilisticTransition<>(probEffect, discriminant, action);
		put(probTrans);
	}

	public void put(ProbabilisticTransition<E> probTrans) throws IncompatibleActionException,
			IncompatibleDiscriminantClassException, IncompatibleEffectClassException {
		E action = probTrans.getAction();
		Discriminant discriminant = probTrans.getDiscriminant();
		ProbabilisticEffect probEffect = probTrans.getProbabilisticEffect();

		if (!sanityCheck(action)) {
			throw new IncompatibleActionException(action);
		}
		if (!sanityCheck(discriminant)) {
			throw new IncompatibleDiscriminantClassException(discriminant.getDiscriminantClass());
		}
		if (!sanityCheck(probEffect)) {
			throw new IncompatibleEffectClassException(probEffect.getEffectClass());
		}
		if (!mActionDescriptions.containsKey(action)) {
			Set<ProbabilisticTransition<E>> actionDesc = new HashSet<>();
			actionDesc.add(probTrans);
			mActionDescriptions.put(action, actionDesc);

			// For fast look-up
			Map<Discriminant, ProbabilisticEffect> table = new HashMap<>();
			table.put(discriminant, probEffect);
			mLookupTable.put(action, table);
		} else {
			mActionDescriptions.get(action).add(probTrans);

			// For fast look-up
			mLookupTable.get(action).put(discriminant, probEffect);
		}
	}

	public void putAll(Set<ProbabilisticTransition<E>> probTransitions) throws IncompatibleActionException,
			IncompatibleDiscriminantClassException, IncompatibleEffectClassException {
		for (ProbabilisticTransition<E> probTrans : probTransitions) {
			put(probTrans);
		}
	}

	private boolean sanityCheck(E action) {
		return mActionDefinition.getActions().contains(action);
	}

	private boolean sanityCheck(Discriminant discriminant) {
		return discriminant.getDiscriminantClass().equals(mDiscriminantClass);
	}

	private boolean sanityCheck(ProbabilisticEffect probEffect) {
		return probEffect.getEffectClass().equals(mEffectClass);
	}

	public Precondition<E> getPrecondition() {
		return mPrecondition;
	}

	@Override
	public Set<ProbabilisticTransition<E>> getProbabilisticTransitions(E action)
			throws ActionNotFoundException, IncompatibleVarException {
		Set<ProbabilisticTransition<E>> probTransitions = new HashSet<>();
		Set<Discriminant> allDiscriminants = getAllDiscriminants(mDiscriminantClass, action);
		for (Discriminant discriminant : allDiscriminants) {
			ProbabilisticEffect probEffect = mTransitionFormula.formula(discriminant, action);
			ProbabilisticTransition<E> probTrans = new ProbabilisticTransition<>(probEffect, discriminant, action);
			probTransitions.add(probTrans);
		}
		return probTransitions;

		// if (!mActionDescriptions.containsKey(action)) {
		// throw new ActionNotFoundException(action);
		// }
		// return mActionDescriptions.get(action);
	}

	private Set<Discriminant> getAllDiscriminants(DiscriminantClass discrClass, E action)
			throws ActionNotFoundException, IncompatibleVarException {
		Set<Discriminant> allDiscriminants = new HashSet<>();

		DiscriminantClass copyDiscrClass = new DiscriminantClass();
		copyDiscrClass.addAll(discrClass);

		Iterator<StateVarDefinition<IStateVarValue>> iter = copyDiscrClass.iterator();
		if (!iter.hasNext()) {
			return allDiscriminants;
		}

		StateVarDefinition<IStateVarValue> srcVarDef = iter.next();
		iter.remove();
		Set<Discriminant> subDiscriminants = getAllDiscriminants(copyDiscrClass, action);
		Set<IStateVarValue> applicableValues = mPrecondition.getApplicableValues(action, srcVarDef);

		for (IStateVarValue value : applicableValues) {
			for (Discriminant subDiscriminant : subDiscriminants) {
				Discriminant discriminant = new Discriminant(discrClass);
				discriminant.addAll(subDiscriminant);
				discriminant.add(srcVarDef.getStateVar(value));
				allDiscriminants.add(discriminant);
			}
		}

		return allDiscriminants;
	}

	@Override
	public double getProbability(Effect effect, Discriminant discriminant, E action)
			throws ActionNotFoundException, DiscriminantNotFoundException, EffectNotFoundException {
		if (!mLookupTable.containsKey(action)) {
			throw new ActionNotFoundException(action);
		}
		if (!mLookupTable.get(action).containsKey(discriminant)) {
			throw new DiscriminantNotFoundException(discriminant);
		}
		return mLookupTable.get(action).get(discriminant).getProbability(effect);
	}

	@Override
	public ProbabilisticEffect getProbabilisticEffect(Discriminant discriminant, E action)
			throws ActionNotFoundException, DiscriminantNotFoundException {
		if (!mLookupTable.containsKey(action)) {
			throw new ActionNotFoundException(action);
		}
		if (!mLookupTable.get(action).containsKey(discriminant)) {
			throw new DiscriminantNotFoundException(discriminant);
		}
		return mLookupTable.get(action).get(discriminant);
	}

	@Override
	public ActionDefinition<E> getActionDefinition() {
		return mActionDefinition;
	}

	@Override
	public DiscriminantClass getDiscriminantClass() {
		return mDiscriminantClass;
	}

	@Override
	public EffectClass getEffectClass() {
		return mEffectClass;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof ActionDescription<?>)) {
			return false;
		}
		ActionDescription<?> actionDesc = (ActionDescription<?>) obj;
		return actionDesc.mActionDescriptions.equals(mActionDescriptions)
				&& actionDesc.mActionDefinition.equals(mActionDefinition)
				&& actionDesc.mDiscriminantClass.equals(mDiscriminantClass)
				&& actionDesc.mEffectClass.equals(mEffectClass);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mActionDescriptions.hashCode();
			result = 31 * result + mActionDefinition.hashCode();
			result = 31 * result + mDiscriminantClass.hashCode();
			result = 31 * result + mEffectClass.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
