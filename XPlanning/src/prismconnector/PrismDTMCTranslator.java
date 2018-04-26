package prismconnector;

import dtmc.XDTMC;
import exceptions.ActionDefinitionNotFoundException;
import exceptions.ActionNotFoundException;
import exceptions.DiscriminantNotFoundException;
import exceptions.EffectClassNotFoundException;
import exceptions.IncompatibleActionException;
import exceptions.IncompatibleVarException;
import exceptions.VarNotFoundException;
import factors.IStateVarValue;
import factors.StateVar;
import mdp.State;
import mdp.XMDP;
import metrics.IQFunction;
import policy.Policy;

public class PrismDTMCTranslator {

	private XDTMC mXDTMC;
	private ValueEncodingScheme mEncodings;
	private boolean mThreeParamRewards;

	public PrismDTMCTranslator(XMDP xmdp, Policy policy, boolean threeParamRewards)
			throws ActionDefinitionNotFoundException, EffectClassNotFoundException, VarNotFoundException,
			IncompatibleVarException, ActionNotFoundException, DiscriminantNotFoundException,
			IncompatibleActionException {
		mXDTMC = new XDTMC(xmdp, policy);
		mThreeParamRewards = threeParamRewards;
		if (threeParamRewards) {
			mEncodings = new ValueEncodingScheme(xmdp.getStateSpace(), xmdp.getActionSpace());
		} else {
			mEncodings = new ValueEncodingScheme(xmdp.getStateSpace());
		}
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
	 * @throws VarNotFoundException
	 */
	public String getObjectivePropertyTranslation(IQFunction qFunction) throws VarNotFoundException {
		State goal = mXDTMC.getXMDP().getGoal();
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
