package explanation.analysis;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import gurobi.GRBException;
import language.domain.metrics.IQFunction;
import language.domain.metrics.ITransitionStructure;
import language.domain.models.IAction;
import language.exceptions.XMDPException;
import language.mdp.XMDP;
import language.objectives.AdditiveCostFunction;
import language.objectives.AttributeConstraint;
import language.objectives.AttributeCostFunction;
import language.objectives.CostFunction;
import language.policy.Policy;
import prism.PrismException;
import solver.gurobiconnector.GRBConnector;
import solver.prismconnector.PrismConnector;
import solver.prismconnector.exceptions.ExplicitModelParsingException;
import solver.prismconnector.exceptions.InitialStateParsingException;
import solver.prismconnector.exceptions.ResultParsingException;

public class AlternativeExplorer {

	private PrismConnector mPrismConnector;
	private GRBConnector mGRBConnector;
	private CostFunction mCostFunction;

	public AlternativeExplorer(PrismConnector prismConnector, GRBConnector grbConnector) {
		mPrismConnector = prismConnector;
		mGRBConnector = grbConnector;
		mCostFunction = prismConnector.getXMDP().getCostFunction();
	}

	/**
	 * Generate alternative policies that are Pareto-optimal and are immediate neighbors of the original solution
	 * policy. This kind of alternatives indicates the "inflection points" of the decision.
	 * 
	 * @param policy
	 *            : Original solution policy
	 * @return Pareto-optimal alternative policies that are immediately next to the original solution policy.
	 * @throws XMDPException
	 * @throws PrismException
	 * @throws ResultParsingException
	 * @throws IOException
	 * @throws GRBException
	 * @throws InitialStateParsingException
	 */
	public Set<Policy> getParetoOptimalImmediateNeighbors(Policy policy) throws XMDPException, PrismException,
			ResultParsingException, IOException, ExplicitModelParsingException, GRBException {
		Set<Policy> alternatives = new HashSet<>();
		XMDP xmdp = mPrismConnector.getXMDP();

		// QAs to be explored
		Set<IQFunction<?, ?>> frontier = new HashSet<>();
		for (IQFunction<?, ?> qFunction : xmdp.getQSpace()) {
			frontier.add(qFunction);
		}

		// Generate alternatives by improving each QA (one at a time) to the next best value, if exists
		while (!frontier.isEmpty()) {
			Iterator<IQFunction<?, ?>> frontierIter = frontier.iterator();
			IQFunction<?, ?> qFunction = frontierIter.next();

			if (hasZeroAttributeCost(policy, qFunction)) {
				// Skip -- This QA already has its best value (0 attribute-cost) in the solution policy
				frontierIter.remove();
				continue;
			}

			// Find an alternative policy, if exists
			Policy alternative = getParetoOptimalImmediateNeighbor(policy, qFunction);

			// Removed explored QA
			frontierIter.remove();

			if (alternative != null) {
				alternatives.add(alternative);

				// For other QAs that have been improved as a side effect, remove them from the set of QAs to be
				// explored
				update(frontierIter, policy, alternative);
			}
		}
		return alternatives;
	}

	public Policy getParetoOptimalImmediateNeighbor(Policy policy, IQFunction<?, ?> qFunction)
			throws ResultParsingException, XMDPException, PrismException, IOException, ExplicitModelParsingException,
			GRBException {
		// QA value of the solution policy
		double currQAValue = mPrismConnector.getQAValue(policy, qFunction);

		// Set a new aspirational level of the QA; use this as a constraint for an alternative
		AttributeConstraint<IQFunction<?, ?>> attrConstraint = new AttributeConstraint<>(qFunction, currQAValue, true);

		// Create a new objective function of n-1 attributes that excludes this QA
		AdditiveCostFunction objectiveFunc = new AdditiveCostFunction("cost_no_" + qFunction.getName());

		for (IQFunction<IAction, ITransitionStructure<IAction>> otherQFunction : mCostFunction.getQFunctions()) {
			if (!otherQFunction.equals(qFunction)) {
				AttributeCostFunction<IQFunction<IAction, ITransitionStructure<IAction>>> otherAttrCostFunc = mCostFunction
						.getAttributeCostFunction(otherQFunction);
				double scalingConst = mCostFunction.getScalingConstant(otherAttrCostFunc);
				objectiveFunc.put(otherAttrCostFunc.getQFunction(), otherAttrCostFunc, scalingConst);
			}
		}

		// Find a constraint-satisfying, optimal policy, if exists
		return mGRBConnector.generateOptimalPolicy(objectiveFunc, attrConstraint);
	}

	private boolean hasZeroAttributeCost(Policy policy, IQFunction<?, ?> qFunction)
			throws ResultParsingException, XMDPException, PrismException {
		double currQAValue = mPrismConnector.getQAValue(policy, qFunction);
		AttributeCostFunction<?> attrCostFunc = mCostFunction.getAttributeCostFunction(qFunction);
		double currQACost = attrCostFunc.getCost(currQAValue);
		return currQACost == 0;
	}

	private void update(Iterator<IQFunction<?, ?>> frontierIter, Policy policy, Policy alternative)
			throws XMDPException, PrismException, ResultParsingException {
		while (frontierIter.hasNext()) {
			IQFunction<?, ?> qFunction = frontierIter.next();
			double solnQAValue = mPrismConnector.getQAValue(policy, qFunction);
			double altQAValue = mPrismConnector.getQAValue(alternative, qFunction);

			// If this QA of the alternative has been improved as a side effect, remove it from the QAs to be explored
			if (altQAValue < solnQAValue) {
				frontierIter.remove();
			}
		}
	}
}
