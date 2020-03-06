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
import language.mdp.ActionSpace;
import language.mdp.Discriminant;
import language.mdp.DiscriminantClass;
import language.mdp.FactoredPSO;
import language.mdp.IActionDescription;
import language.mdp.ProbabilisticEffect;
import language.mdp.StateVarTuple;
import language.mdp.TransitionFunction;
import language.mdp.XMDP;
import language.objectives.AttributeCostFunction;
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
	 * @param withQACostFunctions
	 *            : Whether or not to include single-attribute cost functions of QAs in the DTMC translation
	 * @return Prism model of this DTMC, including constants' declarations, DTMC model, and a reward structure
	 *         representing the cost function of the corresponding MDP, and optionally reward structure(s) representing
	 *         the QA function(s) and the corresponding single-attribute cost function(s).
	 * @throws XMDPException
	 */
	public String getDTMCTranslation(boolean withQAFunctions, boolean withQACostFunctions) throws XMDPException {
		XMDP xmdp = mXDTMC.getXMDP();

		ActionSpace actionDefs = new ActionSpace();
		TransitionFunction actionPSOs = new TransitionFunction();
		for (TwoTBN<IAction> twoTBN : mXDTMC) {
			ActionDefinition<IAction> actionDef = twoTBN.getActionDefinition();

			// Add any action definition from the DTMC to the set actionDefs
			actionDefs.addActionDefinition(actionDef);

			// Any action that is part of a composite action (i.e., constituent action) has additional effect classes
			// that are defined in the composite action PSO, but not defined in the individual action PSO.

			// Therefore, we must obtain both the individual constituent action PSO (if exists) and the parent composite
			// action PSO -- so that we get all the effect classes of the constituent action.

			if (actionDef.getParentCompositeActionDefinition() != null) {
				// This actionDef is a constituent action defn
				// Obtain the parent composition action PSO
				FactoredPSO<IAction> parentCompActionPSO = xmdp.getTransitionFunction()
						.getParentCompositeActionPSO(actionDef);

				// Add the parent composite action PSO to the set actionPSOs
				actionPSOs.add(parentCompActionPSO);
			}

			// Some action definition may only have its parent composite action PSO, and doesn't have its own individual
			// action PSO.

			if (xmdp.getTransitionFunction().hasActionPSO(actionDef)) {
				FactoredPSO<IAction> actionPSO = xmdp.getTransitionFunction().getActionPSO(actionDef);

				// Add the individual action PSO to the set actionPSOs
				actionPSOs.add(actionPSO);
			}
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
				partialCommandsBuilder, hasGoal);
		// helper module
		String helperModule = mHelper.buildHelperModule(xmdp.getActionSpace(), mActionFilter, hasGoal);
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
		builder.append(helperModule);
		builder.append("\n\n");
		builder.append(costStruct);

		if (withQAFunctions) {
			String qaStructs = mRewardTranslator.getQAFunctionsTranslation(xmdp.getQSpace());
			builder.append("\n\n");
			builder.append(qaStructs);
		}

		if (withQACostFunctions) {
			String qaCostStructs = mRewardTranslator
					.getQACostFunctionsTranslation(xmdp.getCostFunction().getAttributeCostFunctions());
			builder.append("\n\n");
			builder.append(qaCostStructs);
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
	 * @return Numerical query property of the expected total cost, or long-run average cost, of this DTMC
	 * @throws VarNotFoundException
	 */
	public String getCostQueryPropertyTranslation(CostCriterion costCriterion) throws VarNotFoundException {
		StateVarTuple goal = mXDTMC.getXMDP().getGoal();
		CostFunction costFunction = mXDTMC.getXMDP().getCostFunction();
		return mPropertyTranslator.buildDTMCCostQueryProperty(goal, costFunction, costCriterion);
	}

	/**
	 * 
	 * @param attrCostFunction
	 *            : Single-attribute cost function of a QA
	 * @param costCriterion
	 *            : Cost criterion of the corresponding MDP
	 * @return Numerical query property of the expected total, or long-run average, QA cost of this DTMC
	 * @throws VarNotFoundException
	 */
	public String getQACostQueryPropertyTranslation(AttributeCostFunction<?> attrCostFunction,
			CostCriterion costCriterion) throws VarNotFoundException {
		StateVarTuple goal = mXDTMC.getXMDP().getGoal();
		return mPropertyTranslator.buildDTMCQACostQueryProperty(goal, attrCostFunction, costCriterion);
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
	 * 
	 * @param queryState
	 *            : Query state
	 * @return Reachability probability query property of the query state
	 * @throws VarNotFoundException
	 */
	public String getReachabilityQueryPropertyTranslation(StateVarTuple queryState) throws VarNotFoundException {
		return mPropertyTranslator.buildDTMCReachabilityQueryProperty(queryState);
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
		ActionDefinition<IAction> actionDef = actionDescription.getActionDefinition();

		if (actionDef.isComposite()) {
			// The action description is of a composite action definition -- its effect class corresponds to multiple
			// action types.
			// Thus, the module commands that update the variables in the effect class must include all of the
			// constituent action types.

			StringBuilder builder = new StringBuilder();

			// Create module commands for each constituent action definition
			for (ActionDefinition<IAction> constituentActionDef : filterConstituentActionDefinitions(actionDef)) {
				TwoTBN<IAction> twoTBN = mXDTMC.get2TBN(constituentActionDef);

				String partialCommands = buildDTMCPartialModuleCommandsHelper(twoTBN, actionDescription);

				builder.append("\n");
				builder.append(PrismTranslatorUtils.INDENT);
				builder.append("// ");
				builder.append(constituentActionDef.getName());
				builder.append("\n");
				builder.append(partialCommands);
			}
			return builder.toString();
		} else {
			TwoTBN<IAction> twoTBN = mXDTMC.get2TBN(actionDef);
			return buildDTMCPartialModuleCommandsHelper(twoTBN, actionDescription);
		}
	}

	/**
	 * Build partial module commands based on the given 2TBN (of a particular action type) and the action description
	 * (of a particular action type and effect class).
	 * 
	 * Note that the action type of the 2TBN can be a constituent of the action type of the action description.
	 * 
	 * @param twoTBN
	 *            : 2TBN -- with all effect classes of its action type
	 * @param actionDescription
	 *            : Action description -- with only 1 effect class (containing the variables defined in the module)
	 * @return Partial module commands based on the 2TBN and the action description.
	 * @throws XMDPException
	 */
	private String buildDTMCPartialModuleCommandsHelper(TwoTBN<IAction> twoTBN,
			IActionDescription<IAction> actionDescription) throws XMDPException {
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

	/**
	 * Filter unique action definitions that are present in the XDTMC.
	 * 
	 * @param compositeActionDef
	 * @return Unique action definitions that are present in the XDTM.
	 */
	private Set<ActionDefinition<IAction>> filterConstituentActionDefinitions(
			ActionDefinition<IAction> compositeActionDef) {
		Set<ActionDefinition<IAction>> res = new HashSet<>();

		for (IAction constituentAction : compositeActionDef.getActions()) {
			// Look up constituent action definition
			ActionDefinition<IAction> constituentActionDef = mXDTMC.getXMDP().getActionSpace()
					.getActionDefinition(constituentAction);

			if (mXDTMC.contains(constituentActionDef)) {
				res.add(constituentActionDef);
			}
		}

		return res;
	}

}
