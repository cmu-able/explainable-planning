package mobilerobot.study.interactive;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import examples.mobilerobot.models.Location;
import examples.mobilerobot.models.MoveToAction;
import examples.mobilerobot.models.RobotSpeed;
import examples.mobilerobot.models.SetSpeedAction;
import language.domain.metrics.IQFunction;
import language.domain.models.ActionDefinition;
import language.domain.models.IAction;
import language.domain.models.IStateVarValue;
import language.domain.models.StateVarDefinition;
import language.exceptions.VarNotFoundException;
import language.exceptions.XMDPException;
import language.mdp.FactoredPSO;
import language.mdp.Precondition;
import language.mdp.QSpace;
import language.mdp.StateVarTuple;
import language.mdp.XMDP;
import language.policy.Decision;
import language.policy.Policy;

public class WhyNotQueryGenerator {

	public WhyNotQueryGenerator(File mapsJsonDir) {

	}

	public void generateAllWhyNotQueries(File questionDir, File agentMissionFile, int agentIndex) {

	}

	public Set<String> generateAllWhyNotStringQueries(XMDP xmdp, Policy queryPolicy) throws XMDPException {
		Set<String> stringQueries = new HashSet<>();

		for (Decision decision : queryPolicy) {
			// Pre-configuration action (optional): setSpeed
			// Other applicable setSpeed actions at this decision state
			Set<SetSpeedAction> applicableSetSpeedActions = getApplicableCandidateActions(xmdp, decision,
					RobotSpeed.class, "rSpeed", "setSpeed");

			// Main query action (required): moveTo
			// Other applicable moveTo actions at this decision state
			Set<MoveToAction> applicableMoveToActions = getApplicableCandidateActions(xmdp, decision, Location.class,
					"rLoc", "moveTo");

			// Filter out moveTo actions that backtrack from the query location
			StateVarDefinition<Location> rLocDef = xmdp.getStateSpace().getStateVarDefinition("rLoc");
			Location querySrcLoc = decision.getState().getStateVarValue(Location.class, rLocDef);
			Set<MoveToAction> nonBackTrackMoveToActions = applicableMoveToActions.stream().filter(candidateMoveTo -> {
				try {
					return !isBackTrackMoveToDestination(queryPolicy, rLocDef, querySrcLoc, candidateMoveTo);
				} catch (VarNotFoundException e) {
					throw new RuntimeException(e.getMessage());
				}
			}).collect(Collectors.toSet());

			// Queries without pre-configuration action
			Set<String> moveToOnlyQueries = createStringQueries(decision.getState(), Collections.emptySet(),
					nonBackTrackMoveToActions, xmdp.getQSpace());

			// Queries with pre-configuration action
			Set<String> setSpeedMoveToQueries = createStringQueries(decision.getState(), applicableSetSpeedActions,
					nonBackTrackMoveToActions, xmdp.getQSpace());

			stringQueries.addAll(moveToOnlyQueries);
			stringQueries.addAll(setSpeedMoveToQueries);
		}

		return stringQueries;
	}

	private <E extends IAction, T extends IStateVarValue> Set<E> getApplicableCandidateActions(XMDP xmdp,
			Decision decision, Class<T> varType, String varName, String actionName) throws XMDPException {
		// Action type: setSpeed or moveTo
		ActionDefinition<E> actionDef = xmdp.getActionSpace().getActionDefinition(actionName);
		FactoredPSO<E> actionPSO = xmdp.getTransitionFunction().getActionPSO(actionDef);
		Precondition<E> precond = actionPSO.getPrecondition();

		// State variable in the precondition of the action: rSpeed or rLoc
		StateVarDefinition<T> varDef = xmdp.getStateSpace().getStateVarDefinition(varName);

		Set<E> applicableCandidateActions = new HashSet<>();

		for (E candidateAction : actionDef.getActions()) {
			if (candidateAction.equals(decision.getAction())) {
				// Skip the action used in the query policy
				continue;
			}

			Set<T> applicableValues = precond.getApplicableValues(candidateAction, varDef);
			T rValue = decision.getState().getStateVarValue(varType, varDef);

			if (applicableValues.contains(rValue)) {
				// This candidate action is applicable in this decision state
				applicableCandidateActions.add(candidateAction);
			}
		}

		return applicableCandidateActions;
	}

	private boolean isBackTrackMoveToDestination(Policy queryPolicy, StateVarDefinition<Location> rLocDef,
			Location querySrcLoc, MoveToAction candidateMoveTo) throws VarNotFoundException {
		Location candidateDest = candidateMoveTo.getDestination();

		for (Decision decision : queryPolicy) {
			Location srcLoc = decision.getState().getStateVarValue(Location.class, rLocDef);

			if (srcLoc.equals(candidateDest) && decision.getAction() instanceof MoveToAction) {
				MoveToAction moveTo = (MoveToAction) decision.getAction();
				Location destLoc = moveTo.getDestination();

				if (destLoc.equals(querySrcLoc)) {
					return true;
				}
			}
		}
		return false;
	}

	private Set<String> createStringQueries(StateVarTuple queryState, Set<? extends IAction> preConfigActions,
			Set<? extends IAction> mainQueryActions, QSpace qFunctions) {
		Set<String> stringQueries = new HashSet<>();

		// Main query action (moveTo) is required
		for (IAction mainQueryAction : mainQueryActions) {

			// Target QA is required
			for (IQFunction<?, ?> targetQFunction : qFunctions) {

				// Pre-configuration action (setSpeed) is optional
				if (preConfigActions.isEmpty()) {
					String queryNoPreConfigAction = createStringQuery(queryState, null, mainQueryAction, targetQFunction);
					stringQueries.add(queryNoPreConfigAction);
				} else {
					for (IAction preConfigAction : preConfigActions) {
						String queryWithPreConfigAction = createStringQuery(queryState, preConfigAction,
								mainQueryAction, targetQFunction);
						stringQueries.add(queryWithPreConfigAction);
					}
				}
			}
		}

		return stringQueries;
	}

	private String createStringQuery(StateVarTuple queryState, IAction preConfigAction, IAction mainQueryAction,
			IQFunction<?, ?> targetQFunction) {
		// Query format: [query state];[pre-configuration action],[main query action];[target QA name]
		StringBuilder builder = new StringBuilder();

		builder.append(queryState); // query state: rLoc=L_,rSpeed=_
		builder.append(";");

		if (preConfigAction != null) {
			builder.append(preConfigAction); // pre-configuration action: setSpeed(_)
			builder.append(",");
		}

		builder.append(mainQueryAction); // main query action: moveTo(L_)
		builder.append(";");

		String qFunctionName = targetQFunction.getName();
		builder.append(qFunctionName); // target QA name: traveTime|collision|intrusiveness

		return builder.toString();
	}
}
