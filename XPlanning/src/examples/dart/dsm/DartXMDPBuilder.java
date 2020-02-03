package examples.dart.dsm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import examples.dart.models.ChangeFormAction;
import examples.dart.models.DecAltAction;
import examples.dart.models.FlyAction;
import examples.dart.models.IncAltAction;
import examples.dart.models.RouteSegment;
import examples.dart.models.SwitchECMAction;
import examples.dart.models.TargetDistribution;
import examples.dart.models.TeamAltitude;
import examples.dart.models.TeamDestroyed;
import examples.dart.models.TeamECM;
import examples.dart.models.TeamFormation;
import examples.dart.models.ThreatDistribution;
import language.domain.models.ActionDefinition;
import language.domain.models.StateVarDefinition;
import language.mdp.ActionSpace;
import language.mdp.QSpace;
import language.mdp.StateSpace;
import language.mdp.StateVarTuple;
import language.mdp.TransitionFunction;
import language.mdp.XMDP;
import language.objectives.CostFunction;

public class DartXMDPBuilder {

	// --- TeamAltitude --- //

	// Team's altitude state variable
	private StateVarDefinition<TeamAltitude> teamAltDef;

	// Possible altitude changes within 1 period
	TeamAltitude altChange1 = new TeamAltitude(1);
	TeamAltitude altChange2 = new TeamAltitude(2);

	// Increase altitude actions
	private IncAltAction incAlt1 = new IncAltAction(altChange1);
	private IncAltAction incAlt2 = new IncAltAction(altChange2);

	// IncAlt action definition
	private ActionDefinition<IncAltAction> incAltDef = new ActionDefinition<>("incAlt", incAlt1, incAlt2);

	// Decrease altitude actions
	private DecAltAction decAlt1 = new DecAltAction(altChange1);
	private DecAltAction decAlt2 = new DecAltAction(altChange2);

	// DecAlt action definition
	private ActionDefinition<DecAltAction> decAltDef = new ActionDefinition<>("decAlt", decAlt1, decAlt2);

	// ------ //

	// --- TeamFormation --- //

	// Team's formation state variable
	private StateVarDefinition<TeamFormation> teamFormDef;

	// Change formation actions
	private ChangeFormAction goLoose = new ChangeFormAction(new TeamFormation("loose"));
	private ChangeFormAction goTight = new ChangeFormAction(new TeamFormation("tight"));

	// ChangeForm action definition
	private ActionDefinition<ChangeFormAction> changeFormDef = new ActionDefinition<>("changeForm", goLoose, goTight);

	// ------ //

	// --- TeamECM --- //

	// Team's ECM state variable
	private StateVarDefinition<TeamECM> teamECMDef;

	// Switch ECM actions
	private SwitchECMAction turnECMOn = new SwitchECMAction(new TeamECM(true));
	private SwitchECMAction turnECMOff = new SwitchECMAction(new TeamECM(false));

	// SwitchECM action definition
	private ActionDefinition<SwitchECMAction> switchECMDef = new ActionDefinition<>("switchECM", turnECMOn, turnECMOff);

	// ------ //

	// --- RouteSegment --- //

	// Team's current route segment state variable
	private StateVarDefinition<RouteSegment> segmentDef;

	// Fly (for 1 segment) action
	private FlyAction fly = new FlyAction();

	// Fly action definition
	private ActionDefinition<FlyAction> flyDef = new ActionDefinition<>("fly", fly);

	// IncAlt and DecAlt actions also affect route segment variable

	// ------ //

	// --- TeamDestroyed --- //

	// Whether or not team has been shot down by a threat
	private StateVarDefinition<TeamDestroyed> teamDestroyedDef;

	// IncAlt, DecAlt, and Fly actions can affect teamDestroyed variable

	// ------ //

	// List of route segment objects in the order of their numbers
	// To be used when adding attribute values to each route segment
	private List<RouteSegment> mOrderedSegments = new ArrayList<>();

	public DartXMDPBuilder() {
		// Constructor may take as input other DSMs
	}

	public XMDP buildXMDP(int maxAlt, int horizon, double[] expThreatProbs, double[] expTargetProbs, int iniAltLevel,
			String iniForm, boolean iniECM) {
		StateSpace stateSpace = buildStateSpace(maxAlt, horizon, expThreatProbs, expTargetProbs);
		ActionSpace actionSpace = buildActionSpace();
		StateVarTuple initialState = buildInitialState(iniAltLevel, iniForm, iniECM);
		StateVarTuple goal = buildGoal(horizon);
		TransitionFunction transFunction = buildTransitionFunction();
		QSpace qSpace = buildQFunctions();
		CostFunction costFunction = buildCostFunction(qSpace);
		return new XMDP(stateSpace, actionSpace, initialState, goal, transFunction, qSpace, costFunction);
	}

