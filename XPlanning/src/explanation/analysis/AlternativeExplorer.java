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
	private DifferenceScaler mDiffScaler;

	/**
	 * Generate Pareto-optimal alternative policies that are immediate neighbors of the original solution policy. This
	 * kind of alternatives indicates the "inflection points" of the decision.
	 * 
	 * @param grbConnector
	 */
	public AlternativeExplorer(GRBConnector grbConnector) {
		this(grbConnector, null);
	}

	/**
	 * Generate Pareto-optimal alternative policies that are at some significant distance away from the the original
	 * solution policy on the n-dimensional cost space.
	 * 
	 * @param grbConnector
	 * @param diffScaler
	 *            : Difference scaler
	 */
	public AlternativeExplorer(GRBConnector grbConnector, DifferenceScaler diffScaler) {
		mDiffScaler = diffScaler;
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
		// QA value of the solution policy
		double currQAValue = policyInfo.getQAValue(qFunction);

		if (mDiffScaler == null) {
			// Hard constraint

			// Find a constraint-satisfying, optimal policy, if exists
			return computeHardConstraintSatisfyingAlternative(policyInfo.getXMDP(), qFunction, currQAValue);
		}

		// Soft constraint

		CostFunction costFunction = policyInfo.getXMDP().getCostFunction();

		// Create a new objective function with a demoted QA
		AdditiveCostFunction objectiveFunction = createNewObjective(costFunction, qFunction);

		// Set a new aspirational level of the QA; use this as a constraint for an alternative

		double attrCostFuncSlope = costFunction.getAttributeCostFunction(qFunction).getSlope();

		// Strict, hard upper/lower bound
		BOUND_TYPE hardBoundType = attrCostFuncSlope > 0 ? BOUND_TYPE.STRICT_UPPER_BOUND
				: BOUND_TYPE.STRICT_LOWER_BOUND;
		AttributeConstraint<IQFunction<?, ?>> attrHardConstraint = new AttributeConstraint<>(qFunction, hardBoundType,
				currQAValue);

		// Non-strict, soft upper/lower bound
		BOUND_TYPE softBoundType = attrCostFuncSlope > 0 ? BOUND_TYPE.UPPER_BOUND : BOUND_TYPE.LOWER_BOUND;

		// Weber scaling -- decrease or increase in value for improvement
		double softBoundValue = mDiffScaler.getSignificantImprovement(qFunction, currQAValue);

		// Penalty function for soft-constraint violation
		// FIXME
		double penaltyScalingConst = policyInfo.getObjectiveCost();
		IPenaltyFunction penaltyFunction = new QuadraticPenaltyFunction(penaltyScalingConst, 5);

		AttributeConstraint<IQFunction<?, ?>> attrSoftConstraint = new AttributeConstraint<>(qFunction, softBoundType,
				softBoundValue, penaltyFunction);

		// Find a constraint-satisfying, optimal policy (with soft constraint), if exists
		return mGRBConnector.generateOptimalPolicy(objectiveFunction, attrSoftConstraint, attrHardConstraint);
	}

	public PolicyInfo computeHardConstraintSatisfyingAlternative(XMDP xmdp, IQFunction<?, ?> qFunction,
			double qaValueConstraint) throws ExplicitModelParsingException, XMDPException, IOException, GRBException {
		CostFunction costFunction = xmdp.getCostFunction();

		// Create a new objective function with a demoted QA
		AdditiveCostFunction objectiveFunction = createNewObjective(costFunction, qFunction);

		// Set a new aspirational level of the QA; use this as a constraint for an alternative

		double attrCostFuncSlope = costFunction.getAttributeCostFunction(qFunction).getSlope();

		// Strict, hard upper/lower bound
		BOUND_TYPE hardBoundType = attrCostFuncSlope > 0 ? BOUND_TYPE.STRICT_UPPER_BOUND
				: BOUND_TYPE.STRICT_LOWER_BOUND;
		AttributeConstraint<IQFunction<?, ?>> attrHardConstraint = new AttributeConstraint<>(qFunction, hardBoundType,
				qaValueConstraint);

		// Find a constraint-satisfying, optimal policy, if exists
		return mGRBConnector.generateOptimalPolicy(objectiveFunction, attrHardConstraint);
	}

	/**
	 * Create a new objective function, where n-1 attributes have their original scaling constants, but the demoted
	 * attribute has a relatively very small scaling constant. The new objective function will have the same offset as
	 * that of the cost function.
	 * 
	 * If the planning problem is SSP, the offset of the new objective function ensures that all objective costs are
	 * positive, except in the goal states.
	 * 
	 * @param costFunction
	 *            : Cost function of the XMDP
	 * @param demotedQFunction
	 *            : QA function to be demoted in the new objective
	 * @return A new objective function with demoted QA
	 */
	private AdditiveCostFunction createNewObjective(CostFunction costFunction, IQFunction<?, ?> demotedQFunction) {
		AdditiveCostFunction objectiveFunction = new AdditiveCostFunction("cost_demoted_" + demotedQFunction.getName(),
				costFunction.getOffset());

		// The demoted QA will have a scaling constant of 1/10 of the smallest scaling constant of the remaining QAs
		AttributeCostFunction<IQFunction<IAction, ITransitionStructure<IAction>>> demotedAttrCostFunc = null;
		double minNonDemotedScalingConst = 1.0;

		// Add all non-demoted QAs to the new objective function
		for (AttributeCostFunction<IQFunction<IAction, ITransitionStructure<IAction>>> attrCostFunc : costFunction
				.getAttributeCostFunctions()) {

			if (!attrCostFunc.getQFunction().equals(demotedQFunction)) {
				// Non-demoted QA
				double scalingConst = costFunction.getScalingConstant(attrCostFunc);
				objectiveFunction.put(attrCostFunc, scalingConst);

				minNonDemotedScalingConst = Math.min(minNonDemotedScalingConst, scalingConst);
			} else {
				// Demoted QA
				demotedAttrCostFunc = attrCostFunc;
			}
		}

		// Add the demoted QA to the new objective function
		objectiveFunction.put(demotedAttrCostFunc, 0.1 * minNonDemotedScalingConst);
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
			if (mDiffScaler != null) {
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
		double softBound = mDiffScaler.getSignificantImprovement(qFunction, solnQAValue);
		return attrCostFuncSlope > 0 ? altQAValue <= softBound : altQAValue >= softBound;
	}
}
