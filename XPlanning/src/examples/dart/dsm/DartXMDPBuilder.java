package examples.dart.dsm;

import java.util.HashSet;
import java.util.Set;

import examples.dart.models.RouteSegment;
import examples.dart.models.TargetDistribution;
import examples.dart.models.TeamAltitude;
import examples.dart.models.TeamECM;
import examples.dart.models.TeamFormation;
import examples.dart.models.ThreatDistribution;
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

	// --- TeamFormation --- //
	// Team's formation state variable
	private StateVarDefinition<TeamFormation> teamFormDef;

	// --- TeamECM --- //
	// Team's ECM state variable
	private StateVarDefinition<TeamECM> teamECMDef;

	// --- RouteSegment --- //
	// Team's current route segment state variable
	private StateVarDefinition<RouteSegment> segmentDef;

	public DartXMDPBuilder() {
		// Constructor may take as input other DSMs
	}

	public XMDP buildXMDP(int maxAlt, int horizon, double[] expThreatProbs, double[] expTargetProbs) {
		StateSpace stateSpace = buildStateSpace(maxAlt, horizon, expThreatProbs, expTargetProbs);
		ActionSpace actionSpace = buildActionSpace();
		StateVarTuple initialState = buildInitialState();
		StateVarTuple goal = buildGoal();
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
		}
		segmentDef = new StateVarDefinition<>("segment", segments);

		StateSpace stateSpace = new StateSpace();
		stateSpace.addStateVarDefinition(teamAltDef);
		stateSpace.addStateVarDefinition(teamFormDef);
		stateSpace.addStateVarDefinition(teamECMDef);
		stateSpace.addStateVarDefinition(segmentDef);
		return stateSpace;
	}

	private ActionSpace buildActionSpace() {
		return null;
	}

	private StateVarTuple buildInitialState() {
		return null;
	}

	private StateVarTuple buildGoal() {
		return null;
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
