package prismconnector;

import exceptions.VarNotFoundException;
import factors.IStateVarValue;
import factors.StateVar;
import mdp.State;
import metrics.IQFunction;

public class PrismPropertyTranslator {

	private ValueEncodingScheme mEncodings;
	private boolean mThreeParamRewards;

	public PrismPropertyTranslator(ValueEncodingScheme encodings, boolean threeParamRewards) {
		mEncodings = encodings;
		mThreeParamRewards = threeParamRewards;
	}

	/**
	 * 
	 * @param goal
	 * @return R{"cost"}min=? [ F {goal predicate} ]
	 * @throws VarNotFoundException
	 */
	public String buildMDPCostMinProperty(State goal) throws VarNotFoundException {
		StringBuilder builder = new StringBuilder();
		builder.append("R{\"");
		builder.append(PrismRewardTranslatorUtilities.COST_STRUCTURE_NAME);
		builder.append("\"}min=? [ F ");
		String goalPredicate = buildGoalPredicate(goal);
		builder.append(goalPredicate);
		builder.append(" ]");
		return builder.toString();
	}

	/**
	 * 
	 * @param goal
	 * @param qFunction
	 * @param maxQValue
	 * @return multi(R{"cost_no_{QA name}"}min=? [ C ], R{"{QA name}"}<={QA bound} [ C ], P>=1 [ F {goal predicate} ])
	 * @throws VarNotFoundException
	 */
	public String buildMDPConstrainedCostMinProperty(State goal, IQFunction qFunction, double maxQValue)
			throws VarNotFoundException {
		StringBuilder builder = new StringBuilder();
		builder.append("multi(R{\"");
		builder.append(PrismRewardTranslatorUtilities.COST_STRUCTURE_NAME);
		builder.append("_no_");
		builder.append(qFunction.getName());
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
	 * @param qFunction
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

		if (mThreeParamRewards) {
			builder.append(" & readyToCopy");
		}

		builder.append(" & barrier");
		return builder.toString();
	}
}
