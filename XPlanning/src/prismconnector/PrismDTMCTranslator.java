package prismconnector;

import factors.IStateVarValue;
import factors.StateVar;
import mdp.State;
import mdp.XMDP;
import metrics.IQFunction;
import policy.Policy;

public class PrismDTMCTranslator {

	private Policy mPolicy;
	private XMDP mXMDP;
	private ValueEncodingScheme mEncodings;
	private boolean mThreeParamRewards;

	public PrismDTMCTranslator(Policy policy, XMDP xmdp, boolean threeParamRewards) {
		mPolicy = policy;
		mXMDP = xmdp;
		mEncodings = new ValueEncodingScheme(xmdp.getStateSpace(), xmdp.getActionSpace());
		mThreeParamRewards = threeParamRewards;
	}

	public String getDTMCTranslation() {
		StringBuilder builder = new StringBuilder();
		builder.append("dtmc");
		builder.append("\n");
		builder.append("module policy");
		// TODO
		builder.append("endmodule");
		return builder.toString();
	}

	public String getRewardsTranslation() {
		StringBuilder builder = new StringBuilder();
		builder.append("rewards");
		// TODO
		builder.append("endrewards");
		return builder.toString();
	}

	/**
	 * 
	 * @param qFunction
	 * @return R{"{objectiveName}"}=? [ F "{varName}={encoded int value} & ..." ]
	 */
	public String getObjectivePropertyTranslation(IQFunction qFunction) {
		State goal = mXMDP.getGoal();
		StringBuilder builder = new StringBuilder();
		builder.append("R{\"");
		builder.append(qFunction.getName());
		builder.append("\"}=? ");
		builder.append("[ F \"");
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
		builder.append("\" ]");
		return builder.toString();
	}
}
