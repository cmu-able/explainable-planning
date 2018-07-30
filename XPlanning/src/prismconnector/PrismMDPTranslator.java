package prismconnector;

import java.util.Set;

import factors.ActionDefinition;
import factors.IAction;
import language.exceptions.VarNotFoundException;
import language.exceptions.XMDPException;
import language.mdp.Discriminant;
import language.mdp.IActionDescription;
import language.mdp.ProbabilisticEffect;
import language.mdp.ProbabilisticTransition;
import language.mdp.XMDP;
import prismconnector.PrismTranslatorUtilities.PartialModuleCommandsBuilder;

public class PrismMDPTranslator {

	private XMDP mXMDP;
	private ValueEncodingScheme mEncodings;
	private PrismRewardTranslator mRewardTranslator;
	private PrismPropertyTranslator mPropertyTranslator;
	private PrismTranslatorUtilities mUtilities;

	public PrismMDPTranslator(XMDP xmdp, boolean threeParamRewards, PrismRewardType prismRewardType) {
		mXMDP = xmdp;
		if (threeParamRewards) {
			mEncodings = new ValueEncodingScheme(xmdp.getStateSpace(), xmdp.getActionSpace());
		} else {
			mEncodings = new ValueEncodingScheme(xmdp.getStateSpace());
		}
		mRewardTranslator = new PrismRewardTranslator(xmdp.getTransitionFunction(), mEncodings, prismRewardType);
		mPropertyTranslator = new PrismPropertyTranslator(mEncodings);
		mUtilities = new PrismTranslatorUtilities(mEncodings);
	}

	public ValueEncodingScheme getValueEncodingScheme() {
		return mEncodings;
	}

	public PrismRewardTranslator getPrismRewardTranslator() {
		return mRewardTranslator;
	}

	public PrismPropertyTranslator getPrismPropertyTransltor() {
		return mPropertyTranslator;
	}

	/**
	 * 
	 * @param withQAFunctions
	 *            : Whether or not to include QA functions in the MDP translation
	 * @return Prism model of this MDP, including constants' declarations, MDP model, a reward structure representing
	 *         the cost function, and optionally reward structure(s) representing the QA function(s).
	 * @throws XMDPException
	 */
	public String getMDPTranslation(boolean withQAFunctions) throws XMDPException {
		PartialModuleCommandsBuilder partialCommandsBuilder = new PartialModuleCommandsBuilder() {

			@Override
			public String buildPartialModuleCommands(IActionDescription<IAction> actionDescription)
					throws XMDPException {
				return buildMDPPartialModuleCommands(actionDescription);
			}
		};

		String constsDecl = mUtilities.buildConstsDecl(mXMDP.getStateSpace());
		String actionsDecl = mUtilities.buildConstsDecl(mXMDP.getActionSpace());
		String goalDecl = mUtilities.buildGoalDecl(mXMDP.getGoal());
		String modules = mUtilities.buildModules(mXMDP.getStateSpace(), mXMDP.getInitialState(), mXMDP.getActionSpace(),
				mXMDP.getTransitionFunction(), partialCommandsBuilder);
		String costStruct = mRewardTranslator.getCostFunctionTranslation(mXMDP.getCostFunction());

		StringBuilder builder = new StringBuilder();
		builder.append("mdp");
		builder.append("\n\n");
		builder.append(constsDecl);
		builder.append(actionsDecl);
		builder.append("\n");
		builder.append(goalDecl);
		builder.append("\n\n");
		builder.append(modules);
		builder.append("\n\n");
		builder.append(costStruct);

		if (withQAFunctions) {
			String qasRewards = mRewardTranslator.getQAFunctionsTranslation(mXMDP.getQSpace());
			builder.append("\n\n");
			builder.append(qasRewards);
		}

		return builder.toString();
	}

	/**
	 * 
	 * @return Goal reachability property of this MDP with cost minimization
	 * @throws VarNotFoundException
	 */
	public String getGoalPropertyTranslation() throws VarNotFoundException {
		return mPropertyTranslator.buildMDPCostMinProperty(mXMDP.getGoal(), mXMDP.getCostFunction());
	}

	/**
	 * Build partial commands of a module -- for MDP.
	 * 
	 * @param actionDescription
	 *            : Action description of an effect class (possibly merged)
	 * @return commands for updating a particular effect class of actionDescription
	 * @throws XMDPException
	 */
	private String buildMDPPartialModuleCommands(IActionDescription<IAction> actionDescription) throws XMDPException {
		ActionDefinition<IAction> actionDef = actionDescription.getActionDefinition();

		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for (IAction action : actionDef.getActions()) {
			Set<ProbabilisticTransition<IAction>> probTransitions = actionDescription
					.getProbabilisticTransitions(action);
			for (ProbabilisticTransition<IAction> probTrans : probTransitions) {
				Discriminant discriminant = probTrans.getDiscriminant();
				ProbabilisticEffect probEffect = probTrans.getProbabilisticEffect();
				String command = mUtilities.buildModuleCommand(action, discriminant, probEffect);
				if (!first) {
					builder.append("\n");
				} else {
					first = false;
				}
				builder.append(PrismTranslatorUtilities.INDENT);
				builder.append(command);
			}
		}
		return builder.toString();
	}

}
