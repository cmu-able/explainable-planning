package examples.dart.viz;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.parser.ParseException;

import examples.common.DSMException;
import examples.dart.dsm.DartMission;
import examples.dart.dsm.DartMissionReader;
import examples.dart.dsm.DartXMDPBuilder;
import examples.dart.models.DecAltAction;
import examples.dart.models.IDurativeAction;
import examples.dart.models.IncAltAction;
import examples.dart.models.RouteSegment;
import examples.dart.models.TeamAltitude;
import examples.dart.models.TeamDestroyed;
import examples.dart.models.TeamECM;
import examples.dart.models.TeamFormation;
import language.domain.models.IAction;
import language.domain.models.IStateVarInt;
import language.domain.models.StateVarDefinition;
import language.exceptions.VarNotFoundException;
import language.exceptions.XMDPException;
import language.mdp.StateVarTuple;
import language.mdp.XMDP;
import language.policy.Decision;
import language.policy.Policy;
import uiconnector.PolicyReader;

public class DartPolicyViz {

	private static final String DOMAIN_PATH = "/Users/rsukkerd/Projects/explainable-planning/XPlanning/data/dart";

	private static final List<String> DURATIVE_ACTIONS = Arrays.asList("incAlt", "decAlt", "fly", "tick");

	private static final int SEGMENT_LEN = 10;

	// Symbols from DARTSim
	private static final char LOOSE_FORM_ECM_ON = '#';
	private static final char TIGHT_FORM_ECM_OFF = '*';
	private static final char LOOSE_FORM_ECM_OFF = '@';
	private static final char TIGHT_FORM_ECM_ON = '0';
	private static final char THREAT = '^';
	private static final char TARGET = 'T';

	private DartMissionReader mMissionReader = new DartMissionReader();
	private DartMission mMission;
	private PolicyReader mPolicyReader;

	// State variables to visualize
	private StateVarDefinition<TeamAltitude> mAltitudeDef;
	private StateVarDefinition<RouteSegment> mSegmentDef;
	private StateVarDefinition<TeamFormation> mFormDef;
	private StateVarDefinition<TeamECM> mECMDef;
	private StateVarDefinition<TeamDestroyed> mDestroyedDef;

	public DartPolicyViz(File missionFile) throws DSMException, XMDPException {
		mMission = mMissionReader.readDartMission(missionFile);

		DartXMDPBuilder xmdpBuilder = new DartXMDPBuilder();
		XMDP xmdp = xmdpBuilder.buildXMDP(mMission);
		mPolicyReader = new PolicyReader(xmdp);

		mAltitudeDef = xmdp.getStateSpace().getStateVarDefinition("altitude");
		mSegmentDef = xmdp.getStateSpace().getStateVarDefinition("segment");
		mFormDef = xmdp.getStateSpace().getStateVarDefinition("formation");
		mECMDef = xmdp.getStateSpace().getStateVarDefinition("ecm");
		mDestroyedDef = xmdp.getStateSpace().getStateVarDefinition("destroyed");
	}

	public String visualizePolicy(File policyJsonFile) throws VarNotFoundException, IOException, ParseException {
		Policy policy = mPolicyReader.readPolicy(policyJsonFile);

		int maxAltitude = mMission.getMaximumAltitudeLevel();
		int horizon = mMission.getHorizon();

		StringBuilder builder = new StringBuilder();

		// Destination state of a durative action, whose segment is at the horizon
		StateVarTuple destStateAtHorizon = getDestStateAtHorizon(policy, horizon);
		TeamAltitude destAltitudeAtHorizon = destStateAtHorizon.getStateVarValue(TeamAltitude.class, mAltitudeDef);

		// Altitude=0 is ground level
		for (int altitude = maxAltitude; altitude > 0; altitude--) {
			// Decisions where the source state is at this altitude
			Set<Decision> decisionsAtAltitude = filterDecisionsWithIntValue(policy, TeamAltitude.class, mAltitudeDef,
					altitude);

			if (decisionsAtAltitude.isEmpty() && destAltitudeAtHorizon.getAltitudeLevel() != altitude) {
				// No decisions at this altitude (when team is alive), and
				// Team is not at this altitude when it reaches the horizon

				// Draw the next lower altitude level
				builder.append("\n");
				continue;
			}

			buildRowAtAltitude(altitude, horizon, decisionsAtAltitude, destStateAtHorizon, builder);

			// Draw the next lower altitude level
			builder.append("\n");
		}

		// Draw threats on the ground level (altitude=0)
		double[] expThreatProbs = mMission.getExpectedThreatProbabilities();
		buildRow(expThreatProbs, THREAT, builder);

		// Draw targets below the threats
		double[] expTargetProbs = mMission.getExpectedTargetProbabilities();
		builder.append("\n");
		buildRow(expTargetProbs, TARGET, builder);

		return builder.toString();
	}

