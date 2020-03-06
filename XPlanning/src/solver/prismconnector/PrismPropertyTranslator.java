package solver.prismconnector;

import language.domain.metrics.IEvent;
import language.domain.metrics.IQFunction;
import language.exceptions.VarNotFoundException;
import language.mdp.StateVarTuple;
import language.objectives.AttributeConstraint;
import language.objectives.AttributeConstraint.BOUND_TYPE;
import language.objectives.AttributeCostFunction;
import language.objectives.CostCriterion;
import language.objectives.CostFunction;
import language.objectives.IAdditiveCostFunction;
import solver.common.NonStrictConstraint;

public class PrismPropertyTranslator {

	private static final String DTMC_REWARD_QUERY = "=?";
	private static final String MDP_MIN_REWARD_QUERY = "min=?";

	private ValueEncodingScheme mEncodings;

	public PrismPropertyTranslator(ValueEncodingScheme encodings) {
		mEncodings = encodings;
	}

	/**
	 * 
	 * @param goal
	 *            : Goal of MDP
	 * @param costFunction
	 *            : Cost function of MDP
	 * @param costCriterion
	 *            : Cost criterion of MDP
	 * @return R{"cost"}min=? [ F {end predicate} ] for SSP; R{"cost"}min=? [ S ] for average-cost MDP
	 * @throws VarNotFoundException
	 */
	public String buildMDPCostMinProperty(StateVarTuple goal, CostFunction costFunction, CostCriterion costCriterion)
			throws VarNotFoundException {
		String sanitizedCostName = PrismTranslatorUtils.sanitizeNameString(costFunction.getName());
		return buildRewardQueryProperty(sanitizedCostName, MDP_MIN_REWARD_QUERY, goal, costCriterion);
	}

	/**
	 * 
	 * @param goal
	 *            : Goal of MDP
	 * @param objectiveFunction
	 *            : Objective function to be minimized, which does not contain the constrained QA function
	 * @param constraint
	 *            : Constraint on the expected total QA value
	 * @return multi(R{"{objective name}"}min=? [ C ], R{"{QA name}"}<={QA bound} [ C ], P>=1 [ F {end predicate} ])
	 * @throws VarNotFoundException
	 */
	public String buildMDPConstrainedMinProperty(StateVarTuple goal, IAdditiveCostFunction objectiveFunction,
			AttributeConstraint<? extends IQFunction<?, ?>> constraint) throws VarNotFoundException {
		IQFunction<?, ?> qFunction = constraint.getQFunction();

		// Multi-objective properties cannot use strict inequalities on P/R operators
		// Hack: Decrease the upper bound by 1% and use non-strict inequality
		NonStrictConstraint nonStrictConstraint = new NonStrictConstraint(constraint, 0.99);

		StringBuilder builder = new StringBuilder();
		builder.append("multi(R{\"");
		builder.append(objectiveFunction.getName());
		builder.append("\"}min=? [ C ], ");
		builder.append("R{\"");
		builder.append(qFunction.getName());
		builder.append("\"}");
		if (nonStrictConstraint.getBoundType() == BOUND_TYPE.UPPER_BOUND) {
			builder.append("<=");
			builder.append(nonStrictConstraint.getBoundValue());
		} else {
			builder.append(">=");
			builder.append(nonStrictConstraint.getBoundValue());
		}
		builder.append(" [ C ], ");
		builder.append("P>=1 [ F ");
		String endPredicate = buildEndPredicate(goal);
		builder.append(endPredicate);
		builder.append(" ]");
		builder.append(")");
		return builder.toString();
	}

	/**
	 * 
	 * @param goal
	 *            : Goal of the corresponding MDP
	 * @param costFunction
	 *            : Cost function of the corresponding MDP
	 * @param costCriterion
	 *            : Cost criterion of the corresponding MDP
	 * @return R{"cost"}=? [ F {end predicate} ] for SSP; R{"cost"}=? [ S ] for average-cost MDP
	 * @throws VarNotFoundException
	 */
	public String buildDTMCCostQueryProperty(StateVarTuple goal, CostFunction costFunction, CostCriterion costCriterion)
			throws VarNotFoundException {
		String sanitizedCostName = PrismTranslatorUtils.sanitizeNameString(costFunction.getName());
		return buildRewardQueryProperty(sanitizedCostName, DTMC_REWARD_QUERY, goal, costCriterion);
	}

