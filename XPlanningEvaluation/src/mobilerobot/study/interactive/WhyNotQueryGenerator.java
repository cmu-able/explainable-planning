package mobilerobot.study.interactive;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.json.simple.parser.ParseException;

import examples.common.DSMException;
import examples.common.IXMDPLoader;
import examples.mobilerobot.demo.MobileRobotXMDPLoader;
import language.exceptions.XMDPException;
import language.mdp.XMDP;
import language.policy.Policy;
import mobilerobot.missiongen.MissionJSONGenerator;
import mobilerobot.study.utilities.ExplanationUtils;
import mobilerobot.study.utilities.QuestionUtils;
import mobilerobot.utilities.FileIOUtils;
import uiconnector.PolicyReader;

public class WhyNotQueryGenerator {

	private IXMDPLoader mXMDPLoader;

	public WhyNotQueryGenerator(File mapsJsonDir) {
		mXMDPLoader = new MobileRobotXMDPLoader(mapsJsonDir);
	}

	public void generateAllWhyNotQueries(File questionsRootDir)
			throws IOException, DSMException, XMDPException, ParseException {
		File[] questionDirsIn = QuestionUtils.listQuestionDirs(questionsRootDir);

		for (File questionDirIn : questionDirsIn) {
			File missionFile = QuestionUtils.getMissionJSONFile(questionDirIn);

			// Create a symbolic link mission[X].json in questionDirOut that points to
			// the mission file in questionDirIn
			File questionDirOut = QuestionUtils.initializeQuestionDir(missionFile, false);

			generateAllWhyNotQueries(questionDirIn, questionDirOut);
		}
	}

	public void generateAllWhyNotQueries(File questionDirIn, File questionDirOut)
			throws DSMException, XMDPException, IOException, ParseException {
		// questionDirIn: /prefaign/questions/question-mission[X]/
		// questionDirOut: /output/question-mission[X]/ to be moved to /interactive/questions/question-mission[X]/

		// Load XMDP from questionDirIn/mission[X].json
		File missionJsonFile = QuestionUtils.getMissionJSONFile(questionDirIn);
		XMDP xmdp = mXMDPLoader.loadXMDP(missionJsonFile);

		// For the purpose of generating queries, we can use mission[X].json with 
		// any solnPolicy.json of any agent in questionDirIn
		WhyNotQueryStringGenerator queryStringGenerator = new WhyNotQueryStringGenerator(xmdp);

		File[] explanationDirs = QuestionUtils.listExplanationDirs(questionDirIn);

		// Create why-not queries for all agents' solution policies
		for (File explanationDir : explanationDirs) {
			int agentIndex = ExplanationUtils.getAgentIndex(explanationDir);

			// Get the agent's mission[Y].json file from questionDirIn/explanation-agent[i]/
			// This is not necessarily the same as mission[X].json
			File agentMissionJsonFile = ExplanationUtils.getMissionJSONFile(explanationDir);

			// Read query policy from the agent's solnPolicy.json in questionDirIn/explanation-agent[i]/
			File queryPolicyJsonFile = new File(explanationDir, "solnPolicy.json");
			PolicyReader policyReader = new PolicyReader(xmdp);
			Policy queryPolicy = policyReader.readPolicy(queryPolicyJsonFile);

			// Generate all allowable why-not queries for the agent's solution policy
			Set<String> whyNotStringQueries = queryStringGenerator.generateAllWhyNotStringQueries(queryPolicy);

			// Write all allowable why-not queries to files
			// Files' structure: questionDirOut/queries-agent[i]/query0/query0.txt ... /query[k]/query[k].txt
			writeQueriesToFiles(questionDirOut, agentIndex, agentMissionJsonFile, queryPolicyJsonFile,
					whyNotStringQueries);
		}
	}

	private void writeQueriesToFiles(File questionDirOut, int agentIndex, File agentMissionJsonFile,
			File queryPolicyJsonFile, Set<String> whyNotStringQueries) throws IOException {
		// Create /queries-agent[i]/ dir under questionDirOut
		Path queriesPath = questionDirOut.toPath().resolve("queries-agent" + agentIndex);
		Files.createDirectory(queriesPath);

		// Create a symbolic link mission[Y].json in questionDirOut/queries-agent[i]/ that points to
		// the agent[i]'s mission file in questionDirIn
		Path agentMissionFileLink = queriesPath.resolve(agentMissionJsonFile.getName());
		Files.createSymbolicLink(agentMissionFileLink, agentMissionJsonFile.toPath());

		// Create a symbolic link queryPolicy.json in questionDirOut/queries-agent[i]/ that points to
		// the agent[i]'s policy in questionDirIn
		Path queryPolicyFileLink = queriesPath.resolve("queryPolicy.json");
		Files.createSymbolicLink(queryPolicyFileLink, queryPolicyJsonFile.toPath());

		// For each query, create /query[j]/ dir under /queries-agent[i]/ dir
		int j = 0;
		for (String queryStr : whyNotStringQueries) {
			Path queryPath = queriesPath.resolve("query" + j);
			Files.createDirectory(queryPath);

			// Create query[j].txt file under /query[j]/ and write the query string
			Path queryTxtFile = Files.createFile(queryPath.resolve("query" + j + ".txt"));
			Files.write(queryTxtFile, queryStr.getBytes());

			j++;
		}
	}

	public static void main(String[] args)
			throws URISyntaxException, IOException, DSMException, XMDPException, ParseException {
		String questionsRootPath = args[0];

		File questionsRootDir = new File(questionsRootPath);
		File mapsJsonDir = FileIOUtils.getMapsResourceDir(MissionJSONGenerator.class);

		WhyNotQueryGenerator queryGenerator = new WhyNotQueryGenerator(mapsJsonDir);
		queryGenerator.generateAllWhyNotQueries(questionsRootDir);
	}
}
