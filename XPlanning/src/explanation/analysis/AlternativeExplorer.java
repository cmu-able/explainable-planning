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
import language.objectives.IPenaltyFunction;
import language.objectives.QuadraticPenaltyFunction;
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
	private WeberScale mWeberScale;

	/**
	 * Generate Pareto-optimal alternative policies that are immediate neighbors of the original solution policy. This
	 * kind of alternatives indicates the "inflection points" of the decision.
	 * 
	 * @param prismConnector
	 * @param grbConnector
	 */
	public AlternativeExplorer(PrismConnector prismConnector, GRBConnector grbConnector) {
		this(null, prismConnector, grbConnector);
	}

	/**
	 * Generate Pareto-optimal alternative policies that are at some significant distance away from the the original
	 * solution policy on the n-dimensional cost space.
	 * 
	 * @param weberScale
	 *            : Weber-Fechner law of perception
	 * @param prismConnector
	 * @param grbConnector
	 */
	public AlternativeExplorer(WeberScale weberScale, PrismConnector prismConnector, GRBConnector grbConnector) {
		mWeberScale = weberScale;
		mPrismConnector = prismConnector;
		mGRBConnector = grbConnector;
		mCostFunction = prismConnector.getXMDP().getCostFunction();
	}

	/**
	 * Generate Pareto-optimal alternative policies. Each alternative policy has an improvement in at least 1 QA
	 * compared to the original solution policy.
	 * 
	 * @param policy
	 *            : Original solution policy
	 * @return Pareto-optimal alternative policies
	 * @throws XMDPException
	 * @throws PrismException
	 * @throws ResultParsingException
	 * @throws IOException
	 * @throws GRBException
	 * @throws InitialStateParsingException
	 */
	public Set<Policy> getParetoOptimalAlternatives(Policy policy) throws XMDPException, PrismException,
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
			Policy alternative = getParetoOptimalAlternative(policy, qFunction);

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

	/**
	 * Generate a Pareto-optimal alternative policy that has an improvement in the given QA compared to the original
	 * solution policy.
	 * 
	 * @param policy
	 *            : Original solution policy
	 * @param qFunction
	 *            : QA function to improve
	 * @return Pareto-optimal alternative policy
	 * @throws ResultParsingException
	 * @throws XMDPException
	 * @throws PrismException
	 * @throws IOException
	 * @throws ExplicitModelParsingException
	 * @throws GRBException
	 */
	public Policy getParetoOptimalAlternative(Policy policy, IQFunction<?, ?> qFunction) throws ResultParsingException,
			XMDPException, PrismException, IOException, ExplicitModelParsingException, GRBException {
		// Create a new objective function of n-1 attributes that excludes this QA
		AdditiveCostFunction objectiveFunction = createNewObjective(qFunction);

		// QA value of the solution policy
		double currQAValue = mPrismConnector.getQAValue(policy, qFunction);

		// Set a new aspirational level of the QA; use this as a constraint for an alternative

		// Strict, hard upper-bound
		AttributeConstraint<IQFunction<?, ?>> attrHardConstraint = new AttributeConstraint<>(qFunction, currQAValue,
				true);

		if (mWeberScale != null) {
			// Weber scaling
			double softUpperBound = mWeberScale.getSignificantImprovement(qFunction, currQAValue);

			// Non-strict, soft upper-bound
			double penaltyScalingConst = mPrismConnector.getCost(policy);
			IPenaltyFunction penaltyFunction = new QuadraticPenaltyFunction(penaltyScalingConst, 5);
			AttributeConstraint<IQFunction<?, ?>> attrSoftConstraint = new AttributeConstraint<>(qFunction,
					softUpperBound, penaltyFunction);

			// Find a constraint-satisfying, optimal policy (with soft constraint), if exists
			return mGRBConnector.generateOptimalPolicy(objectiveFunction, attrSoftConstraint, attrHardConstraint);
		} else {
			// Find a constraint-satisfying, optimal policy, if exists
			return mGRBConnector.generateOptimalPolicy(objectiveFunction, attrHardConstraint);
		}
	}

	/**
	 * Create a new objective function of n-1 attributes that excludes this QA
	 * 
	 * @param excludedQFunction
	 *            : QA function to be excluded from the objective
	 * @return (n-1)-attribute objective function
	 */
	private AdditiveCostFunction createNewObjective(IQFunction<?, ?> excludedQFunction) {
		AdditiveCostFunction objectiveFunction = new AdditiveCostFunction("cost_no_" + excludedQFunction.getName());

		for (IQFunction<IAction, ITransitionStructure<IAction>> otherQFunction : mCostFunction.getQFunctions()) {
			if (!otherQFunction.equals(excludedQFunction)) {
				AttributeCostFunction<IQFunction<IAction, ITransitionStructure<IAction>>> otherAttrCostFunc = mCostFunction
						.getAttributeCostFunction(otherQFunction);
				double scalingConst = mCostFunction.getScalingConstant(otherAttrCostFunc);
				objectiveFunction.put(otherAttrCostFunc.getQFunction(), otherAttrCostFunc, scalingConst);
			}
		}
		return objectiveFunction;
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
			if (mWeberScale != null) {
				// Check if the side-effect improvement is significant
				double softUpperBound = mWeberScale.getSignificantImprovement(qFunction, solnQAValue);
				if (altQAValue <= softUpperBound) {
					frontierIter.remove();
				}
			} else if (altQAValue < solnQAValue) {
				frontierIter.remove();
			}
		}
	}
}
