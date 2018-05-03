package policy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import exceptions.PredicateNotFoundException;
import factors.IAction;
import mdp.State;

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
	private Map<State, IAction> mPolicy = new HashMap<>(); // For fast look-up

	public Policy() {
		// mDecisions and mPolicy are initially empty
	}

	public void put(State state, IAction action) {
		Decision decision = new Decision(state, action);
		mDecisions.add(decision);
		mPolicy.put(state, action);
	}

	public IAction getAction(State state) throws PredicateNotFoundException {
		if (!mPolicy.containsKey(state)) {
			throw new PredicateNotFoundException(state);
		}
		return mPolicy.get(state);
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
