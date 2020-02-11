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
import examples.dart.models.RouteSegment;
import examples.dart.models.TeamAltitude;
import examples.dart.models.TeamDestroyed;
import examples.dart.models.TeamECM;
import examples.dart.models.TeamFormation;
import language.domain.models.IStateVarInt;
import language.domain.models.StateVarDefinition;
import language.exceptions.VarNotFoundException;
import language.exceptions.XMDPException;
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
	private XMDP mXMDP;
	private PolicyReader mPolicyReader;

	public DartPolicyViz(File missionFile) throws DSMException, XMDPException {
		mMission = mMissionReader.readDartMission(missionFile);

		DartXMDPBuilder xmdpBuilder = new DartXMDPBuilder();
		mXMDP = xmdpBuilder.buildXMDP(mMission);

		mPolicyReader = new PolicyReader(mXMDP);
	}

	public String visualizePolicy(File policyJsonFile) throws VarNotFoundException, IOException, ParseException {
		Policy policy = mPolicyReader.readPolicy(policyJsonFile);

		int maxAltitude = mMission.getMaximumAltitudeLevel();
		int horizon = mMission.getHorizon();

		StateVarDefinition<TeamAltitude> altitudeDef = mXMDP.getStateSpace().getStateVarDefinition("altitude");
		StateVarDefinition<RouteSegment> segmentDef = mXMDP.getStateSpace().getStateVarDefinition("segment");
		StateVarDefinition<TeamFormation> formDef = mXMDP.getStateSpace().getStateVarDefinition("formation");
		StateVarDefinition<TeamECM> ecmDef = mXMDP.getStateSpace().getStateVarDefinition("ecm");

		StringBuilder builder = new StringBuilder();

		// Altitude=0 is ground level
		for (int altitude = maxAltitude; altitude > 0; altitude--) {
			Set<Decision> decisionsAtAltitude = filterDecisionsWithIntValue(policy, TeamAltitude.class, altitudeDef,
					altitude);

			if (decisionsAtAltitude.isEmpty()) {
				// No decisions at this altitude (when team is alive)

				// Draw the next lower altitude level
				builder.append("\n");
				continue;
			}

			buildRowAtAltitude(horizon, decisionsAtAltitude, segmentDef, formDef, ecmDef, builder);

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

	private void buildRowAtAltitude(int horizon, Set<Decision> decisionsAtAltitude,
			StateVarDefinition<RouteSegment> segmentDef, StateVarDefinition<TeamFormation> formDef,
			StateVarDefinition<TeamECM> ecmDef, StringBuilder builder) throws VarNotFoundException {
		// Segment=1 is the first route segment
		for (int segment = 1; segment < horizon; segment++) {
			Set<Decision> decisionsAtSegment = filterDecisionsWithIntValue(decisionsAtAltitude, RouteSegment.class,
					segmentDef, segment);

			if (decisionsAtSegment.isEmpty()) {
				// No decisions at this segment (when team is alive)

				// Draw empty space for the whole route segment
				builder.append(StringUtils.repeat(' ', SEGMENT_LEN));
				continue;
			}

			// Last action in the decision period (corresponding to the current route segment)
			Decision decision = getDecisionWithDurativeAction(decisionsAtSegment);

			TeamFormation teamForm = decision.getState().getStateVarValue(TeamFormation.class, formDef);
			TeamECM teamECM = decision.getState().getStateVarValue(TeamECM.class, ecmDef);

			// Draw team configuration at <altitude, segment>
			if (teamForm.getFormation().equals("loose") && teamECM.isECMOn()) {
				builder.append(LOOSE_FORM_ECM_ON);
			} else if (teamForm.getFormation().equals("tight") && !teamECM.isECMOn()) {
				builder.append(TIGHT_FORM_ECM_OFF);
			} else if (teamForm.getFormation().equals("loose") && !teamECM.isECMOn()) {
				builder.append(LOOSE_FORM_ECM_OFF);
			} else if (teamForm.getFormation().equals("tight") && teamECM.isECMOn()) {
				builder.append(TIGHT_FORM_ECM_ON);
			}

			// Draw empty space for the rest of the route segment
			builder.append(StringUtils.repeat(' ', SEGMENT_LEN - 1));
		}
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
		StateVarDefinition<TeamDestroyed> destroyedDef = mXMDP.getStateSpace().getStateVarDefinition("destroyed");

		Set<Decision> res = new HashSet<>();
		for (Decision decision : decisions) {
			E varValue = decision.getState().getStateVarValue(varType, intVarDef);
			TeamDestroyed destroyed = decision.getState().getStateVarValue(TeamDestroyed.class, destroyedDef);

			// Only get decisions when team is alive
			if (!destroyed.isDestroyed() && varValue.getValue() == value) {
				res.add(decision);
			}
		}
		return res;
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
