package examples.dart.viz;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import examples.common.DSMException;
import examples.dart.dsm.DartMission;
import examples.dart.dsm.DartMissionReader;
import examples.dart.dsm.DartXMDPBuilder;
import examples.dart.models.RouteSegment;
import examples.dart.models.TeamAltitude;
import examples.dart.models.TeamECM;
import examples.dart.models.TeamFormation;
import language.domain.models.IStateVarInt;
import language.domain.models.StateVarDefinition;
import language.exceptions.VarNotFoundException;
import language.exceptions.XMDPException;
import language.mdp.XMDP;
import language.policy.Decision;
import language.policy.Policy;

public class DartPolicyViz {

	private static final List<String> DURATIVE_ACTIONS = Arrays.asList("incAlt", "decAlt", "fly", "tick");

	private static final int SEGMENT_LEN = 5;

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

	public DartPolicyViz(File missionFile) throws DSMException, XMDPException {
		mMission = mMissionReader.readDartMission(missionFile);

		DartXMDPBuilder xmdpBuilder = new DartXMDPBuilder();
		mXMDP = xmdpBuilder.buildXMDP(mMission);
	}

	public String visualizePolicy(Policy policy) throws VarNotFoundException {
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

			// Segment=1 is the first route segment
			for (int segment = 1; segment < horizon; segment++) {
				Set<Decision> decisionsAtSegment = filterDecisionsWithIntValue(decisionsAtAltitude, RouteSegment.class,
						segmentDef, segment);

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

			if (varValue.getValue() == value) {
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

}