	private void buildRowAtAltitude(int altitude, int horizon, Set<Decision> decisionsAtAltitude,
			StateVarTuple destStateAtHorizon, StringBuilder builder) throws VarNotFoundException {
		// Segment=1 is the first route segment
		// Segment=horizon is the last route segment
		for (int segment = 1; segment <= horizon; segment++) {
			char teamConfigSymbol;
			TeamAltitude destAltitudeAtHorizon = destStateAtHorizon.getStateVarValue(TeamAltitude.class, mAltitudeDef);

			if (segment < horizon) {
				Set<Decision> decisionsAtSegment = filterDecisionsWithIntValue(decisionsAtAltitude, RouteSegment.class,
						mSegmentDef, segment);

				if (decisionsAtSegment.isEmpty()) {
					// No decisions at this segment (when team is alive)

					// Draw empty space for the whole route segment
					builder.append(StringUtils.repeat(' ', SEGMENT_LEN));
					continue;
				}

				// Last action in the decision period (corresponding to the current route segment)
				Decision decision = getDecisionWithDurativeAction(decisionsAtSegment);

				teamConfigSymbol = drawTeamConfiguration(decision.getState());
			} else if (destAltitudeAtHorizon.getAltitudeLevel() == altitude) {
				// Team is at this altitude when it reaches the horizon
				teamConfigSymbol = drawTeamConfiguration(destStateAtHorizon);
			} else {
				teamConfigSymbol = ' ';
			}

			// Draw team configuration at <altitude, segment>
			builder.append(teamConfigSymbol);

			// Draw empty space for the rest of the route segment
			builder.append(StringUtils.repeat(' ', SEGMENT_LEN - 1));
		}
	}

	private char drawTeamConfiguration(StateVarTuple teamState) throws VarNotFoundException {
		TeamFormation teamForm = teamState.getStateVarValue(TeamFormation.class, mFormDef);
		TeamECM teamECM = teamState.getStateVarValue(TeamECM.class, mECMDef);

		// Draw team configuration at <altitude, segment>
		if (teamForm.getFormation().equals("loose") && teamECM.isECMOn()) {
			return LOOSE_FORM_ECM_ON;
		} else if (teamForm.getFormation().equals("tight") && !teamECM.isECMOn()) {
			return TIGHT_FORM_ECM_OFF;
		} else if (teamForm.getFormation().equals("loose") && !teamECM.isECMOn()) {
			return LOOSE_FORM_ECM_OFF;
		} else if (teamForm.getFormation().equals("tight") && teamECM.isECMOn()) {
			return TIGHT_FORM_ECM_ON;
		}

		throw new IllegalArgumentException("Team state: " + teamState + " does not have a valid configuration");
	}

	private void buildRow(double[] expProbs, char symbol, StringBuilder builder) {
		for (double expProb : expProbs) {
			if (expProb > 0) {
				String symbolStr = String.format("%s (%.2f)", symbol, expProb);

				// Draw threat/target and its expected probability
				builder.append(symbolStr);

				// Draw empty space for the rest of the route segment
				builder.append(StringUtils.repeat(' ', SEGMENT_LEN - symbolStr.length()));
			} else {
				// Draw empty space for the whole route segment
				builder.append(StringUtils.repeat(' ', SEGMENT_LEN));
			}
		}
	}

	private <E extends IStateVarInt> Set<Decision> filterDecisionsWithIntValue(Iterable<Decision> decisions,
			Class<E> varType, StateVarDefinition<E> intVarDef, int value) throws VarNotFoundException {
		Set<Decision> res = new HashSet<>();
		for (Decision decision : decisions) {
			E varValue = decision.getState().getStateVarValue(varType, intVarDef);
			TeamDestroyed destroyed = decision.getState().getStateVarValue(TeamDestroyed.class, mDestroyedDef);

			// Only get decisions when team is alive
			if (!destroyed.isDestroyed() && varValue.getValue() == value) {
				res.add(decision);
			}
		}
		return res;
	}

