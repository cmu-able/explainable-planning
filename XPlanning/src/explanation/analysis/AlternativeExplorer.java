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
import language.objectives.AttributeConstraint.BOUND_TYPE;
import language.objectives.AttributeCostFunction;
import language.objectives.CostFunction;
import language.objectives.IPenaltyFunction;
import language.objectives.QuadraticPenaltyFunction;
import solver.gurobiconnector.GRBConnector;
import solver.prismconnector.exceptions.ExplicitModelParsingException;

public class AlternativeExplorer {

	private GRBConnector mGRBConnector;
	private WeberScale mWeberScale;

	/**
	 * Generate Pareto-optimal alternative policies that are immediate neighbors of the original solution policy. This
	 * kind of alternatives indicates the "inflection points" of the decision.
	 * 
	 * @param grbConnector
	 */
	public AlternativeExplorer(GRBConnector grbConnector) {
		this(null, grbConnector);
	}

	/**
	 * Generate Pareto-optimal alternative policies that are at some significant distance away from the the original
	 * solution policy on the n-dimensional cost space.
	 * 
	 * @param weberScale
	 *            : Weber-Fechner law of perception
	 * @param grbConnector
	 */
	public AlternativeExplorer(WeberScale weberScale, GRBConnector grbConnector) {
		mWeberScale = weberScale;
		mGRBConnector = grbConnector;
	}

	/**
	 * Generate Pareto-optimal alternative policies. Each alternative policy has an improvement in at least 1 QA
	 * compared to the original solution policy.
	 * 
	 * @param policyInfo
	 *            : Original solution policy information
	 * @return Pareto-optimal alternative policies
	 * @throws XMDPException
	 * @throws IOException
	 * @throws ExplicitModelParsingException
	 * @throws GRBException
	 */
	public Set<PolicyInfo> getParetoOptimalAlternatives(PolicyInfo policyInfo)
			throws XMDPException, IOException, ExplicitModelParsingException, GRBException {
		Set<PolicyInfo> alternatives = new HashSet<>();
		XMDP xmdp = policyInfo.getXMDP();

		// QAs to be explored
		Set<IQFunction<?, ?>> frontier = new HashSet<>();
		for (IQFunction<?, ?> qFunction : xmdp.getQSpace()) {
			frontier.add(qFunction);
		}

		// Generate alternatives by improving each QA (one at a time) to the next best value, if exists
		while (!frontier.isEmpty()) {
			Iterator<IQFunction<?, ?>> frontierIter = frontier.iterator();
			IQFunction<?, ?> qFunction = frontierIter.next();

			if (hasZeroAttributeCost(policyInfo, qFunction)) {
				// Skip -- This QA already has its best value (0 attribute-cost) in the solution policy
				frontierIter.remove();
				continue;
			}

			// Find an alternative policy, if exists
			PolicyInfo alternativeInfo = getParetoOptimalAlternative(policyInfo, qFunction);

			// Removed explored QA
			frontierIter.remove();

			if (alternativeInfo != null) {
				alternatives.add(alternativeInfo);

				// For other QAs that have been improved as a side effect, remove them from the set of QAs to be
				// explored
				update(frontierIter, policyInfo, alternativeInfo);
			}
		}
		return alternatives;
	}

