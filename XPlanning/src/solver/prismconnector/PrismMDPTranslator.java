package solver.prismconnector;

import java.util.Set;

import language.domain.models.ActionDefinition;
import language.domain.models.IAction;
import language.exceptions.VarNotFoundException;
import language.exceptions.XMDPException;
import language.mdp.Discriminant;
import language.mdp.IActionDescription;
import language.mdp.ProbabilisticEffect;
import language.mdp.ProbabilisticTransition;
import language.mdp.XMDP;
import language.objectives.CostCriterion;
import solver.prismconnector.PrismTranslatorHelper.ActionFilter;
import solver.prismconnector.PrismTranslatorHelper.PartialModuleCommandsBuilder;

public class PrismMDPTranslator {

	private XMDP mXMDP;
	private ValueEncodingScheme mEncodings;
	private ActionFilter mActionFilter;
	private PrismRewardTranslator mRewardTranslator;
	private PrismPropertyTranslator mPropertyTranslator;
	private PrismTranslatorHelper mHelper;

	public PrismMDPTranslator(XMDP xmdp) {
		mXMDP = xmdp;
		mEncodings = new ValueEncodingScheme(xmdp.getStateSpace(), xmdp.getActionSpace(), xmdp.getQSpace(),
				xmdp.getCostFunction());
		mActionFilter = action -> mXMDP.getActionSpace().contains(action);
		mRewardTranslator = new PrismRewardTranslator(xmdp.getTransitionFunction(), mEncodings, mActionFilter);
		mPropertyTranslator = new PrismPropertyTranslator(mEncodings);
		mHelper = new PrismTranslatorHelper(mEncodings);
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

		String constsDecl = mHelper.buildConstsDecl(mXMDP.getStateSpace());

		String modules = mHelper.buildModules(mXMDP.getStateSpace(), mXMDP.getInitialState(), mXMDP.getActionSpace(),
				mXMDP.getTransitionFunction(), partialCommandsBuilder, mActionFilter);
		String costStruct = mRewardTranslator.getCostFunctionTranslation(mXMDP.getCostFunction());

		StringBuilder builder = new StringBuilder();
		builder.append("mdp");
		builder.append("\n\n");
		builder.append(constsDecl);
		builder.append("\n\n");

		if (mXMDP.getGoal() != null) {
			String goalDecl = mHelper.buildGoalDecl(mXMDP.getGoal());
			String endDecl = mHelper.buildEndDecl(mXMDP.getGoal());
			builder.append(goalDecl);
			builder.append("\n");
			builder.append(endDecl);
			builder.append("\n\n");
		}

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
	 * @param costCriterion
	 *            : Cost criterion of MDP
	 * @return Goal reachability property of this MDP with cost minimization
	 * @throws VarNotFoundException
	 */
	public String getGoalPropertyTranslation(CostCriterion costCriterion) throws VarNotFoundException {
		return mPropertyTranslator.buildMDPCostMinProperty(mXMDP.getGoal(), mXMDP.getCostFunction(), costCriterion);
	}

	/**
	 * Build partial commands of a module -- for MDP.
	 * 
	 * @param actionDescription
	 *            : Action description of an effect class (possibly merged)
	 * @return Commands for updating a particular effect class of actionDescription
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
				String command = mHelper.buildModuleCommand(action, discriminant, probEffect);
				if (!first) {
					builder.append("\n");
				} else {
					first = false;
				}
				builder.append(PrismTranslatorUtils.INDENT);
				builder.append(command);
			}
		}
		return builder.toString();
	}

}
