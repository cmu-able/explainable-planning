package mdp;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import exceptions.ActionNotApplicableException;
import exceptions.ActionNotFoundException;
import exceptions.AttributeNameNotFoundException;
import exceptions.IncompatibleEffectClassException;
import exceptions.IncompatibleVarException;
import exceptions.VarNotFoundException;
import factors.ActionDefinition;
import factors.IAction;
import factors.IProbabilisticTransitionFormula;
import factors.IStateVarValue;
import factors.StateVarDefinition;

/**
 * 
 * {@link FormulaActionDescription} is a generic action description of a specific {@link EffectClass}. A "formula"
 * action description functionally maps a set of mutually exclusive discriminants to the corresponding probabilistic
 * effects.
 * 
 * @author rsukkerd
 *
 * @param <E>
 */
public class FormulaActionDescription<E extends IAction> implements IActionDescription<E> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private ActionDefinition<E> mActionDefinition;
	private DiscriminantClass mDiscriminantClass;
	private EffectClass mEffectClass;
	private IProbabilisticTransitionFormula<E> mProbTransFormula;

	public FormulaActionDescription(ActionDefinition<E> actionDefinition, DiscriminantClass discriminantClass,
			EffectClass effectClass, IProbabilisticTransitionFormula<E> transitionFormula) {
		mActionDefinition = actionDefinition;
		mDiscriminantClass = discriminantClass;
		mEffectClass = effectClass;
		mProbTransFormula = transitionFormula;
	}

	@Override
	public Set<ProbabilisticTransition<E>> getProbabilisticTransitions(E action)
			throws ActionNotFoundException, IncompatibleVarException, VarNotFoundException,
			AttributeNameNotFoundException, IncompatibleEffectClassException, ActionNotApplicableException {
		Set<ProbabilisticTransition<E>> probTransitions = new HashSet<>();
		Set<Discriminant> allDiscriminants = getAllDiscriminants(mDiscriminantClass, action,
				mProbTransFormula.getPrecondition());
		for (Discriminant discriminant : allDiscriminants) {
			ProbabilisticEffect probEffect = mProbTransFormula.formula(discriminant, action);
			ProbabilisticTransition<E> probTrans = new ProbabilisticTransition<>(probEffect, discriminant, action);
			probTransitions.add(probTrans);
		}
		return probTransitions;
	}

	/**
	 * Create all possible combinations of values of a given discriminant class recursively.
	 * 
	 * @param discrClass
	 * @param action
	 * @param precondition
	 * @return All possible combinations of values of a given discriminant class -- as a set of discriminants.
	 * @throws ActionNotFoundException
	 * @throws IncompatibleVarException
	 */
	private Set<Discriminant> getAllDiscriminants(DiscriminantClass discrClass, E action, Precondition<E> precondition)
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
		Set<Discriminant> subDiscriminants = getAllDiscriminants(copyDiscrClass, action, precondition);
		Set<IStateVarValue> applicableValues = precondition.getApplicableValues(action, srcVarDef);

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
	public ProbabilisticEffect getProbabilisticEffect(Discriminant discriminant, E action)
			throws VarNotFoundException, AttributeNameNotFoundException, IncompatibleVarException,
			IncompatibleEffectClassException, ActionNotFoundException, ActionNotApplicableException {
		return mProbTransFormula.formula(discriminant, action);
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
		if (!(obj instanceof FormulaActionDescription<?>)) {
			return false;
		}
		FormulaActionDescription<?> actionDesc = (FormulaActionDescription<?>) obj;
		return actionDesc.mProbTransFormula.equals(mProbTransFormula)
				&& actionDesc.mActionDefinition.equals(mActionDefinition)
				&& actionDesc.mDiscriminantClass.equals(mDiscriminantClass)
				&& actionDesc.mEffectClass.equals(mEffectClass);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mProbTransFormula.hashCode();
			result = 31 * result + mActionDefinition.hashCode();
			result = 31 * result + mDiscriminantClass.hashCode();
			result = 31 * result + mEffectClass.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
