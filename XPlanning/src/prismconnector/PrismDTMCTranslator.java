package prismconnector;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import dtmc.TwoTBN;
import dtmc.XDTMC;
import exceptions.VarNotFoundException;
import exceptions.XMDPException;
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
import mdp.StatePredicate;
import mdp.XMDP;
import metrics.IQFunction;
import objectives.CostFunction;
import prismconnector.PrismTranslatorUtilities.PartialModuleCommandsBuilder;

public class PrismDTMCTranslator {

	private XDTMC mXDTMC;
	private ValueEncodingScheme mEncodings;
	private PrismRewardTranslator mRewardTranslator;
	private PrismPropertyTranslator mPropertyTranslator;
	private PrismTranslatorUtilities mUtilities;

	public PrismDTMCTranslator(XDTMC xdtmc, boolean threeParamRewards, PrismRewardType prismRewardType) {
		mXDTMC = xdtmc;
		if (threeParamRewards) {
			mEncodings = new ValueEncodingScheme(xdtmc.getXMDP().getStateSpace(), xdtmc.getXMDP().getActionSpace());
		} else {
			mEncodings = new ValueEncodingScheme(xdtmc.getXMDP().getStateSpace());
		}
		mRewardTranslator = new PrismRewardTranslator(xdtmc.getXMDP().getTransitionFunction(), mEncodings,
				prismRewardType);
		mPropertyTranslator = new PrismPropertyTranslator(mEncodings);
		mUtilities = new PrismTranslatorUtilities(mEncodings);
	}

	public ValueEncodingScheme getValueEncodingScheme() {
		return mEncodings;
	}

	/**
	 * 
	 * @param withQAFunctions
	 *            : Whether or not to include QA functions in the DTMC translation
	 * @return Prism model of this DTMC, including constants' declarations, DTMC model, and a reward structure
	 *         representing the cost function of the corresponding MDP, and optionally reward structure(s) representing
	 *         the QA function(s).
	 * @throws XMDPException
	 */
	public String getDTMCTranslation(boolean withQAFunctions) throws XMDPException {
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
					throws XMDPException {
				return buildDTMCPartialModuleCommands(actionDescription);
			}
		};

		String constsDecl = mUtilities.buildConstsDecl(xmdp.getStateSpace());
		String actionsDecl = mUtilities.buildConstsDecl(xmdp.getActionSpace());
		String goalDecl = mUtilities.buildGoalDecl(xmdp.getGoal());
		String modules = mUtilities.buildModules(xmdp.getStateSpace(), xmdp.getInitialState(), actionDefs, actionPSOs,
				partialCommandsBuilder);
		String costStruct = mRewardTranslator.getCostFunctionTranslation(xmdp.getCostFunction());

		StringBuilder builder = new StringBuilder();
		builder.append("dtmc");
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
			String qasRewards = mRewardTranslator.getQAFunctionsTranslation(xmdp.getQFunctions());
			builder.append("\n\n");
			builder.append(qasRewards);
		}

		return builder.toString();
	}

	/**
	 * 
	 * @return Numerical query property of the expected total cost of this DTMC
	 * @throws VarNotFoundException
	 */
	public String getCostQueryPropertyTranslation() throws VarNotFoundException {
		StatePredicate goal = mXDTMC.getXMDP().getGoal();
		CostFunction costFunction = mXDTMC.getXMDP().getCostFunction();
		return mPropertyTranslator.buildDTMCCostQueryProperty(goal, costFunction);
	}

	/**
	 * 
	 * @param qFunction
	 *            : QA function
	 * @return Numerical query property of the expected total QA value of this DTMC
	 * @throws VarNotFoundException
	 */
	public String getNumQueryPropertyTranslation(IQFunction qFunction) throws VarNotFoundException {
		StatePredicate goal = mXDTMC.getXMDP().getGoal();
		return mPropertyTranslator.buildDTMCNumQueryProperty(goal, qFunction);
	}

	/**
	 * Build partial commands of a module -- for DTMC.
	 * 
	 * @param actionDescription
	 *            : Action description of an effect class (possibly merged)
	 * @return commands for updating a particular effect class of actionDescription
	 * @throws XMDPException
	 */
	private String buildDTMCPartialModuleCommands(IActionDescription<IAction> actionDescription) throws XMDPException {
		TwoTBN<IAction> twoTBN = mXDTMC.get2TBN(actionDescription.getActionDefinition());
		DiscriminantClass discrClass = actionDescription.getDiscriminantClass();

		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for (Entry<StatePredicate, IAction> entry : twoTBN) {
			StatePredicate state = entry.getKey();
			IAction action = entry.getValue();

			Discriminant discriminant = new Discriminant(discrClass);
			for (StateVarDefinition<IStateVarValue> stateVarDef : discrClass) {
				IStateVarValue value = state.getStateVarValue(IStateVarValue.class, stateVarDef);
				StateVar<IStateVarValue> stateVar = stateVarDef.getStateVar(value);
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
