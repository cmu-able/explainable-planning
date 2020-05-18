package mobilerobot.study.interactive;

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
import language.mdp.StateVarTuple;
import language.mdp.XMDP;
import language.policy.Decision;
import language.policy.Policy;

public class WhyNotQueryStringGenerator {

	private XMDP mXMDP;

	public WhyNotQueryStringGenerator(XMDP xmdp) {
		mXMDP = xmdp;
	}

	public Set<String> generateAllWhyNotStringQueries(Policy queryPolicy) throws XMDPException {
		Set<String> stringQueries = new HashSet<>();

		for (Decision decision : queryPolicy) {
			// Pre-configuration action (optional): setSpeed
			// Other applicable setSpeed actions at this decision state
			Set<SetSpeedAction> applicableSetSpeedActions = getApplicableCandidateActions(decision, RobotSpeed.class,
					"rSpeed", "setSpeed");

			// Main query action (required): moveTo
			// Other applicable moveTo actions at this decision state
			Set<MoveToAction> applicableMoveToActions = getApplicableCandidateActions(decision, Location.class, "rLoc",
					"moveTo");

			// Filter out moveTo actions that backtrack from the query location
			Set<MoveToAction> nonBackTrackMoveToActions = applicableMoveToActions.stream().filter(candidateMoveTo -> {
				try {
					return !isBackTrackMoveToDestination(queryPolicy, decision.getState(), candidateMoveTo);
				} catch (VarNotFoundException e) {
					throw new RuntimeException(e.getMessage());
				}
			}).collect(Collectors.toSet());

			// Queries without pre-configuration action
			Set<String> moveToOnlyQueries = createStringQueries(decision.getState(), Collections.emptySet(),
					nonBackTrackMoveToActions);

			// Queries with pre-configuration action
			Set<String> setSpeedMoveToQueries = createStringQueries(decision.getState(), applicableSetSpeedActions,
					nonBackTrackMoveToActions);

			stringQueries.addAll(moveToOnlyQueries);
			stringQueries.addAll(setSpeedMoveToQueries);
		}

		return stringQueries;
	}

	private <E extends IAction, T extends IStateVarValue> Set<E> getApplicableCandidateActions(Decision decision,
			Class<T> varType, String varName, String actionName) throws XMDPException {
		// Action type: setSpeed or moveTo
		ActionDefinition<E> actionDef = mXMDP.getActionSpace().getActionDefinition(actionName);
		FactoredPSO<E> actionPSO = mXMDP.getTransitionFunction().getActionPSO(actionDef);
		Precondition<E> precond = actionPSO.getPrecondition();

		// State variable in the precondition of the action: rSpeed or rLoc
		StateVarDefinition<T> varDef = mXMDP.getStateSpace().getStateVarDefinition(varName);

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

	private boolean isBackTrackMoveToDestination(Policy queryPolicy, StateVarTuple queryState,
			MoveToAction candidateMoveTo) throws VarNotFoundException {
		StateVarDefinition<Location> rLocDef = mXMDP.getStateSpace().getStateVarDefinition("rLoc");

		Location querySrcLoc = queryState.getStateVarValue(Location.class, rLocDef);
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
			Set<? extends IAction> mainQueryActions) {
		Set<String> stringQueries = new HashSet<>();

		// Main query action (moveTo) is required
		for (IAction mainQueryAction : mainQueryActions) {

			// Target QA is required
			for (IQFunction<?, ?> targetQFunction : mXMDP.getQSpace()) {

				// Pre-configuration action (setSpeed) is optional
				if (preConfigActions.isEmpty()) {
					String queryNoPreConfigAction = createStringQuery(queryState, null, mainQueryAction,
							targetQFunction);
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
