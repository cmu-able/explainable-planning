package prismconnector;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import dtmc.TwoTBN;
import dtmc.XDTMC;
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
import factors.IStateVarValue;
import factors.StateVar;
import factors.StateVarDefinition;
import mdp.Discriminant;
import mdp.DiscriminantClass;
import mdp.FactoredPSO;
import mdp.IActionDescription;
import mdp.ProbabilisticEffect;
import mdp.State;
import mdp.TransitionFunction;
import mdp.XMDP;
import metrics.IQFunction;
import policy.Policy;
import prismconnector.PrismTranslatorUtilities.PartialModuleCommandsBuilder;

public class PrismDTMCTranslator {

	private XDTMC mXDTMC;
	private PrismTranslatorUtilities mUtilities;
	private PrismRewardTranslatorUtilities mRewardUtilities;

	public PrismDTMCTranslator(XMDP xmdp, Policy policy, boolean threeParamRewards)
			throws ActionDefinitionNotFoundException, EffectClassNotFoundException, VarNotFoundException,
			IncompatibleVarException, ActionNotFoundException, DiscriminantNotFoundException,
			IncompatibleActionException {
		mXDTMC = new XDTMC(xmdp, policy);
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
	 * @return Prism model of this DTMC, including constants' declarations and DTMC model.
	 * @throws VarNotFoundException
	 * @throws EffectClassNotFoundException
	 * @throws ActionNotFoundException
	 * @throws IncompatibleActionException
	 * @throws IncompatibleVarException
	 * @throws IncompatibleEffectClassException
	 * @throws IncompatibleDiscriminantClassException
	 * @throws ActionDefinitionNotFoundException
	 * @throws DiscriminantNotFoundException
	 */
	public String getDTMCTranslation()
			throws VarNotFoundException, EffectClassNotFoundException, ActionNotFoundException,
			IncompatibleActionException, IncompatibleVarException, IncompatibleEffectClassException,
			IncompatibleDiscriminantClassException, ActionDefinitionNotFoundException, DiscriminantNotFoundException {
		XMDP xmdp = mXDTMC.getXMDP();

		Set<ActionDefinition<IAction>> actionDefs = new HashSet<>();
		Set<FactoredPSO<IAction>> actionPSOs = new HashSet<>();
		for (TwoTBN<IAction> twoTBN : mXDTMC) {
			ActionDefinition<IAction> actionDef = twoTBN.getActionDefinition();
			FactoredPSO<IAction> actionPSO = xmdp.getTransitionFunction().getActionPSO(actionDef);
			actionDefs.add(actionDef);
			actionPSOs.add(actionPSO);
		}

		PartialModuleCommandsBuilder partialCommandsBuilder = new PartialModuleCommandsBuilder() {

			@Override
			public String buildPartialModuleCommands(IActionDescription<IAction> actionDescription)
					throws ActionNotFoundException, VarNotFoundException, IncompatibleVarException,
					DiscriminantNotFoundException {
				return buildDTMCPartialModuleCommands(actionDescription);
			}
		};

		String constsDecl = mUtilities.buildConstsDecl(xmdp.getStateSpace());
		String actionsDecl = mUtilities.buildConstsDecl(xmdp.getActionSpace());
		String modules = mUtilities.buildModules(xmdp.getStateSpace(), xmdp.getInitialState(), actionDefs, actionPSOs,
				partialCommandsBuilder);
		StringBuilder builder = new StringBuilder();
		builder.append("dtmc");
		builder.append("\n\n");
		builder.append(constsDecl);
		builder.append(actionsDecl);
		builder.append("\n");
		builder.append(modules);
		return builder.toString();
	}

	/**
	 * 
	 * @return Prism model of this DTMC, including constants' declarations, DTMC model, and reward structure(s)
	 *         representing the QA function(s).
	 * @throws VarNotFoundException
	 * @throws EffectClassNotFoundException
	 * @throws ActionNotFoundException
	 * @throws IncompatibleActionException
	 * @throws IncompatibleVarException
	 * @throws IncompatibleEffectClassException
	 * @throws IncompatibleDiscriminantClassException
	 * @throws ActionDefinitionNotFoundException
	 * @throws DiscriminantNotFoundException
	 * @throws AttributeNameNotFoundException
	 */
	public String getDTMCTranslationWithQAs() throws VarNotFoundException, EffectClassNotFoundException,
			ActionNotFoundException, IncompatibleActionException, IncompatibleVarException,
			IncompatibleEffectClassException, IncompatibleDiscriminantClassException, ActionDefinitionNotFoundException,
			DiscriminantNotFoundException, AttributeNameNotFoundException {
		TransitionFunction transFunction = mXDTMC.getXMDP().getTransitionFunction();
		Set<IQFunction> qFunctions = mXDTMC.getXMDP().getQFunctions();
		String dtmcTranslation = getDTMCTranslation();
		String qasRewards = mRewardUtilities.buildRewardStructures(transFunction, qFunctions);
		StringBuilder builder = new StringBuilder();
		builder.append(dtmcTranslation);
		builder.append("\n\n");
		builder.append(qasRewards);
		return builder.toString();
	}

	/**
	 * 
	 * @param qFunction
	 *            : QA function
	 * @return Numerical query property of the expected total QA value of this DTMC
	 * @throws VarNotFoundException
	 */
	public String getNumQueryPropertyTranslation(IQFunction qFunction) throws VarNotFoundException {
		State goal = mXDTMC.getXMDP().getGoal();
		return mRewardUtilities.buildDTMCNumQueryProperty(goal, qFunction);
	}

	/**
	 * Build partial commands of a module -- for DTMC.
	 * 
	 * @param actionDescription
	 *            : Action description of an effect class (possibly merged)
	 * @return commands for updating a particular effect class of actionDescription
	 * @throws ActionNotFoundException
	 * @throws VarNotFoundException
	 * @throws IncompatibleVarException
	 * @throws DiscriminantNotFoundException
	 */
	private String buildDTMCPartialModuleCommands(IActionDescription<IAction> actionDescription)
			throws ActionNotFoundException, VarNotFoundException, IncompatibleVarException,
			DiscriminantNotFoundException {
		TwoTBN<IAction> twoTBN = mXDTMC.get2TBN(actionDescription.getActionDefinition());
		DiscriminantClass discrClass = actionDescription.getDiscriminantClass();

		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for (Entry<State, IAction> entry : twoTBN) {
			State state = entry.getKey();
			IAction action = entry.getValue();

			Discriminant discriminant = new Discriminant(discrClass);
			for (StateVarDefinition<IStateVarValue> stateVarDef : discrClass) {
				IStateVarValue value = state.getStateVarValue(IStateVarValue.class, stateVarDef);
				StateVar<IStateVarValue> stateVar = new StateVar<>(stateVarDef, value);
				discriminant.add(stateVar);
			}
			ProbabilisticEffect probEffect = actionDescription.getProbabilisticEffect(discriminant, action);
			String command = mUtilities.buildModuleCommand(action, state, probEffect);
			if (!first) {
				builder.append("\n");
			} else {
				first = false;
			}
			builder.append(PrismTranslatorUtilities.INDENT);
			builder.append(command);
		}
		return builder.toString();
	}

}
