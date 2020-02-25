package language.mdp;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import language.domain.models.ActionDefinition;
import language.domain.models.IAction;
import language.domain.models.IProbabilisticTransitionFormula;
import language.domain.models.IStateVarValue;
import language.domain.models.StateVarDefinition;
import language.exceptions.ActionNotFoundException;
import language.exceptions.StateVarClassNotFoundException;
import language.exceptions.VarNotFoundException;
import language.exceptions.XMDPException;

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
	private Precondition<E> mPrecondition;
	private DiscriminantClass mDiscriminantClass;
	private EffectClass mEffectClass;
	private IProbabilisticTransitionFormula<E> mProbTransFormula;

	public FormulaActionDescription(ActionDefinition<E> actionDefinition, Precondition<E> precondition,
			DiscriminantClass discriminantClass, EffectClass effectClass,
			IProbabilisticTransitionFormula<E> transitionFormula) {
		mActionDefinition = actionDefinition;
		mPrecondition = precondition;
		mDiscriminantClass = discriminantClass;
		mEffectClass = effectClass;
		mProbTransFormula = transitionFormula;
	}

	@Override
	public Set<ProbabilisticTransition<E>> getProbabilisticTransitions(E action) throws XMDPException {
		Set<ProbabilisticTransition<E>> probTransitions = new HashSet<>();

		if (mDiscriminantClass.isEmpty()) {
			Discriminant emptyDiscr = new Discriminant(mDiscriminantClass);
			ProbabilisticTransition<E> probTrans = getProbabilisticTransition(emptyDiscr, action);
			probTransitions.add(probTrans);
		} else {
			Set<Discriminant> allDiscriminants = getAllDiscriminants(mDiscriminantClass, action);
			for (Discriminant discriminant : allDiscriminants) {
				ProbabilisticTransition<E> probTrans = getProbabilisticTransition(discriminant, action);
				probTransitions.add(probTrans);
			}
		}
		return probTransitions;
	}

	private ProbabilisticTransition<E> getProbabilisticTransition(Discriminant discriminant, E action)
			throws XMDPException {
		ProbabilisticEffect probEffect = mProbTransFormula.formula(discriminant, action);
		return new ProbabilisticTransition<>(probEffect, discriminant, action);
	}

	/**
	 * Create all possible combinations of values of a given discriminant class recursively.
	 * 
	 * @param discrClass
	 * @param action
	 * @return All possible combinations of values of a given discriminant class -- as a set of discriminants.
	 * @throws XMDPException
	 */
	private Set<Discriminant> getAllDiscriminants(DiscriminantClass discrClass, E action) throws XMDPException {
		// If precondition has a multivariate predicate on the discriminant class, get all applicable discriminants
		// from precondition
		if (mPrecondition.hasMultivarPredicateOn(discrClass.getStateVarClass())) {
			return getAllDiscriminantsFromPrecondition(discrClass, action);
		}

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

		// Build a set of all discriminants of the variable srcVarDef
		Set<Discriminant> discriminants = new HashSet<>();
		DiscriminantClass srcVarDiscrClass = new DiscriminantClass();
		srcVarDiscrClass.add(srcVarDef);

		for (IStateVarValue value : applicableValues) {
			Discriminant discriminant = new Discriminant(srcVarDiscrClass);
			discriminant.add(srcVarDef.getStateVar(value));
			discriminants.add(discriminant);
		}

		if (subDiscriminants.isEmpty()) {
			return discriminants;
		}

		// Build a set of all discriminants of all variables
		for (Discriminant tailDiscriminant : subDiscriminants) {
			for (Discriminant headDiscriminant : discriminants) {
				Discriminant fullDiscriminant = new Discriminant(discrClass);
				fullDiscriminant.addAll(headDiscriminant);
				fullDiscriminant.addAll(tailDiscriminant);

				allDiscriminants.add(fullDiscriminant);
			}
		}

		return allDiscriminants;
	}

	private Set<Discriminant> getAllDiscriminantsFromPrecondition(DiscriminantClass discrClass, E action)
			throws ActionNotFoundException, StateVarClassNotFoundException, VarNotFoundException {
		Set<StateVarTuple> applicableTuples = mPrecondition.getApplicableTuples(action, discrClass.getStateVarClass());
		Set<Discriminant> discriminants = new HashSet<>();
		for (StateVarTuple applicableTuple : applicableTuples) {
			Discriminant discriminant = new Discriminant(discrClass);
			discriminant.addAllRelevant(applicableTuple);
			discriminants.add(discriminant);
		}
		return discriminants;
	}

	@Override
	public ProbabilisticEffect getProbabilisticEffect(Discriminant discriminant, E action) throws XMDPException {
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