	private StateVarTuple getDestStateAtHorizon(Iterable<Decision> decisions, int horizon) throws VarNotFoundException {
		for (Decision decision : decisions) {
			RouteSegment srcSegment = decision.getState().getStateVarValue(RouteSegment.class, mSegmentDef);
			TeamDestroyed destroyed = decision.getState().getStateVarValue(TeamDestroyed.class, mDestroyedDef);
			IAction action = decision.getAction();

			// Only get decisions when:
			// - team is alive
			// - team is at the segment prior to the horizon
			// - action is a durative action -- that will move team to the horizon
			if (!destroyed.isDestroyed() && srcSegment.getSegment() == horizon - 1
					&& action instanceof IDurativeAction) {

				// Source and destination teamAltitude
				TeamAltitude srcAlt = decision.getState().getStateVarValue(TeamAltitude.class, mAltitudeDef);
				TeamAltitude destAlt = srcAlt;

				if (action instanceof IncAltAction) {
					IncAltAction incAlt = (IncAltAction) action;
					TeamAltitude altChange = incAlt.getAltitudeChange();
					destAlt = new TeamAltitude(srcAlt.getAltitudeLevel() + altChange.getAltitudeLevel());
				} else if (action instanceof DecAltAction) {
					DecAltAction decAlt = (DecAltAction) action;
					TeamAltitude altChange = decAlt.getAltitudeChange();
					destAlt = new TeamAltitude(srcAlt.getAltitudeLevel() - altChange.getAltitudeLevel());
				} // Fly action does not change teamAltitude

				// Durative action advances segment by 1
				RouteSegment destSegment = mSegmentDef.getPossibleValues().stream()
						.filter(segment -> segment.getSegment() == horizon).findFirst().get();

				// Durative action does not change teamFormation or teamECM
				TeamFormation teamForm = decision.getState().getStateVarValue(TeamFormation.class, mFormDef);
				TeamECM teamECM = decision.getState().getStateVarValue(TeamECM.class, mECMDef);

				StateVarTuple destStateAtHorizon = new StateVarTuple();
				destStateAtHorizon.addStateVar(mSegmentDef.getStateVar(destSegment));
				destStateAtHorizon.addStateVar(mAltitudeDef.getStateVar(destAlt));
				destStateAtHorizon.addStateVar(mFormDef.getStateVar(teamForm));
				destStateAtHorizon.addStateVar(mECMDef.getStateVar(teamECM));
				// Only visualize the team state when it is alive
				destStateAtHorizon.addStateVar(mDestroyedDef.getStateVar(destroyed));
				return destStateAtHorizon;
			}
		}

		throw new IllegalStateException("Destination state at horizon is not found");
	}

	private Decision getDecisionWithDurativeAction(Iterable<Decision> decisionsAtSegment) {
		for (Decision decision : decisionsAtSegment) {
			String namePrefix = decision.getAction().getNamePrefix();

			if (DURATIVE_ACTIONS.contains(namePrefix)) {
				return decision;
			}
		}

		throw new IllegalArgumentException(decisionsAtSegment.toString() + "does not contain any durative action");
	}

	public static void main(String[] args) throws DSMException, XMDPException, IOException, ParseException {
		String missionName = args[0];
		String missionFilename = missionName + ".txt";

		Path missionsDirPath = Paths.get(DOMAIN_PATH, "missions");
		Path policiesDirPath = Paths.get(DOMAIN_PATH, "policies", missionName);

		Path missionFilePath = missionsDirPath.resolve(missionFilename);

		File missionFile = missionFilePath.toFile();
		File policiesDir = policiesDirPath.toFile();

		DartPolicyViz policyViz = new DartPolicyViz(missionFile);

		for (File policyJsonFile : policiesDir.listFiles()) {
			String viz = policyViz.visualizePolicy(policyJsonFile);

			File policyVizFile = new File("tmpdata", policyJsonFile.getName().replace(".json", ".txt"));
			try (FileWriter writer = new FileWriter(policyVizFile)) {
				writer.write(viz);
				writer.flush();
			}
		}
	}

}
