package mobilerobot.study.utilities;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.json.simple.parser.ParseException;

import examples.common.DSMException;
import gurobi.GRBException;
import language.exceptions.XMDPException;
import prism.PrismException;
import solver.prismconnector.exceptions.PrismConnectorException;

public interface IQuestionGenerator {

	public int generateQuestions(File mapJsonFile, String startNodeID, String goalNodeID, int startMissionIndex)
			throws URISyntaxException, IOException, ParseException, DSMException, XMDPException, PrismException,
			PrismConnectorException, GRBException;
}
