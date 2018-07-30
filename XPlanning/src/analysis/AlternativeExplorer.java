package analysis;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import language.exceptions.ActionDefinitionNotFoundException;
import language.exceptions.ActionNotFoundException;
import language.exceptions.AttributeNameNotFoundException;
import language.exceptions.DiscriminantNotFoundException;
import language.exceptions.EffectClassNotFoundException;
import language.exceptions.IncompatibleActionException;
import language.exceptions.IncompatibleEffectClassException;
import language.exceptions.IncompatibleVarException;
import language.exceptions.QFunctionNotFoundException;
import language.exceptions.VarNotFoundException;
import language.exceptions.XMDPException;
import language.mdp.XMDP;
import language.metrics.IQFunction;
import language.metrics.IQFunctionDomain;
import language.objectives.AdditiveCostFunction;
import language.objectives.AttributeConstraint;
import language.objectives.AttributeCostFunction;
import language.objectives.CostFunction;
import language.policy.Policy;
import language.qfactors.IAction;
import prism.PrismException;
import prismconnector.PrismConnector;
import prismconnector.exceptions.ResultParsingException;

public class AlternativeExplorer {

	private PrismConnector mPrismConnector;
	private Policy mPolicy;

	public AlternativeExplorer(PrismConnector prismConnector, Policy policy) {
		mPrismConnector = prismConnector;
		mPolicy = policy;
	}

	/**
	 * Generate alternative policies that are Pareto-optimal and are immediate neighbors of the original solution
	 * policy. This kind of alternatives indicates the "inflection points" of the decision.
	 * 
	 * @return Pareto-optimal alternative policies that are immediately next to the original solution policy.
	 * @throws QFunctionNotFoundException
	 * @throws ActionDefinitionNotFoundException
	 * @throws EffectClassNotFoundException
	 * @throws VarNotFoundException
	 * @throws IncompatibleVarException
	 * @throws ActionNotFoundException
	 * @throws DiscriminantNotFoundException
	 * @throws IncompatibleActionException
	 * @throws IncompatibleEffectClassException
	 * @throws IncompatibleDiscriminantClassException
	 * @throws AttributeNameNotFoundException
	 * @throws PrismException
	 * @throws ResultParsingException
	 * @throws IOException
	 */
	public Set<Policy> getParetoOptimalImmediateNeighbors()
			throws XMDPException, PrismException, ResultParsingException, IOException {
		Set<Policy> alternatives = new HashSet<>();
		XMDP xmdp = mPrismConnector.getXMDP();
		CostFunction costFunction = xmdp.getCostFunction();

		// QAs to be explored
		Iterator<IQFunction<IAction, IQFunctionDomain<IAction>>> frontierIter = xmdp.getQSpace().iterator();

		// Generate alternatives by improving each QA (one at a time) to the next best value, if exists
		while (frontierIter.hasNext()) {
			IQFunction<IAction, IQFunctionDomain<IAction>> qFunction = frontierIter.next();
			frontierIter.remove();

			// QA value of the solution policy
			double currQAValue = mPrismConnector.getQAValue(mPolicy, qFunction);

			// Set a new aspirational level of the QA; use this as a constraint for an alternative
			AttributeConstraint<IQFunction<IAction, IQFunctionDomain<IAction>>> attrConstraint = new AttributeConstraint<>(
					qFunction, currQAValue, true);

			// Create a new objective function of n-1 attributes that excludes this QA
			AdditiveCostFunction objectiveFunc = new AdditiveCostFunction("cost_no_" + qFunction.getName());

			for (IQFunction<IAction, IQFunctionDomain<IAction>> otherQFunction : costFunction.getQFunctions()) {
				if (!otherQFunction.equals(qFunction)) {
					AttributeCostFunction<IQFunction<IAction, IQFunctionDomain<IAction>>> otherAttrCostFunc = costFunction
							.getAttributeCostFunction(otherQFunction);
					double scalingConst = costFunction.getScalingConstant(otherAttrCostFunc);
					objectiveFunc.put(otherAttrCostFunc.getQFunction(), otherAttrCostFunc, scalingConst);
				}
			}

			// Find an alternative policy, if exists
			Policy alternative = mPrismConnector.generateOptimalPolicy(objectiveFunc, attrConstraint);

			if (alternative != null) {
				alternatives.add(alternative);

				// For other QAs that have been improved as a side effect, remove them from the set of QAs to be
				// explored
				update(frontierIter, alternative);
			}
		}
		return alternatives;
	}

	private <E extends IAction, T extends IQFunctionDomain<E>> void update(Iterator<IQFunction<E, T>> frontierIter,
			Policy alternative) throws XMDPException, PrismException, ResultParsingException {
		while (frontierIter.hasNext()) {
			IQFunction<E, T> qFunction = frontierIter.next();
			double solnQAValue = mPrismConnector.getQAValue(mPolicy, qFunction);
			double altQAValue = mPrismConnector.getQAValue(alternative, qFunction);

			// If this QA of the alternative has been improved as a side effect, remove it from the QAs to be explored
			if (altQAValue < solnQAValue) {
				frontierIter.remove();
			}
		}
	}
}
