package factors;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import exceptions.TransitionProbabilityNotFoundException;

/**
 * {@link CPT} represents a generic a conditional probability table (CPT). A CPT can access state variables, actions,
 * and action attributes.
 * 
 * @author rsukkerd
 *
 */
public class CPT implements ICPT {

	private Map<Transition, Double> mCPT;

	public CPT() {
		mCPT = new HashMap<>();
	}

	public void addConditionalProbability(double prob, IStateVar destStateVar, IAction action,
			IStateVar... srcStateVars) {
		Transition trans = createTransition(destStateVar, action, srcStateVars);
		mCPT.put(trans, prob);
	}

	public double getConditionalProbability(IStateVar destStateVar, IAction action, IStateVar... srcStateVars)
			throws TransitionProbabilityNotFoundException {
		Transition trans = createTransition(destStateVar, action, srcStateVars);
		if (!mCPT.containsKey(trans)) {
			throw new TransitionProbabilityNotFoundException(trans);
		}
		return mCPT.get(trans);
	}

	private Transition createTransition(IStateVar destStateVar, IAction action, IStateVar... srcStateVars) {
		Set<IStateVar> srcVars = new HashSet<>();
		Set<IStateVar> destVars = new HashSet<>();
		for (IStateVar var : srcStateVars) {
			srcVars.add(var);
		}
		destVars.add(destStateVar);
		return new Transition(srcVars, action, destVars);
	}
}
