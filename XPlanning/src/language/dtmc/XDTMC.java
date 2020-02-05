package language.dtmc;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import language.domain.models.ActionDefinition;
import language.domain.models.IAction;
import language.domain.models.IStateVarValue;
import language.domain.models.StateVar;
import language.domain.models.StateVarDefinition;
import language.exceptions.XMDPException;
import language.mdp.Discriminant;
import language.mdp.DiscriminantClass;
import language.mdp.EffectClass;
import language.mdp.FactoredPSO;
import language.mdp.IActionDescription;
import language.mdp.ProbabilisticEffect;
import language.mdp.StateVarTuple;
import language.mdp.XMDP;
import language.policy.Decision;
import language.policy.Policy;

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

	public XDTMC(XMDP xmdp, Policy policy) throws XMDPException {
		mXMDP = xmdp;
		mPolicy = policy;
		induceDTMC(xmdp, policy);
	}

	private void induceDTMC(XMDP xmdp, Policy policy) throws XMDPException {
		for (Decision decision : policy) {
			StateVarTuple predicate = decision.getState();
			IAction action = decision.getAction();
			ActionDefinition<IAction> actionDef = xmdp.getActionSpace().getActionDefinition(action);

			if (!mDTMC.containsKey(actionDef)) {
				TwoTBN<IAction> twoTBN = new TwoTBN<>(actionDef);
				mDTMC.put(actionDef, twoTBN);
			}

			TwoTBN<IAction> twoTBN = mDTMC.get(actionDef);

			// Any action that is part of a composite action (i.e., constituent action) has additional effect classes
			// that are defined in the composite action PSO, but not defined in the individual action PSO.

			// Therefore, when we look up action PSO of a constituent action (e.g., of a policy), we must get both the
			// individual action PSO (if exists) and the composite action PSO -- so that we get all the effect classes
			// of the constituent action.

			// TODO
			ActionDefinition<IAction> parentCompActionDef = actionDef.getParentCompositeActionDefinition();
			if (parentCompActionDef != null) {
				FactoredPSO<IAction> parentCompActionPSO = xmdp.getTransitionFunction()
						.getActionPSO(parentCompActionDef);
				Set<EffectClass> additionalEffectClasses = parentCompActionPSO.getIndependentEffectClasses();
			}

			FactoredPSO<IAction> actionPSO = xmdp.getTransitionFunction().getActionPSO(actionDef);
			Set<EffectClass> effectClasses = actionPSO.getIndependentEffectClasses();

			for (EffectClass effectClass : effectClasses) {
				IActionDescription<IAction> actionDesc = actionPSO.getActionDescription(effectClass);
				DiscriminantClass discrClass = actionDesc.getDiscriminantClass();

				Discriminant discriminant = new Discriminant(discrClass);
				for (StateVarDefinition<IStateVarValue> stateVarDef : discrClass) {
					IStateVarValue value = predicate.getStateVarValue(IStateVarValue.class, stateVarDef);
					StateVar<IStateVarValue> stateVar = stateVarDef.getStateVar(value);
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
