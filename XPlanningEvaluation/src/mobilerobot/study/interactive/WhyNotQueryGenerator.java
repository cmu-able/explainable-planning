package mobilerobot.study.interactive;

import java.io.File;
import java.io.IOException;
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
import mobilerobot.study.utilities.QuestionUtils;
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
		for (int agentIndex = 0; agentIndex < explanationDirs.length; agentIndex++) {

			// Read query policy from the agent's solnPolicy.json in questionDirIn/explanation-agent[Y]/
			File explanationDir = explanationDirs[agentIndex];
			File queryPolicyJsonFile = new File(explanationDir, "solnPolicy.json");
			PolicyReader policyReader = new PolicyReader(xmdp);
			Policy queryPolicy = policyReader.readPolicy(queryPolicyJsonFile);

			// Generate all allowable why-not queries for the agent's solution policy
			Set<String> whyNotStringQueries = queryStringGenerator.generateAllWhyNotStringQueries(queryPolicy);

			// Write all allowable why-not queries to files
			// Files' structure: questionDirOut/queries-agent[i]/query0/query0.txt ... /query[k]/query[k].txt
			writeQueriesToFiles(whyNotStringQueries, questionDirOut, agentIndex);
		}
	}

	private void writeQueriesToFiles(Set<String> whyNotStringQueries, File questionDirOut, int agentIndex)
			throws IOException {
		// Create /queries-agent[i]/ dir under questionDirOut
		Path queriesPath = questionDirOut.toPath().resolve("queries-agent" + agentIndex);
		Files.createDirectory(queriesPath);

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
}
