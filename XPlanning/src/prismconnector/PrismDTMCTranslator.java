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
import mdp.XMDP;
import metrics.IQFunction;
import policy.Policy;
import policy.Predicate;
import prismconnector.PrismTranslatorUtilities.BuildPartialModuleCommands;

public class PrismDTMCTranslator {

	private XDTMC mXDTMC;
	private PrismTranslatorUtilities mUtilities;

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
	}

	public String getDTMCTranslation() throws VarNotFoundException, EffectClassNotFoundException,
			ActionNotFoundException, IncompatibleActionException, IncompatibleVarException,
			IncompatibleEffectClassException, IncompatibleDiscriminantClassException, ActionDefinitionNotFoundException,
			DiscriminantNotFoundException, AttributeNameNotFoundException {
		XMDP xmdp = mXDTMC.getXMDP();

		Set<ActionDefinition<IAction>> actionDefs = new HashSet<>();
		Set<FactoredPSO<IAction>> actionPSOs = new HashSet<>();
		for (TwoTBN<IAction> twoTBN : mXDTMC) {
			ActionDefinition<IAction> actionDef = twoTBN.getActionDefinition();
			FactoredPSO<IAction> actionPSO = xmdp.getTransitionFunction().getActionPSO(actionDef);
			actionDefs.add(actionDef);
			actionPSOs.add(actionPSO);
		}

		BuildPartialModuleCommands partialCommandsBuilder = new BuildPartialModuleCommands() {

			@Override
			public String buildPartialModuleCommands(IActionDescription<IAction> actionDescription)
					throws ActionNotFoundException, VarNotFoundException, IncompatibleVarException,
					DiscriminantNotFoundException {
				return buildDTMCPartialModuleCommands(actionDescription);
			}
		};

		String constsDecl = mUtilities.buildConstsDecl(xmdp.getStateSpace());
		String modules = mUtilities.buildModules(xmdp.getStateSpace(), xmdp.getInitialState(), actionDefs, actionPSOs,
				partialCommandsBuilder);
		String rewards = mUtilities.buildRewards(xmdp.getTransitionFunction(), xmdp.getQFunctions(),
				xmdp.getCostFunction());
		StringBuilder builder = new StringBuilder();
		builder.append("dtmc");
		builder.append("\n\n");
		builder.append(constsDecl);
		builder.append("\n");
		builder.append(modules);
		builder.append("\n");
		builder.append(rewards);
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
			Integer encodedValue = mUtilities.getValueEncodingScheme().getEncodedIntValue(goalVar.getDefinition(),
					goalVar.getValue());
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

	/**
	 * Build partial module commands for DTMC.
	 * 
	 * @param actionDescription
	 *            Action description of an effect class (possibly merged)
	 * @param twoTBN
	 *            2TBN of the same action type as that of actionDescription
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

		StringBuilder builder = new StringBuilder();
		DiscriminantClass discrClass = actionDescription.getDiscriminantClass();
		for (Entry<Predicate, IAction> entry : twoTBN) {
			Predicate predicate = entry.getKey();
			IAction action = entry.getValue();

			Discriminant discriminant = new Discriminant(discrClass);
			for (StateVarDefinition<IStateVarValue> stateVarDef : discrClass) {
				IStateVarValue value = predicate.getStateVarValue(IStateVarValue.class, stateVarDef);
				StateVar<IStateVarValue> stateVar = new StateVar<>(stateVarDef, value);
				discriminant.add(stateVar);
			}
			ProbabilisticEffect probEffect = actionDescription.getProbabilisticEffect(discriminant, action);
			String command = mUtilities.buildModuleCommand(action, predicate, probEffect);
			builder.append(PrismTranslatorUtilities.INDENT);
			builder.append(command);
			builder.append("\n");
		}
		return builder.toString();
	}

}
