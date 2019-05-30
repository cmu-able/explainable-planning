package mobilerobot.study.utilities;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.json.simple.parser.ParseException;

import examples.common.DSMException;
import language.exceptions.XMDPException;
import prism.PrismException;
import solver.prismconnector.exceptions.ResultParsingException;

public interface IQuestionGenerator {

	public int generateQuestions(File mapJsonFile, String startNodeID, String goalNodeID, int startMissionIndex)
			throws ResultParsingException, URISyntaxException, IOException, ParseException, DSMException, XMDPException,
			PrismException;
}
