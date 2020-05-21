package mobilerobot.study.interactive;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FilenameUtils;
import org.json.simple.parser.ParseException;

import examples.common.DSMException;
import examples.common.XPlannerOutDirectories;
import examples.mobilerobot.demo.MobileRobotXPlanner;
import explanation.verbalization.VerbalizerSettings;
import gurobi.GRBException;
import language.exceptions.XMDPException;
import main.MobileRobotWhyNotXPlanner;
import mobilerobot.missiongen.MissionJSONGenerator;
import mobilerobot.study.utilities.QuestionUtils;
import mobilerobot.utilities.FileIOUtils;
import prism.PrismException;
import solver.prismconnector.exceptions.PrismConnectorException;

public class WhyNotXPlanningRunner {

	private MobileRobotWhyNotXPlanner mWhyNotXPlanner;
	private XPlannerOutDirectories mOutputDirs;

	public WhyNotXPlanningRunner(File mapsJsonDir) throws IOException {
		XPlannerOutDirectories outputDirs = FileIOUtils.createXPlannerOutDirectories();

		// Configure verbalizer settings
		VerbalizerSettings verbalizerSettings = new VerbalizerSettings();
		verbalizerSettings.setDescribeCosts(false);
		verbalizerSettings.setQADecimalFormatter(MobileRobotXPlanner.getQADecimalFormatter());
		MobileRobotXPlanner.setVerbalizerOrdering(verbalizerSettings);

		mWhyNotXPlanner = new MobileRobotWhyNotXPlanner(mapsJsonDir, outputDirs, verbalizerSettings);
		mOutputDirs = outputDirs;
	}

	/**
	 * Run why-not explanation on a given query.
	 * 
	 * The queries parent directory of the given query directory must contain a symbolic link queryPolicy.json.
	 * 
	 * @param missionJsonFile
	 *            : Mission file or symbolic link to it: mission[X].json
	 * @param queryDir
	 *            : Query_j directory containing query[j].txt
	 * @throws IOException
	 * @throws DSMException
	 * @throws XMDPException
	 * @throws PrismConnectorException
	 * @throws ParseException
	 * @throws PrismException
	 * @throws GRBException
	 */
	public void runQuery(File missionJsonFile, File queryDir) throws IOException, DSMException, XMDPException,
			PrismConnectorException, ParseException, PrismException, GRBException {
		// Queries parent dir: /queries-agent[i]/ contains all allowable queries of the agent[i]'s policy
		File queriesParentDir = queryDir.getParentFile();

		// /queries-agent[i]/ contains a symbolic link queryPolicy.json that points to the agent[i]'s policy
		File queryPolicyJsonFile = new File(queriesParentDir, "queryPolicy.json");

		// Query dir: /query[j]/ contains query[j].txt
		String queryID = queryDir.getName();
		File whyNotQueryFile = new File(queryDir, queryID + ".txt");
		String whyNotQueryStr = new String(Files.readAllBytes(whyNotQueryFile.toPath()));

		mWhyNotXPlanner.answerWhyNotQuery(missionJsonFile, queryPolicyJsonFile, whyNotQueryStr);

		// Copy output HPolicy: hPolicy.json and query explanation: mission[X]_explanation.json to /query[j]/ dir
		copyOutputToQueryDir(missionJsonFile, queryDir);
	}

	private void copyOutputToQueryDir(File missionJsonFile, File queryDir) throws IOException {
		String missionName = FilenameUtils.removeExtension(missionJsonFile.getName());

		// Policies output path: /[XPlannerOutDir]/policies/mission[X]/
		Path policiesOutputPath = mOutputDirs.getPoliciesOutputPath().resolve(missionName);
		String hPolicyFilename = "hPolicy.json";
		Path hPolicyPath = policiesOutputPath.resolve(hPolicyFilename);

		// Copy hPolicy.json from /[XPlannerOutDir]/policies/mission[X]/ to /query[j]/ dir
		Files.copy(hPolicyPath, queryDir.toPath().resolve(hPolicyFilename));

		// Explanations output path: /[XPlannerOutDir]/explanations/
		String queryExplanationFilename = missionName + "_explanation.json";
		Path explanationsOutputPath = mOutputDirs.getExplanationsOutputPath();
		Path queryExplanationPath = explanationsOutputPath.resolve(queryExplanationFilename);

		// Copy mission[X]_explanation.json from /[XPlannerOutDir]/explanations/ to /query[j]/ dir
		Files.copy(queryExplanationPath, queryDir.toPath().resolve(queryExplanationFilename));
	}

	/**
	 * Run all allowable queries of a single question: /question-mission[X]/.
	 * 
	 * Directory structure: /question-mission[X]/ -> /queries-agent[i]/ -> /query[j]/ -> query[j].txt.
	 * 
	 * Directory /question-mission[X]/ contains a symbolic link to mission[X].json. Each /queries-agent[i]/ contains a
	 * symbolic link to the agent[i]'s policy JSON file.
	 * 
	 * @param questionDir
	 *            : Question directory of a particular mission: /question-mission[X]/
	 * @throws GRBException
	 * @throws PrismException
	 * @throws ParseException
	 * @throws PrismConnectorException
	 * @throws XMDPException
	 * @throws DSMException
	 * @throws IOException
	 */
	public void runAllQueriesOfSingleQuestion(File questionDir) throws IOException, DSMException, XMDPException,
			PrismConnectorException, ParseException, PrismException, GRBException {
		// Symbolic link /question-mission[X]/mission[X].json points to the actual mission[X].json file
		// in study.prefalign
		File missionJsonFile = QuestionUtils.getMissionJSONFile(questionDir);

		// /question-mission[X] contains multiple directories /queries-agent[i]/ (and potentially others
		// if we allow build-up queries)
		File[] queriesParentDirs = questionDir.listFiles(File::isDirectory);

		for (File queriesParentDir : queriesParentDirs) {
			// Each /queries-agent[i]/ contains all allowable queries of the agent[i]'s policy (or potentially
			// HPolicy from a prior query)
			File[] queryDirs = queriesParentDir.listFiles(File::isDirectory);

			// Run each query[j] in /queries-agent[i]/
			for (File queryDir : queryDirs) {
				runQuery(missionJsonFile, queryDir);
			}
		}
	}

	public static void main(String[] args) throws URISyntaxException, IOException, DSMException, XMDPException,
			PrismConnectorException, ParseException, PrismException, GRBException {
		String questionsRootPath = args[0];

		File questionsRootDir = new File(questionsRootPath);
		File mapsJsonDir = FileIOUtils.getMapsResourceDir(MissionJSONGenerator.class);

		WhyNotXPlanningRunner runner = new WhyNotXPlanningRunner(mapsJsonDir);

		for (File questionDir : questionsRootDir.listFiles(File::isDirectory)) {
			runner.runAllQueriesOfSingleQuestion(questionDir);
		}
	}

}
