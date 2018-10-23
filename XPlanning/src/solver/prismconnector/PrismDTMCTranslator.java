package solver.prismconnector;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import language.domain.metrics.EventBasedMetric;
import language.domain.metrics.IEvent;
import language.domain.metrics.IQFunction;
import language.domain.models.ActionDefinition;
import language.domain.models.IAction;
import language.domain.models.IStateVarValue;
import language.domain.models.StateVar;
import language.domain.models.StateVarDefinition;
import language.dtmc.TwoTBN;
import language.dtmc.XDTMC;
import language.exceptions.VarNotFoundException;
import language.exceptions.XMDPException;
import language.mdp.Discriminant;
import language.mdp.DiscriminantClass;
import language.mdp.FactoredPSO;
import language.mdp.IActionDescription;
import language.mdp.ProbabilisticEffect;
import language.mdp.StateVarTuple;
import language.mdp.XMDP;
import language.objectives.CostCriterion;
import language.objectives.CostFunction;
import solver.prismconnector.PrismTranslatorHelper.ActionFilter;
import solver.prismconnector.PrismTranslatorHelper.PartialModuleCommandsBuilder;

public class PrismDTMCTranslator {

	private XDTMC mXDTMC;
	private ValueEncodingScheme mEncodings;
	private ActionFilter mActionFilter;
	private PrismRewardTranslator mRewardTranslator;
	private PrismPropertyTranslator mPropertyTranslator;
	private PrismTranslatorHelper mHelper;

	public PrismDTMCTranslator(XDTMC xdtmc) {
		mXDTMC = xdtmc;
		XMDP xmdp = xdtmc.getXMDP();
		mEncodings = new ValueEncodingScheme(xmdp.getStateSpace(), xmdp.getActionSpace(), xmdp.getQSpace(),
				xmdp.getCostFunction());
		mActionFilter = action -> mXDTMC.getPolicy().containsAction(action);
		mRewardTranslator = new PrismRewardTranslator(xmdp.getTransitionFunction(), mEncodings, mActionFilter);
		mPropertyTranslator = new PrismPropertyTranslator(mEncodings);
		mHelper = new PrismTranslatorHelper(mEncodings);
	}

	public ValueEncodingScheme getValueEncodingScheme() {
		return mEncodings;
	}

	public PrismPropertyTranslator getPrismPropertyTranslator() {
		return mPropertyTranslator;
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

		boolean hasGoal = xmdp.getGoal() != null;

		String constsDecl = mHelper.buildConstsDecl(xmdp.getStateSpace());
		String modules = mHelper.buildModules(xmdp.getStateSpace(), xmdp.getInitialState(), actionDefs, actionPSOs,
				partialCommandsBuilder, mActionFilter, hasGoal);
		String costStruct = mRewardTranslator.getCostFunctionTranslation(xmdp.getCostFunction());

		StringBuilder builder = new StringBuilder();
		builder.append("dtmc");
		builder.append("\n\n");
		builder.append(constsDecl);
		builder.append("\n\n");

		if (hasGoal) {
			String goalDecl = mHelper.buildGoalDecl(xmdp.getGoal());
			String endDecl = mHelper.buildEndDecl(xmdp.getGoal());
			builder.append(goalDecl);
			builder.append("\n");
			builder.append(endDecl);
			builder.append("\n\n");
		}

		builder.append(modules);
		builder.append("\n\n");
		builder.append(costStruct);

		if (withQAFunctions) {
			String qasRewards = mRewardTranslator.getQAFunctionsTranslation(xmdp.getQSpace());
			builder.append("\n\n");
			builder.append(qasRewards);
		}

		return builder.toString();
	}

	/**
	 * 
	 * @param eventBasedMetric
	 *            : Event-based metric
	 * @return Reward structures for counters of events in the event-based metric
	 * @throws XMDPException
	 */
	public String getEventCounterRewardsTranslation(EventBasedMetric<?, ?, ?> eventBasedMetric) throws XMDPException {
		return mRewardTranslator.getEventCounters(eventBasedMetric);
	}

	/**
	 * 
	 * @param costCriterion
	 *            : Cost criterion of the corresponding MDP
	 * @return Numerical query property of the expected total cost of this DTMC
	 * @throws VarNotFoundException
	 */
	public String getCostQueryPropertyTranslation(CostCriterion costCriterion) throws VarNotFoundException {
		StateVarTuple goal = mXDTMC.getXMDP().getGoal();
		CostFunction costFunction = mXDTMC.getXMDP().getCostFunction();
		return mPropertyTranslator.buildDTMCCostQueryProperty(goal, costFunction, costCriterion);
	}

	/**
	 * 
	 * @param qFunction
	 *            : QA function
	 * @param costCriterion
	 *            : Cost criterion of the corresponding MDP
	 * @return Numerical query property of the expected total QA value, or long-run average QA value, of this DTMC
	 * @throws VarNotFoundException
	 */
	public String getNumQueryPropertyTranslation(IQFunction<?, ?> qFunction, CostCriterion costCriterion)
			throws VarNotFoundException {
		StateVarTuple goal = mXDTMC.getXMDP().getGoal();
		return mPropertyTranslator.buildDTMCNumQueryProperty(goal, qFunction, costCriterion);
	}

	/**
	 * 
	 * @param event
	 *            : Event to be counted
	 * @param costCriterion
	 *            : Cost criterion of the corresponding MDP
	 * @return Numerical query property of the expected total occurrences of the event in this DTMC
	 * @throws VarNotFoundException
	 */
	public String getEventCountPropertyTranslation(IEvent<?, ?> event, CostCriterion costCriterion)
			throws VarNotFoundException {
		StateVarTuple goal = mXDTMC.getXMDP().getGoal();
		return mPropertyTranslator.buildDTMCEventCountProperty(goal, event, costCriterion);
	}

	/**
	 * Build partial commands of a module -- for DTMC.
	 * 
	 * @param actionDescription
	 *            : Action description of an effect class (possibly merged)
	 * @return Commands for updating the effect class of actionDescription
	 * @throws XMDPException
	 */
	private String buildDTMCPartialModuleCommands(IActionDescription<IAction> actionDescription) throws XMDPException {
		TwoTBN<IAction> twoTBN = mXDTMC.get2TBN(actionDescription.getActionDefinition());
		DiscriminantClass discrClass = actionDescription.getDiscriminantClass();

		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for (Entry<StateVarTuple, IAction> entry : twoTBN) {
			StateVarTuple state = entry.getKey();
			IAction action = entry.getValue();

			Discriminant discriminant = new Discriminant(discrClass);
			for (StateVarDefinition<IStateVarValue> stateVarDef : discrClass) {
				IStateVarValue value = state.getStateVarValue(IStateVarValue.class, stateVarDef);
				StateVar<IStateVarValue> stateVar = stateVarDef.getStateVar(value);
				discriminant.add(stateVar);
			}
			ProbabilisticEffect probEffect = actionDescription.getProbabilisticEffect(discriminant, action);
			String command = mHelper.buildModuleCommand(action, state, probEffect);
			if (!first) {
				builder.append("\n");
			} else {
				first = false;
			}
			builder.append(PrismTranslatorUtils.INDENT);
			builder.append(command);
		}
		return builder.toString();
	}

}