	/**
	 * 
	 * @param goal
	 *            : Goal of the corresponding MDP
	 * @param attrCostFunction
	 *            : Single-attribute cost function of a QA
	 * @param costCriterion
	 *            : Cost criterion of the corresponding MDP
	 * @return R{"cost_{QA name}"}=? [ F {end predicate} ] for SSP; R{"cost_{QA name}"}=? [ S ] for average-cost MDP
	 * @throws VarNotFoundException
	 */
	public String buildDTMCQACostQueryProperty(StateVarTuple goal, AttributeCostFunction<?> attrCostFunction,
			CostCriterion costCriterion) throws VarNotFoundException {
		String sanitizedQACostName = PrismTranslatorUtils.sanitizeNameString(attrCostFunction.getName());
		return buildRewardQueryProperty(sanitizedQACostName, DTMC_REWARD_QUERY, goal, costCriterion);
	}

	/**
	 * 
	 * @param goal
	 *            : Goal of the corresponding MDP
	 * @param qFunction
	 *            : QA function of the value to be queried
	 * @param costCriterion
	 *            : Cost criterion of the corresponding MDP
	 * @return R{"{QA name}"}=? [ F {end predicate} ] for SSP; R{"{QA name}"}=? [ S ] for average-cost MDP
	 * @throws VarNotFoundException
	 */
	public String buildDTMCNumQueryProperty(StateVarTuple goal, IQFunction<?, ?> qFunction, CostCriterion costCriterion)
			throws VarNotFoundException {
		String sanitizedQAName = PrismTranslatorUtils.sanitizeNameString(qFunction.getName());
		return buildRewardQueryProperty(sanitizedQAName, DTMC_REWARD_QUERY, goal, costCriterion);
	}

	/**
	 * 
	 * @param goal
	 *            : Goal of the corresponding MDP
	 * @param event
	 *            : Event to be counted
	 * @param costCriterion
	 *            : Cost criterion of the corresponding MDP
	 * @return R{"{event name}_count"}=? [ F {end predicate} ] for SSP; R{"{event name}_count"}=? [ S ] for average-cost
	 *         MDP
	 * @throws VarNotFoundException
	 */
	public String buildDTMCEventCountProperty(StateVarTuple goal, IEvent<?, ?> event, CostCriterion costCriterion)
			throws VarNotFoundException {
		String sanitizedEventName = PrismTranslatorUtils.sanitizeNameString(event.getName());
		return buildRewardQueryProperty(sanitizedEventName + "_count", DTMC_REWARD_QUERY, goal, costCriterion);
	}

	/**
	 * 
	 * @param goal
	 *            : Goal of the corresponding MDP
	 * @param costCriterion
	 *            : Cost criterion of the corresponding MDP
	 * @return R=? [ F {end predicate} ] for SSP; R=? [ S ] for average-cost MDP
	 * @throws VarNotFoundException
	 */
	public String buildDTMCRawRewardQueryProperty(StateVarTuple goal, CostCriterion costCriterion)
			throws VarNotFoundException {
		return buildRewardQueryProperty(null, DTMC_REWARD_QUERY, goal, costCriterion);
	}

	/**
	 * 
	 * @param queryState
	 *            : State for which to compute reachability probability
	 * @return P=? [ F {query state predicate} ]
	 * @throws VarNotFoundException
	 */
	public String buildDTMCReachabilityQueryProperty(StateVarTuple queryState) throws VarNotFoundException {
		String queryStateExpr = PrismTranslatorUtils.buildExpression(queryState, mEncodings);

		StringBuilder builder = new StringBuilder();
		builder.append("P=? [ F ");
		builder.append(queryStateExpr);
		builder.append(" ]");
		return builder.toString();
	}

	private String buildRewardQueryProperty(String rewardName, String query, StateVarTuple goal,
			CostCriterion costCriterion) throws VarNotFoundException {
		StringBuilder builder = new StringBuilder();
		builder.append("R");

		if (rewardName != null) {
			builder.append("{\"");
			builder.append(rewardName);
			builder.append("\"}");
		}

		// =? for DTMC or min=? for MDP
		builder.append(query);
		builder.append(" [ ");

		if (costCriterion == CostCriterion.TOTAL_COST) {
			String endPredicate = buildEndPredicate(goal);
			builder.append("F ");
			builder.append(endPredicate);
		} else if (costCriterion == CostCriterion.AVERAGE_COST) {
			builder.append("S");
		}

		builder.append(" ]");
		return builder.toString();
	}

	/**
	 * 
	 * @param goal
	 *            : Goal of MDP
	 * @return {varName}={value OR encoded int value} & ... & !computeGo & barrier
	 * @throws VarNotFoundException
	 */
	private String buildEndPredicate(StateVarTuple goal) throws VarNotFoundException {
		String goalExpr = PrismTranslatorUtils.buildExpression(goal, mEncodings);

		StringBuilder builder = new StringBuilder();
		builder.append(goalExpr);
		builder.append(" & !computeGo & barrier");
		return builder.toString();
	}

}
