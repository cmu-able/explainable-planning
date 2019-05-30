package mobilerobot.study.utilities;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.json.simple.parser.ParseException;

import examples.common.DSMException;
import language.exceptions.XMDPException;
import mobilerobot.missiongen.MissionJSONGenerator;
import mobilerobot.study.prefalign.PrefAlignQuestionGenerator;
import mobilerobot.study.prefinterp.PrefInterpQuestionGenerator;
import mobilerobot.utilities.FileIOUtils;
import prism.PrismException;
import solver.prismconnector.exceptions.ResultParsingException;

public class MainQuestionGenerator {

	private IQuestionGenerator mQuestionGenerator;

	public MainQuestionGenerator(IQuestionGenerator questionGenerator) {
		mQuestionGenerator = questionGenerator;
	}

	public void generateAllQuestions(File mapsDir, String startNodeID, String goalNodeID) throws ResultParsingException,
			URISyntaxException, IOException, ParseException, DSMException, XMDPException, PrismException {
		int nextMissionIndex = 0;
		for (File mapJsonFile : mapsDir.listFiles()) {
			nextMissionIndex = mQuestionGenerator.generateQuestions(mapJsonFile, startNodeID, goalNodeID,
					nextMissionIndex);
		}
	}

	public static void main(String[] args) throws URISyntaxException, ResultParsingException, IOException,
			ParseException, DSMException, XMDPException, PrismException {
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
		if (studyName.equals("PrefInterp")) {
			questionGenerator = new PrefInterpQuestionGenerator();
		} else if (studyName.equals("PrefAlign")) {
			questionGenerator = new PrefAlignQuestionGenerator();
		} else {
			throw new IllegalArgumentException("Invalid Study Name");
		}

		MainQuestionGenerator mainGenerator = new MainQuestionGenerator(questionGenerator);
		mainGenerator.generateAllQuestions(mapsDir, startNodeID, goalNodeID);
	}

}
