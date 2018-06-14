package prismconnector;

import exceptions.VarNotFoundException;
import factors.IStateVarValue;
import factors.StateVar;
import mdp.State;
import metrics.IQFunction;
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
	public String buildMDPCostMinProperty(State goal, CostFunction costFunction) throws VarNotFoundException {
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
	 * @param qFunction
	 *            : QA function of the value to be constrained
	 * @param maxQValue
	 *            : Maximum value of the constrained QA
	 * @return multi(R{"{objective name}"}min=? [ C ], R{"{QA name}"}<={QA bound} [ C ], P>=1 [ F {goal predicate} ])
	 * @throws VarNotFoundException
	 */
	public String buildMDPConstrainedCostMinProperty(State goal, IAdditiveCostFunction objectiveFunction,
			IQFunction qFunction, double maxQValue) throws VarNotFoundException {
		StringBuilder builder = new StringBuilder();
		builder.append("multi(R{\"");
		builder.append(objectiveFunction.getName());
		builder.append("\"}min=? [ C ], ");
		builder.append("R{\"");
		builder.append(qFunction.getName());
		builder.append("\"}<=");
		builder.append(maxQValue);
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
	 *            : Goal of MDP
	 * @param qFunction
	 *            : QA function of the value to be queried
	 * @return R{"{QA name}"}=? [ F {goal predicate} ]
	 * @throws VarNotFoundException
	 */
	public String buildDTMCNumQueryProperty(State goal, IQFunction qFunction) throws VarNotFoundException {
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
	 *            : Goal of MDP
	 * @return R=? [ F {goal predicate} ]
	 * @throws VarNotFoundException
	 */
	public String buildDTMCRawRewardQueryProperty(State goal) throws VarNotFoundException {
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
	private String buildGoalPredicate(State goal) throws VarNotFoundException {
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
