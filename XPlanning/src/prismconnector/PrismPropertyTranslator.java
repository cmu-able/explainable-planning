package prismconnector;

import exceptions.VarNotFoundException;
import factors.IStateVarValue;
import factors.StateVar;
import mdp.StateVarTuple;
import metrics.IQFunction;
import objectives.AttributeConstraint;
import objectives.CostFunction;
import objectives.IAdditiveCostFunction;

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
	 * @return R{"cost"}min=? [ F {goal predicate} ]
	 * @throws VarNotFoundException
	 */
	public String buildMDPCostMinProperty(StateVarTuple goal, CostFunction costFunction) throws VarNotFoundException {
		StringBuilder builder = new StringBuilder();
		builder.append("R{\"");
		builder.append(costFunction.getName());
		builder.append("\"}min=? [ F ");
		String goalPredicate = buildGoalPredicate(goal);
		builder.append(goalPredicate);
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
	 * @return multi(R{"{objective name}"}min=? [ C ], R{"{QA name}"}<={QA bound} [ C ], P>=1 [ F {goal predicate} ])
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
		String goalPredicate = buildGoalPredicate(goal);
		builder.append(goalPredicate);
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
	 * @return R{"cost"}=? [ F {goal predicate} ]
	 * @throws VarNotFoundException
	 */
	public String buildDTMCCostQueryProperty(StateVarTuple goal, CostFunction costFunction)
			throws VarNotFoundException {
		StringBuilder builder = new StringBuilder();
		builder.append("R{\"");
		builder.append(costFunction.getName());
		builder.append("\"}=? [ F ");
		String goalPredicate = buildGoalPredicate(goal);
		builder.append(goalPredicate);
		builder.append(" ]");
		return builder.toString();
	}

	/**
	 * 
	 * @param goal
	 *            : Goal of the corresponding MDP
	 * @param qFunction
	 *            : QA function of the value to be queried
	 * @return R{"{QA name}"}=? [ F {goal predicate} ]
	 * @throws VarNotFoundException
	 */
	public String buildDTMCNumQueryProperty(StateVarTuple goal, IQFunction<?, ?> qFunction)
			throws VarNotFoundException {
		StringBuilder builder = new StringBuilder();
		builder.append("R{\"");
		builder.append(qFunction.getName());
		builder.append("\"}=? [ F ");
		String goalPredicate = buildGoalPredicate(goal);
		builder.append(goalPredicate);
		builder.append(" ]");
		return builder.toString();
	}

	/**
	 * 
	 * @param goal
	 *            : Goal of the corresponding MDP
	 * @return R=? [ F {goal predicate} ]
	 * @throws VarNotFoundException
	 */
	public String buildDTMCRawRewardQueryProperty(StateVarTuple goal) throws VarNotFoundException {
		StringBuilder builder = new StringBuilder();
		builder.append("R=? [ F ");
		String goalPredicate = buildGoalPredicate(goal);
		builder.append(goalPredicate);
		builder.append(" ]");
		return builder.toString();
	}

	/**
	 * 
	 * @param goal
	 *            : Goal of MDP
	 * @return {varName}={encoded int value} & ... & readyToCopy & barrier
	 * @throws VarNotFoundException
	 */
	private String buildGoalPredicate(StateVarTuple goal) throws VarNotFoundException {
		StringBuilder builder = new StringBuilder();
		boolean firstVar = true;
		for (StateVar<IStateVarValue> goalVar : goal) {
			Integer encodedValue = mEncodings.getEncodedIntValue(goalVar.getDefinition(), goalVar.getValue());
			if (!firstVar) {
				builder.append(" & ");
			} else {
				firstVar = false;
			}
			builder.append(goalVar.getName());
			builder.append("=");
			builder.append(encodedValue);
		}

		if (mEncodings.isThreeParamRewards()) {
			builder.append(" & readyToCopy");
		}

		builder.append(" & barrier");
		return builder.toString();
	}
}