	/**
	 * Generate a Pareto-optimal alternative policy that has an improvement in the given QA compared to the original
	 * solution policy.
	 * 
	 * @param policyInfo
	 *            : Original solution policy information
	 * @param qFunction
	 *            : QA function to improve
	 * @return Pareto-optimal alternative policy
	 * @throws XMDPException
	 * @throws IOException
	 * @throws ExplicitModelParsingException
	 * @throws GRBException
	 */
	public PolicyInfo getParetoOptimalAlternative(PolicyInfo policyInfo, IQFunction<?, ?> qFunction)
			throws XMDPException, IOException, ExplicitModelParsingException, GRBException {
		CostFunction costFunction = policyInfo.getXMDP().getCostFunction();

		// Create a new objective function of n-1 attributes that excludes this QA
		AdditiveCostFunction objectiveFunction = createNewObjective(costFunction, qFunction);

		// QA value of the solution policy
		double currQAValue = policyInfo.getQAValue(qFunction);

		// Set a new aspirational level of the QA; use this as a constraint for an alternative

		double attrCostFuncSlope = costFunction.getAttributeCostFunction(qFunction).getSlope();

		// Strict, hard upper/lower bound
		BOUND_TYPE hardBoundType = attrCostFuncSlope > 0 ? BOUND_TYPE.STRICT_UPPER_BOUND
				: BOUND_TYPE.STRICT_LOWER_BOUND;
		AttributeConstraint<IQFunction<?, ?>> attrHardConstraint = new AttributeConstraint<>(qFunction, hardBoundType,
				currQAValue);

		if (mWeberScale != null) {
			// Non-strict, soft upper/lower bound
			BOUND_TYPE softBoundType = attrCostFuncSlope > 0 ? BOUND_TYPE.UPPER_BOUND : BOUND_TYPE.LOWER_BOUND;

			// Weber scaling -- decrease or increase in value for improvement
			double softBoundValue = attrCostFuncSlope > 0 ? mWeberScale.getSignificantDecrease(qFunction, currQAValue)
					: mWeberScale.getSignificantIncrease(qFunction, currQAValue);

			// Penalty function for soft-constraint violation
			// FIXME
			double penaltyScalingConst = policyInfo.getObjectiveCost();
			IPenaltyFunction penaltyFunction = new QuadraticPenaltyFunction(penaltyScalingConst, 5);

			AttributeConstraint<IQFunction<?, ?>> attrSoftConstraint = new AttributeConstraint<>(qFunction,
					softBoundType, softBoundValue, penaltyFunction);

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
	 * @param costFunction
	 *            : Cost function of the XMDP
	 * @param excludedQFunction
	 *            : QA function to be excluded from the objective
	 * @return (n-1)-attribute objective function
	 */
	private AdditiveCostFunction createNewObjective(CostFunction costFunction, IQFunction<?, ?> excludedQFunction) {
		AdditiveCostFunction objectiveFunction = new AdditiveCostFunction("cost_no_" + excludedQFunction.getName());

		for (IQFunction<IAction, ITransitionStructure<IAction>> otherQFunction : costFunction.getQFunctions()) {
			if (!otherQFunction.equals(excludedQFunction)) {
				AttributeCostFunction<IQFunction<IAction, ITransitionStructure<IAction>>> otherAttrCostFunc = costFunction
						.getAttributeCostFunction(otherQFunction);
				double scalingConst = costFunction.getScalingConstant(otherAttrCostFunc);
				objectiveFunction.put(otherAttrCostFunc, scalingConst);
			}
		}
		return objectiveFunction;
	}

	private boolean hasZeroAttributeCost(PolicyInfo policyInfo, IQFunction<?, ?> qFunction) {
		double currQAValue = policyInfo.getQAValue(qFunction);
		CostFunction costFunction = policyInfo.getXMDP().getCostFunction();
		AttributeCostFunction<?> attrCostFunc = costFunction.getAttributeCostFunction(qFunction);
		double currQACost = attrCostFunc.getCost(currQAValue);
		return currQACost == 0;
	}

	private void update(Iterator<IQFunction<?, ?>> frontierIter, PolicyInfo policyInfo, PolicyInfo alternativeInfo) {
		CostFunction costFunction = policyInfo.getXMDP().getCostFunction();

		while (frontierIter.hasNext()) {
			IQFunction<?, ?> qFunction = frontierIter.next();
			double attrCostFuncSlope = costFunction.getAttributeCostFunction(qFunction).getSlope();

			double solnQAValue = policyInfo.getQAValue(qFunction);
			double altQAValue = alternativeInfo.getQAValue(qFunction);

			// If this QA of the alternative has been improved as a side effect, remove it from the QAs to be explored
			if (mWeberScale != null) {
				// Check if the side-effect improvement is significant
				if (hasSignificantImprovement(qFunction, attrCostFuncSlope, solnQAValue, altQAValue)) {
					frontierIter.remove();
				}
			} else if ((attrCostFuncSlope > 0 && altQAValue < solnQAValue)
					|| (attrCostFuncSlope < 0 && altQAValue > solnQAValue)) {
				frontierIter.remove();
			}
		}
	}

	private boolean hasSignificantImprovement(IQFunction<?, ?> qFunction, double attrCostFuncSlope, double solnQAValue,
			double altQAValue) {
		if (attrCostFuncSlope > 0) {
			// Decrease in value for improvement
			double softUpperBound = mWeberScale.getSignificantDecrease(qFunction, solnQAValue);
			return altQAValue <= softUpperBound;
		} else {
			// Increase in value for improvement
			double softLowerBound = mWeberScale.getSignificantIncrease(qFunction, solnQAValue);
			return altQAValue >= softLowerBound;
		}
	}
}
