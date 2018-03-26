package mdp;

import java.util.Set;

import exceptions.EffectClassNotFoundException;
import factors.IStateVarValue;
import factors.StateVarDefinition;

/**
 * {@link IFactoredPSO} is an interface to a "factored" probabilistic STRIPS operator representation.
 * 
 * Reference: Using Abstractions for Decision-Theoretic Planning with Time Constraints, Boutilier & Dearden, 1994
 * 
 * @author rsukkerd
 *
 */
public interface IFactoredPSO {

	public Set<IStateVarValue> getApplicableValues(StateVarDefinition<IStateVarValue> stateVarDef);

	public Set<EffectClass> getIndependentEffectClasses();

	public IActionDescription getActionDescription(EffectClass effectClass) throws EffectClassNotFoundException;

}
