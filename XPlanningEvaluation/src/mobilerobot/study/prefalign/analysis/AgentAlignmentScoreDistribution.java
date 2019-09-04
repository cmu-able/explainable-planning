package mobilerobot.study.prefalign.analysis;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URISyntaxException;

import org.json.simple.parser.ParseException;

import mobilerobot.study.prefalign.LinkedPrefAlignQuestions;
import mobilerobot.study.prefalign.PrefAlignQuestionLinker;
import mobilerobot.study.utilities.QuestionUtils;
import mobilerobot.utilities.FileIOUtils;

public class AgentAlignmentScoreDistribution {

	public static Histogram getAgentScoreDistribution(LinkedPrefAlignQuestions[] allLinkedPrefAlignQuestions,
			int numBins) throws IOException, ParseException {
		Histogram histogram = new Histogram(numBins, 0.0, 1.0);
		for (LinkedPrefAlignQuestions linkedQuestions : allLinkedPrefAlignQuestions) {
			for (int i = 0; i < linkedQuestions.getNumQuestions(); i++) {
				File questionDir = linkedQuestions.getQuestionDir(i);
				int agentIndex = linkedQuestions.getQuestionAgentIndex(i);
				double agentScore = PrefAlignQuestionUtils.getAgentAlignmentScore(questionDir, agentIndex);
				histogram.addData(agentScore);
			}
		}
		return histogram;
	}

	public static Histogram getAllAgentScoreDistribution(File rootDir, int numBins) throws IOException, ParseException {
		Histogram histogram = new Histogram(numBins, 0.0, 1.0);
		File[] questionDirs = QuestionUtils.listQuestionDirs(rootDir);
		FilenameFilter agentPolicyFilenameFilter = (dir, name) -> name.matches("agentPolicy[0-9]+.json");

		for (File questionDir : questionDirs) {
			int numAgents = questionDir.listFiles(agentPolicyFilenameFilter).length;
			for (int i = 0; i < numAgents; i++) {
				double agentScore = PrefAlignQuestionUtils.getAgentAlignmentScore(questionDir, i);
				histogram.addData(agentScore);
			}
		}
		return histogram;
	}

	public static void main(String[] args)
			throws URISyntaxException, ClassNotFoundException, IOException, ParseException {
		int numBins = Integer.parseInt(args[0]);

		// Only get agent-score distribution from non-validation questions
		// Read serialized LinkedPrefAlignQuestions objects that do not contain validation questions
		File serLinkedQuestionsDir = FileIOUtils.getResourceDir(PrefAlignQuestionLinker.class,
				"serialized-linked-questions");
		LinkedPrefAlignQuestions[] allLinkedPrefAlignQuestions = PrefAlignQuestionLinker
				.readAllLinkedPrefAlignQuestions(serLinkedQuestionsDir);

		Histogram distribution = getAgentScoreDistribution(allLinkedPrefAlignQuestions, numBins);
		System.out.println("Agent alignment score distribution from serialized-linked-questions:");
		System.out.println(distribution);

		File questionsRootDir = FileIOUtils.getQuestionsResourceDir(PrefAlignQuestionLinker.class);
		Histogram fullDistribution = getAllAgentScoreDistribution(questionsRootDir, numBins);
		System.out.println("Agent alignment score distribution from all questions:");
		System.out.println(fullDistribution);
	}
}