	private StateSpace buildStateSpace(int maxAlt, int horizon, double[] expThreatProbs, double[] expTargetProbs) {
		// Possible values of teamAltitude
		Set<TeamAltitude> alts = new HashSet<>();
		for (int i = 0; i < maxAlt; i++) {
			TeamAltitude teamAlt = new TeamAltitude(i + 1);
			alts.add(teamAlt);
		}
		teamAltDef = new StateVarDefinition<>("altitude", alts);

		// Possible values of teamFormation
		TeamFormation tightForm = new TeamFormation("tight");
		TeamFormation looseForm = new TeamFormation("loose");
		teamFormDef = new StateVarDefinition<>("formation", tightForm, looseForm);

		// Possible values of teamECM
		TeamECM ecmOn = new TeamECM(true);
		TeamECM ecmOff = new TeamECM(false);
		teamECMDef = new StateVarDefinition<>("ecm", ecmOn, ecmOff);

		// Possible values of routeSegment
		Set<RouteSegment> segments = new HashSet<>();
		for (int i = 0; i < horizon; i++) {
			ThreatDistribution threatDist = new ThreatDistribution(expThreatProbs[i]);
			TargetDistribution targetDist = new TargetDistribution(expTargetProbs[i]);

			RouteSegment segment = new RouteSegment(i + 1, threatDist, targetDist);
			segments.add(segment);

			// Add each route segment object in the order of its number
			mOrderedSegments.add(segment);
		}
		segmentDef = new StateVarDefinition<>("segment", segments);

		// Possible values of teamDestroyed
		TeamDestroyed destroyed = new TeamDestroyed(true);
		TeamDestroyed alive = new TeamDestroyed(false);
		teamDestroyedDef = new StateVarDefinition<>("destroyed", destroyed, alive);

		StateSpace stateSpace = new StateSpace();
		stateSpace.addStateVarDefinition(teamAltDef);
		stateSpace.addStateVarDefinition(teamFormDef);
		stateSpace.addStateVarDefinition(teamECMDef);
		stateSpace.addStateVarDefinition(segmentDef);
		stateSpace.addStateVarDefinition(teamDestroyedDef);
		return stateSpace;
	}

	private ActionSpace buildActionSpace() {
		ActionSpace actionSpace = new ActionSpace();
		actionSpace.addActionDefinition(incAltDef);
		actionSpace.addActionDefinition(decAltDef);
		actionSpace.addActionDefinition(flyDef);
		actionSpace.addActionDefinition(changeFormDef);
		actionSpace.addActionDefinition(switchECMDef);
		return actionSpace;
	}

	private StateVarTuple buildInitialState(int iniAltLevel, String iniForm, boolean iniECM) {
		TeamAltitude iniTeamAlt = new TeamAltitude(iniAltLevel);
		TeamFormation iniTeamForm = new TeamFormation(iniForm);
		TeamECM iniTeamECM = new TeamECM(iniECM);
		RouteSegment iniSegment = mOrderedSegments.get(0);
		TeamDestroyed iniDestroyed = new TeamDestroyed(false);

		StateVarTuple initialState = new StateVarTuple();
		initialState.addStateVar(teamAltDef.getStateVar(iniTeamAlt));
		initialState.addStateVar(teamFormDef.getStateVar(iniTeamForm));
		initialState.addStateVar(teamECMDef.getStateVar(iniTeamECM));
		initialState.addStateVar(segmentDef.getStateVar(iniSegment));
		initialState.addStateVar(teamDestroyedDef.getStateVar(iniDestroyed));
		return initialState;
	}

	private StateVarTuple buildGoal(int horizon) {
		RouteSegment lastSegment = mOrderedSegments.get(horizon - 1);

		StateVarTuple goal = new StateVarTuple();
		goal.addStateVar(segmentDef.getStateVar(lastSegment));
		return goal;
	}

	private TransitionFunction buildTransitionFunction() {
		return null;
	}

	private QSpace buildQFunctions() {
		return null;
	}

	private CostFunction buildCostFunction(QSpace qSpace) {
		return null;
	}
}
