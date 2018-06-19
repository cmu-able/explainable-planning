package analysis;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import exceptions.ActionDefinitionNotFoundException;
import exceptions.ActionNotFoundException;
import exceptions.AttributeNameNotFoundException;
import exceptions.DiscriminantNotFoundException;
import exceptions.EffectClassNotFoundException;
import exceptions.IncompatibleActionException;
import exceptions.IncompatibleDiscriminantClassException;
import exceptions.IncompatibleEffectClassException;
import exceptions.IncompatibleVarException;
import exceptions.QFunctionNotFoundException;
import exceptions.ResultParsingException;
import exceptions.VarNotFoundException;
import mdp.XMDP;
import metrics.IQFunction;
import objectives.AdditiveCostFunction;
import objectives.AttributeConstraint;
import objectives.AttributeCostFunction;
import objectives.CostFunction;
import policy.Policy;
import prism.PrismException;
import prismconnector.PrismConnector;

public class AlternativeExplorer {

	private PrismConnector mPrismConnector;
	private Policy mPolicy;

	public AlternativeExplorer(PrismConnector prismConnector, Policy policy) {
		mPrismConnector = prismConnector;
		mPolicy = policy;
	}

	public Set<Policy> getParetoOptimalAlternatives()
			throws QFunctionNotFoundException, ActionDefinitionNotFoundException, EffectClassNotFoundException,
			VarNotFoundException, IncompatibleVarException, ActionNotFoundException, DiscriminantNotFoundException,
			IncompatibleActionException, IncompatibleEffectClassException, IncompatibleDiscriminantClassException,
			AttributeNameNotFoundException, PrismException, ResultParsingException, IOException {
		Set<Policy> alternatives = new HashSet<>();
		XMDP xmdp = mPrismConnector.getXMDP();
		CostFunction costFunction = xmdp.getCostFunction();
		Set<IQFunction> qFunctions = xmdp.getQFunctions();

		// Generate alternatives by improving each QA (one at a time) by some amount
		for (IQFunction qFunction : qFunctions) {
			// QA value of the solution policy
			double currQAValue = mPrismConnector.getQAValue(mPolicy, qFunction);

			// Set a new aspirational level of the QA; use this as a constraint for an alternative
			AttributeConstraint<IQFunction> attrConstraint = new AttributeConstraint<>(qFunction, currQAValue, true);

			// Create a new objective function of n-1 attributes that excludes this QA
			AdditiveCostFunction objectiveFunc = new AdditiveCostFunction("cost_no_" + qFunction.getName());
			for (AttributeCostFunction<IQFunction> otherAttrCostFunc : costFunction) {
				if (!otherAttrCostFunc.getQFunction().equals(qFunction)) {
					double scalingConst = costFunction.getScalingConstant(otherAttrCostFunc);
					objectiveFunc.put(otherAttrCostFunc.getQFunction(), otherAttrCostFunc, scalingConst);
				}
			}

			// Find an alternative policy, if exists
			Policy alternative = mPrismConnector.generateOptimalPolicy(objectiveFunc, attrConstraint);

			// TODO: Check if an alternative policy exists
			alternatives.add(alternative);
		}
		return alternatives;
	}
}
