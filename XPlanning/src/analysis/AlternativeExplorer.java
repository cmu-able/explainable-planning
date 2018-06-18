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

	private static final double COST_IMPROVE_STEP = 0.2;

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
		for (IQFunction qFunction : qFunctions) {
			AttributeCostFunction<IQFunction> attrCostFunc = costFunction.getAttributeCostFunction(qFunction);
			double currQAValue = mPrismConnector.getQAValue(mPolicy, qFunction);
			double currAttrCost = attrCostFunc.getCost(currQAValue);
			double improvedQAValue = attrCostFunc.inverse(currAttrCost - COST_IMPROVE_STEP);
			AttributeConstraint<IQFunction> attrConstraint = new AttributeConstraint<>(qFunction, improvedQAValue);
			AdditiveCostFunction objectiveFunc = new AdditiveCostFunction("cost_no_" + qFunction.getName());
			for (AttributeCostFunction<IQFunction> otherAttrCostFunc : costFunction) {
				IQFunction otherQFunc = otherAttrCostFunc.getQFunction();
				if (!otherQFunc.equals(qFunction)) {
					double scalingConst = costFunction.getScalingConstant(otherAttrCostFunc);
					objectiveFunc.put(otherAttrCostFunc.getQFunction(), otherAttrCostFunc, scalingConst);
				}
			}
			Policy alternative = mPrismConnector.generateOptimalPolicy(objectiveFunc, attrConstraint);
			alternatives.add(alternative);
		}
		return alternatives;
	}
}
