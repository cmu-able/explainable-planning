package dtmc;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import exceptions.ActionDefinitionNotFoundException;
import exceptions.ActionNotFoundException;
import exceptions.DiscriminantNotFoundException;
import exceptions.EffectClassNotFoundException;
import exceptions.IncompatibleActionException;
import exceptions.IncompatibleVarException;
import exceptions.VarNotFoundException;
import factors.ActionDefinition;
import factors.IAction;
import factors.IStateVarValue;
import factors.StateVar;
import factors.StateVarDefinition;
import mdp.Discriminant;
import mdp.DiscriminantClass;
import mdp.EffectClass;
import mdp.FactoredPSO;
import mdp.IActionDescription;
import mdp.ProbabilisticEffect;
import mdp.XMDP;
import policy.Decision;
import policy.Policy;
import policy.Predicate;

/**
 * {@link XDTMC} is a discrete-time Markov chain induced from a policy and a MDP. It is consists of a 2TBN for each
 * action type.
 * 
 * @author rsukkerd
 *
 */
public class XDTMC implements Iterable<TwoTBN<IAction>> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private XMDP mXMDP;
	private Policy mPolicy;
	private Map<ActionDefinition<IAction>, TwoTBN<IAction>> mDTMC = new HashMap<>();

	public XDTMC(XMDP xmdp, Policy policy) throws ActionDefinitionNotFoundException, EffectClassNotFoundException,
			VarNotFoundException, IncompatibleVarException, ActionNotFoundException, DiscriminantNotFoundException,
			IncompatibleActionException {
		mXMDP = xmdp;
		mPolicy = policy;
		induceDTMC(xmdp, policy);
	}

	private void induceDTMC(XMDP xmdp, Policy policy) throws ActionDefinitionNotFoundException,
			EffectClassNotFoundException, VarNotFoundException, IncompatibleVarException, ActionNotFoundException,
			DiscriminantNotFoundException, IncompatibleActionException {
		for (Decision decision : policy) {
			Predicate predicate = decision.getPredicate();
			IAction action = decision.getAction();
			ActionDefinition<IAction> actionDef = xmdp.getActionSpace().getActionDefinition(action);

			if (!mDTMC.containsKey(actionDef)) {
				TwoTBN<IAction> twoTBN = new TwoTBN<>(actionDef);
				mDTMC.put(actionDef, twoTBN);
			}

			TwoTBN<IAction> twoTBN = mDTMC.get(actionDef);

			FactoredPSO<IAction> actionPSO = xmdp.getTransitionFunction().getActionPSO(actionDef);
			Set<EffectClass> effectClasses = actionPSO.getIndependentEffectClasses();

			for (EffectClass effectClass : effectClasses) {
				IActionDescription<IAction> actionDesc = actionPSO.getActionDescription(effectClass);
				DiscriminantClass discrClass = actionDesc.getDiscriminantClass();

				Discriminant discriminant = new Discriminant(discrClass);
				for (StateVarDefinition<IStateVarValue> stateVarDef : discrClass) {
					IStateVarValue value = predicate.getStateVarValue(IStateVarValue.class, stateVarDef);
					StateVar<IStateVarValue> stateVar = new StateVar<>(stateVarDef, value);
					discriminant.add(stateVar);
				}

				ProbabilisticEffect probEffect = actionDesc.getProbabilisticEffect(discriminant, action);
				twoTBN.add(predicate, action, probEffect);
			}
		}
	}

	public TwoTBN<IAction> get2TBN(ActionDefinition<IAction> actionDef) {
		return mDTMC.get(actionDef);
	}

	public XMDP getXMDP() {
		return mXMDP;
	}

	public Policy getPolicy() {
		return mPolicy;
	}

	@Override
	public Iterator<TwoTBN<IAction>> iterator() {
		return mDTMC.values().iterator();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof XDTMC)) {
			return false;
		}
		XDTMC dtmc = (XDTMC) obj;
		return dtmc.mDTMC.equals(mDTMC);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mDTMC.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
