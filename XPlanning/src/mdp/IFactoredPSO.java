package mdp;

import java.util.Set;

import exceptions.EffectClassNotFoundException;

/**
 * {@link IFactoredPSO} is an interface to a "factored" probabilistic STRIPS operator representation.
 * 
 * Reference: Using Abstractions for Decision-Theoretic Planning with Time Constraints, Boutilier & Dearden, 1994
 * 
 * @author rsukkerd
 *
 */
public interface IFactoredPSO {

	public Set<EffectClass> getIndependentEffectClasses();

	public IActionDescription getActionDescription(EffectClass effectClass) throws EffectClassNotFoundException;

}
