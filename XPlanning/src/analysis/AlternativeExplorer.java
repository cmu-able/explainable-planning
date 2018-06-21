package analysis;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
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
		Iterator<IQFunction> frontierIter = xmdp.getQFunctions().iterator();

		// Generate alternatives by improving each QA (one at a time) by some amount
		while (frontierIter.hasNext()) {
			IQFunction qFunction = frontierIter.next();

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
			if (alternative != null) {
				alternatives.add(alternative);
			}

			// For other QAs that have been improved as a side effect, remove them from the set of QAs to be explored
			update(frontierIter, alternative);
		}
		return alternatives;
	}

	private void update(Iterator<IQFunction> frontierIter, Policy alternative)
			throws QFunctionNotFoundException, ActionDefinitionNotFoundException, EffectClassNotFoundException,
			VarNotFoundException, IncompatibleVarException, ActionNotFoundException, DiscriminantNotFoundException,
			IncompatibleActionException, IncompatibleEffectClassException, IncompatibleDiscriminantClassException,
			AttributeNameNotFoundException, PrismException, ResultParsingException {
		while (frontierIter.hasNext()) {
			IQFunction qFunction = frontierIter.next();
			double solnQAValue = mPrismConnector.getQAValue(mPolicy, qFunction);
			double altQAValue = mPrismConnector.getQAValue(alternative, qFunction);
			if (altQAValue < solnQAValue) {
				frontierIter.remove();
			}
		}
	}
}
