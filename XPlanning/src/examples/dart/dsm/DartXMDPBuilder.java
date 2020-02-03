package examples.dart.dsm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import examples.dart.metrics.DestroyedProbabilityDomain;
import examples.dart.metrics.DestroyedProbabilityQFunction;
import examples.dart.metrics.DetectTargetDomain;
import examples.dart.metrics.MissTargetEvent;
import examples.dart.models.ChangeFormAction;
import examples.dart.models.DecAltAction;
import examples.dart.models.DecAltAltitudeActionDescription;
import examples.dart.models.FlyAction;
import examples.dart.models.IDurativeAction;
import examples.dart.models.IncAltAction;
import examples.dart.models.IncAltAltitudeActionDescription;
import examples.dart.models.RouteSegment;
import examples.dart.models.RouteSegmentActionDescription;
import examples.dart.models.SwitchECMAction;
import examples.dart.models.TargetDistribution;
import examples.dart.models.TeamAltitude;
import examples.dart.models.TeamDestroyed;
import examples.dart.models.TeamDestroyedActionDescription;
import examples.dart.models.TeamECM;
import examples.dart.models.TeamECMActionDescription;
import examples.dart.models.TeamFormation;
import examples.dart.models.TeamFormationActionDescription;
import examples.dart.models.ThreatDistribution;
import language.domain.metrics.CountQFunction;
import language.domain.models.ActionDefinition;
import language.domain.models.StateVarDefinition;
import language.exceptions.IncompatibleActionException;
import language.mdp.ActionSpace;
import language.mdp.FactoredPSO;
import language.mdp.Precondition;
import language.mdp.QSpace;
import language.mdp.StateSpace;
import language.mdp.StateVarTuple;
import language.mdp.TransitionFunction;
import language.mdp.XMDP;
import language.objectives.AttributeCostFunction;
import language.objectives.CostFunction;
import solver.common.Constants;

public class DartXMDPBuilder {

	// --- TeamAltitude --- //

	// Team's altitude state variable
	private StateVarDefinition<TeamAltitude> teamAltDef;

	// Possible altitude changes within 1 period
	private TeamAltitude altChange1 = new TeamAltitude(1);
	private TeamAltitude altChange2 = new TeamAltitude(2);

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

	// Available team formations (known, fixed)
	private TeamFormation looseForm = new TeamFormation("loose");
	private TeamFormation tightForm = new TeamFormation("tight");

	// Change formation actions
	private ChangeFormAction goLoose = new ChangeFormAction(looseForm);
	private ChangeFormAction goTight = new ChangeFormAction(tightForm);

	// ChangeForm action definition
	private ActionDefinition<ChangeFormAction> changeFormDef = new ActionDefinition<>("changeForm", goLoose, goTight);

	// ------ //

	// --- TeamECM --- //

	// Team's ECM state variable
	private StateVarDefinition<TeamECM> teamECMDef;

	// ECM states (known, fixed)
	private TeamECM ecmOn = new TeamECM(true);
	private TeamECM ecmOff = new TeamECM(false);

	// Switch ECM actions
	private SwitchECMAction turnECMOn = new SwitchECMAction(ecmOn);
	private SwitchECMAction turnECMOff = new SwitchECMAction(ecmOff);

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

	// Composite durative action: for "route segment" and "teamDestroyed" independent effect classes
	private ActionDefinition<IDurativeAction> durativeDef = new ActionDefinition<>("durative", incAlt1, incAlt2,
			decAlt1, decAlt2, fly);

	// --- QFunctions --- //
	private CountQFunction<IDurativeAction, DetectTargetDomain, MissTargetEvent> missTargetQFunction;
	private DestroyedProbabilityQFunction destroyedProbQFunction;

	// List of route segment objects in the order of their numbers
	// To be used when adding attribute values to each route segment
	private List<RouteSegment> mOrderedSegments = new ArrayList<>();

	public DartXMDPBuilder() {
		// Constructor may take as input other DSMs
	}

