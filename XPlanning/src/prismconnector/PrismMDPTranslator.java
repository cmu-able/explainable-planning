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
import mdp.XMDP;
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
		String modules = mUtilities.buildModules(mXMDP.getStateSpace(), mXMDP.getInitialState(), mXMDP.getActionSpace(),
				mXMDP.getTransitionFunction(), partialCommandsBuilder);
		String rewards = mRewardUtilities.buildRewards(mXMDP.getTransitionFunction(), mXMDP.getQFunctions(),
				mXMDP.getCostFunction());
		StringBuilder builder = new StringBuilder();
		builder.append("mdp");
		builder.append("\n\n");
		builder.append(constsDecl);
		builder.append("\n");
		builder.append(modules);
		builder.append("\n");
		builder.append(rewards);
		return builder.toString();
	}

	/**
	 * 
	 * @return R{"cost"}min=? [ F {varName}={encoded int value} & ... ]
	 * @throws VarNotFoundException
	 */
	public String getGoalPropertyTranslation() throws VarNotFoundException {
		return mRewardUtilities.buildGoalProperty(mXMDP.getGoal());
	}

	/**
	 * Build partial module commands for MDP.
	 * 
	 * @param actionDescription
	 *            : Action description of an effect class (possibly merged)
	 * @return commands for updating a particular effect class of actionDescription
	 * @throws ActionNotFoundException
	 * @throws VarNotFoundException
	 */
	private String buildMDPPartialModuleCommands(IActionDescription<IAction> actionDescription)
			throws ActionNotFoundException, VarNotFoundException {
		StringBuilder builder = new StringBuilder();
		ActionDefinition<IAction> actionDef = actionDescription.getActionDefinition();
		for (IAction action : actionDef.getActions()) {
			Set<ProbabilisticTransition<IAction>> probTransitions = actionDescription
					.getProbabilisticTransitions(action);
			for (ProbabilisticTransition<IAction> probTrans : probTransitions) {
				Discriminant discriminant = probTrans.getDiscriminant();
				ProbabilisticEffect probEffect = probTrans.getProbabilisticEffect();
				String command = mUtilities.buildModuleCommand(action, discriminant, probEffect);
				builder.append(PrismTranslatorUtilities.INDENT);
				builder.append(command);
				builder.append("\n");
			}
		}
		return builder.toString();
	}

}
