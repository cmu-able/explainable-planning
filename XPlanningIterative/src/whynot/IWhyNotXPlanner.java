package whynot;

import java.io.File;
import java.io.IOException;

import org.json.simple.parser.ParseException;

import examples.common.DSMException;
import gurobi.GRBException;
import language.exceptions.XMDPException;
import models.explanation.HPolicyExplanation;
import prism.PrismException;
import solver.prismconnector.exceptions.PrismConnectorException;

public interface IWhyNotXPlanner {

	public HPolicyExplanation answerWhyNotQuery(File problemFile, File queryPolicyJsonFile, String whyNotQueryStr)
			throws DSMException, XMDPException, PrismConnectorException, IOException, ParseException, PrismException,
			GRBException;
}
