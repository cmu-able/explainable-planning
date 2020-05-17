package mobilerobot.study.utilities;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.json.simple.parser.ParseException;

import examples.common.DSMException;
import gurobi.GRBException;
import language.exceptions.XMDPException;
import mobilerobot.missiongen.MissionJSONGenerator;
import mobilerobot.study.prefalign.PrefAlignQuestionGenerator;
import mobilerobot.utilities.FileIOUtils;
import prism.PrismException;
import solver.prismconnector.exceptions.PrismConnectorException;

public class MainQuestionGenerator {

	private IQuestionGenerator mQuestionGenerator;

	public MainQuestionGenerator(IQuestionGenerator questionGenerator) {
		mQuestionGenerator = questionGenerator;
	}

	public void generateAllQuestions(File mapsDir, String startNodeID, String goalNodeID)
			throws URISyntaxException, IOException, ParseException, DSMException, XMDPException, PrismException,
			PrismConnectorException, GRBException {
		int nextMissionIndex = 0;
		for (File mapJsonFile : mapsDir.listFiles()) {
			nextMissionIndex = mQuestionGenerator.generateQuestions(mapJsonFile, startNodeID, goalNodeID,
					nextMissionIndex);
		}
	}

	public static void main(String[] args) throws URISyntaxException, IOException, ParseException, DSMException,
			XMDPException, PrismException, PrismConnectorException, GRBException {
		String studyName = args[0];
		String startNodeID;
		String goalNodeID;
		if (args.length > 2) {
			startNodeID = args[1];
			goalNodeID = args[2];
		} else {
			startNodeID = MissionJSONGenerator.DEFAULT_START_NODE_ID;
			goalNodeID = MissionJSONGenerator.DEFAULT_GOAL_NODE_ID;
		}

		File mapsDir = FileIOUtils.getMapsResourceDir(MissionJSONGenerator.class);
		IQuestionGenerator questionGenerator;
		if (studyName.equals("PrefAlign")) {
			questionGenerator = new PrefAlignQuestionGenerator(mapsDir);
		} else {
			throw new IllegalArgumentException("Invalid Study Name");
		}

		MainQuestionGenerator mainGenerator = new MainQuestionGenerator(questionGenerator);
		mainGenerator.generateAllQuestions(mapsDir, startNodeID, goalNodeID);
	}

}