	public XMDP buildXMDP(int maxAltLevel, int horizon, double[] expThreatProbs, double[] expTargetProbs,
			int iniAltLevel, String iniForm, boolean iniECM, double targetWeight, double threatWeight)
			throws IncompatibleActionException {
		StateSpace stateSpace = buildStateSpace(maxAltLevel, horizon, expThreatProbs, expTargetProbs);
		ActionSpace actionSpace = buildActionSpace();
		StateVarTuple initialState = buildInitialState(iniAltLevel, iniForm, iniECM);
		StateVarTuple goal = buildGoal(horizon);
		TransitionFunction transFunction = buildTransitionFunction(maxAltLevel, horizon);
		QSpace qSpace = buildQFunctions();
		CostFunction costFunction = buildCostFunction(targetWeight, threatWeight);
		return new XMDP(stateSpace, actionSpace, initialState, goal, transFunction, qSpace, costFunction);
	}

	private StateSpace buildStateSpace(int maxAltLevel, int horizon, double[] expThreatProbs, double[] expTargetProbs) {
		// Possible values of teamAltitude
		Set<TeamAltitude> alts = new HashSet<>();
		for (int i = 0; i < maxAltLevel; i++) {
			TeamAltitude teamAlt = new TeamAltitude(i + 1);
			alts.add(teamAlt);
		}
		teamAltDef = new StateVarDefinition<>("altitude", alts);

		// Possible values of teamFormation
		teamFormDef = new StateVarDefinition<>("formation", tightForm, looseForm);

		// Possible values of teamECM
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

	private TransitionFunction buildTransitionFunction(int maxAltLevel, int horizon)
			throws IncompatibleActionException {
		// IncAlt and DecAlt: for "teamAltitude" effect class
		// Preconditions
		Precondition<IncAltAction> preIncAlt = new Precondition<>(incAltDef);
		Precondition<DecAltAction> preDecAlt = new Precondition<>(decAltDef);

		for (TeamAltitude altitude : teamAltDef.getPossibleValues()) {

			// Precondition for IncAlt actions
			for (IncAltAction incAlt : incAltDef.getActions()) {
				if (altitude.getAltitudeLevel() <= maxAltLevel - incAlt.getAltitudeChange().getAltitudeLevel()) {
					preIncAlt.add(incAlt, teamAltDef, altitude);
				}
			}

			// Precondition for DecAlt actions
			for (DecAltAction decAlt : decAltDef.getActions()) {
				// Lowest altitude level is 1
				if (altitude.getAltitudeLevel() >= 1 + decAlt.getAltitudeChange().getAltitudeLevel()) {
					preDecAlt.add(decAlt, teamAltDef, altitude);
				}
			}
		}

		// Action descriptions for teamAltitude effect class of IncAlt and DecAlt actions
		IncAltAltitudeActionDescription incAltAltActionDesc = new IncAltAltitudeActionDescription(incAltDef, preIncAlt,
				teamAltDef);
		DecAltAltitudeActionDescription decAltAltActionDesc = new DecAltAltitudeActionDescription(decAltDef, preDecAlt,
				teamAltDef);

		// PSOs of IncAlt and DecAlt
		// Note: IncAlt and DecAlt also affect "route segment" and "teamDestroyed" variables, but those effects are
		// defined using composite action
		FactoredPSO<IncAltAction> incAltPSO = new FactoredPSO<>(incAltDef, preIncAlt);
		incAltPSO.addActionDescription(incAltAltActionDesc);

		FactoredPSO<DecAltAction> decAltPSO = new FactoredPSO<>(decAltDef, preDecAlt);
		decAltPSO.addActionDescription(decAltAltActionDesc);

		// ChangeForm: for "teamFormation" effect class
		// Precondition
		Precondition<ChangeFormAction> preChangeForm = new Precondition<>(changeFormDef);
		preChangeForm.add(goLoose, teamFormDef, tightForm);
		preChangeForm.add(goTight, teamFormDef, looseForm);

		// Action description for teamFormation (of ChangeForm)
		TeamFormationActionDescription formActionDesc = new TeamFormationActionDescription(changeFormDef, preChangeForm,
				teamFormDef);

		// PSO of ChangeForm
		FactoredPSO<ChangeFormAction> changeFormPSO = new FactoredPSO<>(changeFormDef, preChangeForm);
		changeFormPSO.addActionDescription(formActionDesc);

		// SwitchECM: for "teamECM" effect class
		// Precondition
		Precondition<SwitchECMAction> preSwitchECM = new Precondition<>(switchECMDef);
		preSwitchECM.add(turnECMOn, teamECMDef, ecmOff);
		preSwitchECM.add(turnECMOff, teamECMDef, ecmOn);

		// Action description for teamECM (of SwitchECM)
		TeamECMActionDescription ecmActionDesc = new TeamECMActionDescription(switchECMDef, preSwitchECM, teamECMDef);

		// PSO of SwitchECM
		FactoredPSO<SwitchECMAction> switchECMPSO = new FactoredPSO<>(switchECMDef, preSwitchECM);
		switchECMPSO.addActionDescription(ecmActionDesc);

		// Composite durative action: for "route segment" and "teamDestroyed" independent effect classes
		// Precondition
		Precondition<IDurativeAction> preDurative = new Precondition<>(durativeDef);
		for (IDurativeAction durative : durativeDef.getActions()) {
			for (int i = 0; i < horizon - 1; i++) {
				// Each durative action takes 1 period (= 1 segment) to execute
				preDurative.add(durative, segmentDef, mOrderedSegments.get(i));
			}
		}

		// Action description for route segment (of durative actions)
		RouteSegmentActionDescription segmentActionDesc = new RouteSegmentActionDescription(durativeDef, preDurative,
				segmentDef);

		// Action description for teamDestroyed (of durative actions)
		TeamDestroyedActionDescription destroyedActionDesc = new TeamDestroyedActionDescription(durativeDef,
				preDurative, teamAltDef, teamFormDef, teamECMDef, segmentDef, teamDestroyedDef);

		// PSO of composite durative action
		FactoredPSO<IDurativeAction> durativePSO = new FactoredPSO<>(durativeDef, preDurative);
		durativePSO.addActionDescription(segmentActionDesc);
		durativePSO.addActionDescription(destroyedActionDesc);

		// Transition function
		TransitionFunction transFunction = new TransitionFunction();
		transFunction.add(incAltPSO);
		transFunction.add(decAltPSO);
		transFunction.add(changeFormPSO);
		transFunction.add(switchECMPSO);
		transFunction.add(durativePSO);
		return transFunction;
	}

	private QSpace buildQFunctions() {
		// Miss target event
		DetectTargetDomain detectTargetDomain = new DetectTargetDomain(teamAltDef, teamFormDef, teamECMDef, segmentDef,
				durativeDef);
		MissTargetEvent missTargetEvent = new MissTargetEvent(detectTargetDomain);
		missTargetQFunction = new CountQFunction<>(missTargetEvent);

		// Probability of being destroyed by threat
		DestroyedProbabilityDomain destroyedProbDomain = new DestroyedProbabilityDomain(teamDestroyedDef, teamAltDef,
				teamFormDef, teamECMDef, segmentDef, durativeDef);
		destroyedProbQFunction = new DestroyedProbabilityQFunction(destroyedProbDomain);

		QSpace qSpace = new QSpace();
		qSpace.addQFunction(missTargetQFunction);
		qSpace.addQFunction(destroyedProbQFunction);
		return qSpace;
	}

	private CostFunction buildCostFunction(double targetWeight, double threatWeight) {
		AttributeCostFunction<CountQFunction<IDurativeAction, DetectTargetDomain, MissTargetEvent>> targetAttrCostFunc = new AttributeCostFunction<>(
				missTargetQFunction, 0, 1);
		AttributeCostFunction<DestroyedProbabilityQFunction> threatAttrCostFunc = new AttributeCostFunction<>(
				destroyedProbQFunction, 0, 1);

		CostFunction costFunction = new CostFunction(Constants.SSP_COST_OFFSET);
		costFunction.put(targetAttrCostFunc, targetWeight);
		costFunction.put(threatAttrCostFunc, threatWeight);
		return costFunction;
	}
}
