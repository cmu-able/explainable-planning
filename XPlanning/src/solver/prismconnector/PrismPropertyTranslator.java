package solver.prismconnector;

import language.exceptions.VarNotFoundException;
import language.mdp.StateVarTuple;
import language.metrics.IEvent;
import language.metrics.IQFunction;
import language.objectives.AttributeConstraint;
import language.objectives.CostFunction;
import language.objectives.IAdditiveCostFunction;

public class PrismPropertyTranslator {

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
	 * @return R{"cost"}min=? [ F {end predicate} ]
	 * @throws VarNotFoundException
	 */
	public String buildMDPCostMinProperty(StateVarTuple goal, CostFunction costFunction) throws VarNotFoundException {
		StringBuilder builder = new StringBuilder();
		builder.append("R{\"");
		builder.append(costFunction.getName());
		builder.append("\"}min=? [ F ");
		String endPredicate = buildEndPredicate(goal);
		builder.append(endPredicate);
		builder.append(" ]");
		return builder.toString();
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
		double upperBound = constraint.getExpectedTotalUpperBound();
		StringBuilder builder = new StringBuilder();
		builder.append("multi(R{\"");
		builder.append(objectiveFunction.getName());
		builder.append("\"}min=? [ C ], ");
		builder.append("R{\"");
		builder.append(qFunction.getName());
		builder.append("\"}");
		if (constraint.isStrictBound()) {
			// Multi-objective properties cannot use strict inequalities on P/R operators
			// Hack: Decrease the upper bound by 1% and use non-strict inequality
			double adjustedUpperBound = 0.99 * upperBound;
			builder.append("<=");
			builder.append(adjustedUpperBound);
		} else {
			builder.append("<=");
			builder.append(upperBound);
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
	 * @return R{"cost"}=? [ F {end predicate} ]
	 * @throws VarNotFoundException
	 */
	public String buildDTMCCostQueryProperty(StateVarTuple goal, CostFunction costFunction)
			throws VarNotFoundException {
		StringBuilder builder = new StringBuilder();
		builder.append("R{\"");
		builder.append(costFunction.getName());
		builder.append("\"}=? [ F ");
		String endPredicate = buildEndPredicate(goal);
		builder.append(endPredicate);
		builder.append(" ]");
		return builder.toString();
	}

	/**
	 * 
	 * @param goal
	 *            : Goal of the corresponding MDP
	 * @param qFunction
	 *            : QA function of the value to be queried
	 * @return R{"{QA name}"}=? [ F {end predicate} ]
	 * @throws VarNotFoundException
	 */
	public String buildDTMCNumQueryProperty(StateVarTuple goal, IQFunction<?, ?> qFunction)
			throws VarNotFoundException {
		StringBuilder builder = new StringBuilder();
		builder.append("R{\"");
		builder.append(qFunction.getName());
		builder.append("\"}=? [ F ");
		String endPredicate = buildEndPredicate(goal);
		builder.append(endPredicate);
		builder.append(" ]");
		return builder.toString();
	}

	/**
	 * 
	 * @param goal
	 *            : Goal of the corresponding MDP
	 * @param event
	 *            : Event to be counted
	 * @return R{"{event name}_count"}=? [ F {end predicate} ]
	 * @throws VarNotFoundException
	 */
	public String buildDTMCEventCountProperty(StateVarTuple goal, IEvent<?, ?> event) throws VarNotFoundException {
		String sanitizedEventName = PrismTranslatorUtils.sanitizeNameString(event.getName());
		StringBuilder builder = new StringBuilder();
		builder.append("R{\"");
		builder.append(sanitizedEventName);
		builder.append("_count\"}=? [ F ");
		String endPredicate = buildEndPredicate(goal);
		builder.append(endPredicate);
		builder.append(" ]");
		return builder.toString();
	}

	/**
	 * 
	 * @param goal
	 *            : Goal of the corresponding MDP
	 * @return R=? [ F {end predicate} ]
	 * @throws VarNotFoundException
	 */
	public String buildDTMCRawRewardQueryProperty(StateVarTuple goal) throws VarNotFoundException {
		StringBuilder builder = new StringBuilder();
		builder.append("R=? [ F ");
		String endPredicate = buildEndPredicate(goal);
		builder.append(endPredicate);
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
