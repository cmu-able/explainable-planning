package mdp;

import java.util.Set;

import exceptions.DiscriminantNotFoundException;
import exceptions.EffectClassNotFoundException;
import exceptions.VarNotFoundException;
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

	public Precondition getPrecondition();

	public Set<EffectClass> getIndependentEffectClasses();

	public IActionDescription getActionDescription(EffectClass effectClass) throws EffectClassNotFoundException;

	public DiscriminantClass getDiscriminantClass(StateVarDefinition<IStateVarValue> stateVarDef)
			throws VarNotFoundException;

	public Set<IStateVarValue> getPossibleImpact(StateVarDefinition<IStateVarValue> stateVarDef,
			Discriminant discriminant) throws VarNotFoundException, DiscriminantNotFoundException;
}
