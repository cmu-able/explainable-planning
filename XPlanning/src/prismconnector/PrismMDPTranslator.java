package prismconnector;

import java.util.Set;

import exceptions.ActionDefinitionNotFoundException;
import exceptions.ActionNotFoundException;
import exceptions.AttributeNameNotFoundException;
import exceptions.DiscriminantNotFoundException;
import exceptions.EffectClassNotFoundException;
import exceptions.IncompatibleActionException;
import exceptions.IncompatibleDiscriminantClassException;
import exceptions.IncompatibleEffectClassException;
import exceptions.IncompatibleVarException;
import exceptions.VarNotFoundException;
import factors.ActionDefinition;
import factors.IAction;
import mdp.Discriminant;
import mdp.IActionDescription;
import mdp.ProbabilisticEffect;
import mdp.ProbabilisticTransition;
import mdp.TransitionFunction;
import mdp.XMDP;
import metrics.IQFunction;
import prismconnector.PrismTranslatorUtilities.PartialModuleCommandsBuilder;

public class PrismMDPTranslator {

	private XMDP mXMDP;
	private PrismTranslatorUtilities mUtilities;
	private PrismRewardTranslatorUtilities mRewardUtilities;

	public PrismMDPTranslator(XMDP xmdp, boolean threeParamRewards) {
		mXMDP = xmdp;
		ValueEncodingScheme encodings;
		if (threeParamRewards) {
			encodings = new ValueEncodingScheme(xmdp.getStateSpace(), xmdp.getActionSpace());
		} else {
			encodings = new ValueEncodingScheme(xmdp.getStateSpace());
		}
		mUtilities = new PrismTranslatorUtilities(encodings, threeParamRewards);
		mRewardUtilities = new PrismRewardTranslatorUtilities(encodings, threeParamRewards);
	}

	/**
	 * 
	 * @return Prism model of this MDP, including constants' declarations, MDP model, and reward structure representing
	 *         the cost function.
	 * @throws VarNotFoundException
	 * @throws EffectClassNotFoundException
	 * @throws AttributeNameNotFoundException
	 * @throws IncompatibleVarException
	 * @throws DiscriminantNotFoundException
	 * @throws ActionNotFoundException
	 * @throws IncompatibleActionException
	 * @throws IncompatibleEffectClassException
	 * @throws IncompatibleDiscriminantClassException
	 * @throws ActionDefinitionNotFoundException
	 */
	public String getMDPTranslation() throws VarNotFoundException, EffectClassNotFoundException,
			AttributeNameNotFoundException, IncompatibleVarException, DiscriminantNotFoundException,
			ActionNotFoundException, IncompatibleActionException, IncompatibleEffectClassException,
			IncompatibleDiscriminantClassException, ActionDefinitionNotFoundException {
		PartialModuleCommandsBuilder partialCommandsBuilder = new PartialModuleCommandsBuilder() {

			@Override
			public String buildPartialModuleCommands(IActionDescription<IAction> actionDescription)
					throws ActionNotFoundException, VarNotFoundException {
				return buildMDPPartialModuleCommands(actionDescription);
			}
		};

		String constsDecl = mUtilities.buildConstsDecl(mXMDP.getStateSpace());
		String actionsDecl = mUtilities.buildConstsDecl(mXMDP.getActionSpace());
		String modules = mUtilities.buildModules(mXMDP.getStateSpace(), mXMDP.getInitialState(), mXMDP.getActionSpace(),
				mXMDP.getTransitionFunction(), partialCommandsBuilder);
		String rewardStruc = mRewardUtilities.buildRewardStructure(mXMDP.getTransitionFunction(), mXMDP.getQFunctions(),
				mXMDP.getCostFunction());
		StringBuilder builder = new StringBuilder();
		builder.append("mdp");
		builder.append("\n\n");
		builder.append(constsDecl);
		builder.append(actionsDecl);
		builder.append("\n");
		builder.append(modules);
		builder.append("\n\n");
		builder.append(rewardStruc);
		return builder.toString();
	}

	public String getMDPTranslationWithQAs() throws VarNotFoundException, EffectClassNotFoundException,
			AttributeNameNotFoundException, IncompatibleVarException, DiscriminantNotFoundException,
			ActionNotFoundException, IncompatibleActionException, IncompatibleEffectClassException,
			IncompatibleDiscriminantClassException, ActionDefinitionNotFoundException {
		StringBuilder builder = new StringBuilder();
		String mdpTranslation = getMDPTranslation();
		String qasRewards = getQAsRewardsTranslation();
		builder.append(mdpTranslation);
		builder.append("\n\n");
		builder.append(qasRewards);
		return builder.toString();
	}

	public String getQAsRewardsTranslation()
			throws ActionDefinitionNotFoundException, ActionNotFoundException, VarNotFoundException,
			IncompatibleVarException, DiscriminantNotFoundException, AttributeNameNotFoundException {
		TransitionFunction transFunction = mXMDP.getTransitionFunction();
		StringBuilder builder = new StringBuilder();
		builder.append("// All Quality-Attribute Functions\n\n");
		boolean first = true;
		for (IQFunction qFunction : mXMDP.getQFunctions()) {
			if (!first) {
				builder.append("\n\n");
			} else {
				first = false;
			}
			builder.append("// ");
			builder.append(qFunction.getName());
			builder.append("\n\n");
			String rewards = mRewardUtilities.buildRewardStructure(transFunction, qFunction);
			builder.append(rewards);
		}
		return builder.toString();
	}

	/**
	 * 
	 * @return Goal reachability property of this MDP with cost minimization
	 * @throws VarNotFoundException
	 */
	public String getGoalPropertyTranslation() throws VarNotFoundException {
		return mRewardUtilities.buildMDPCostMinProperty(mXMDP.getGoal());
	}

	/**
	 * Build partial commands of a module -- for MDP.
	 * 
	 * @param actionDescription
	 *            : Action description of an effect class (possibly merged)
	 * @return commands for updating a particular effect class of actionDescription
	 * @throws ActionNotFoundException
	 * @throws VarNotFoundException
	 */
	private String buildMDPPartialModuleCommands(IActionDescription<IAction> actionDescription)
			throws ActionNotFoundException, VarNotFoundException {
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
