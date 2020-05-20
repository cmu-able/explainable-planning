package language.policy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import language.domain.models.IAction;
import language.exceptions.StateNotFoundException;
import language.mdp.StateVarTuple;

/**
 * {@link Policy} contains a set of {@link Decision}s.
 * 
 * @author rsukkerd
 *
 */
public class Policy implements Iterable<Decision> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Set<Decision> mDecisions = new HashSet<>();
	private Map<StateVarTuple, IAction> mPolicy = new HashMap<>(); // For fast look-up

	public Policy() {
		// mDecisions and mPolicy are initially empty
	}

	public Policy(Policy initialPolicy) {
		// Initialize this policy with existing content
		// Part of this content may be overridden later
		mDecisions.addAll(initialPolicy.mDecisions);
		mPolicy.putAll(initialPolicy.mPolicy);
	}

	public void put(StateVarTuple state, IAction action) {
		// Override state->action mapping in this policy (if already exists)
		mDecisions.removeIf(existingDecision -> existingDecision.getState().equals(state));

		Decision decision = new Decision(state, action);
		mDecisions.add(decision);
		mPolicy.put(state, action);
	}

	public void remove(Decision decision) {
		mDecisions.remove(decision);
		mPolicy.remove(decision.getState(), decision.getAction());
	}

	public IAction getAction(StateVarTuple state) throws StateNotFoundException {
		if (!mPolicy.containsKey(state)) {
			throw new StateNotFoundException(state);
		}
		return mPolicy.get(state);
	}

	public boolean containsState(StateVarTuple state) {
		return mPolicy.containsKey(state);
	}

	public boolean containsAction(IAction action) {
		return mPolicy.containsValue(action);
	}

	@Override
	public Iterator<Decision> iterator() {
		return mDecisions.iterator();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof Policy)) {
			return false;
		}
		Policy policy = (Policy) obj;
		return policy.mDecisions.equals(mDecisions);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mDecisions.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
