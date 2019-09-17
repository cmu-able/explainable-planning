package mobilerobot.study.prefalign.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;

import org.json.simple.parser.ParseException;

import mobilerobot.study.prefalign.LinkedPrefAlignQuestions;
import mobilerobot.study.prefalign.PrefAlignQuestionLinker;
import mobilerobot.utilities.FileIOUtils;

public class AnswerKeyGenerator {

	private LinkedPrefAlignQuestions[] mAllLinkedQuestions;
	private File mAnswerKeyFile;

	private DecimalFormat mCostDecimalFormat = new DecimalFormat("#.#");

	public AnswerKeyGenerator(String serLinkedQuestionsDirname, String[] dataTypes)
			throws URISyntaxException, ClassNotFoundException, IOException {
		File serLinkedQuestionsDir = FileIOUtils.getResourceDir(PrefAlignQuestionLinker.class,
				serLinkedQuestionsDirname);
		mAllLinkedQuestions = PrefAlignQuestionLinker.readAllLinkedPrefAlignQuestions(serLinkedQuestionsDir);
		mAnswerKeyFile = createAnswerKeyCSVFile(dataTypes);

		mCostDecimalFormat.setRoundingMode(RoundingMode.HALF_UP);
	}

	public File generateAnswerKeyCSVFile(boolean withExplanation) throws IOException, ParseException {
		try (BufferedWriter writer = Files.newBufferedWriter(mAnswerKeyFile.toPath(), StandardOpenOption.APPEND)) {

			for (LinkedPrefAlignQuestions linkedQuestions : mAllLinkedQuestions) {
				int numQuestons = linkedQuestions.getNumQuestions();
				for (int i = 0; i < numQuestons; i++) {
					// "ref" column:
					// question-mission[x]-agent[y] or question-mission[x]-agent[y]-explanation
					String questionDocName = linkedQuestions.getQuestionDocumentName(i, withExplanation);
					writer.write(questionDocName);

					File questionDir = linkedQuestions.getQuestionDir(i);
					int agentIndex = linkedQuestions.getQuestionAgentIndex(i);

					// "total-cost" column:
					double agentPolicyCost = PrefAlignQuestionUtils.getAgentPolicyCost(questionDir, agentIndex);
					String totalCost = mCostDecimalFormat.format(agentPolicyCost);
					writer.write(",");
					writer.write(totalCost);

					// "answer" column:
					String answer = PrefAlignQuestionUtils.getAgentAlignmentAnswer(questionDir, agentIndex);
					writer.write(",");
					writer.write(answer);

					writer.write("\n");
				}
			}
		}

		return mAnswerKeyFile;
	}

	private File createAnswerKeyCSVFile(String[] dataTypes) throws IOException {
		File answerKeyCSVFile = FileIOUtils.createOutputFile("answerKey.csv");
		try (BufferedWriter writer = Files.newBufferedWriter(answerKeyCSVFile.toPath())) {
			// Header: ref,total-cost,answer,...
			String header = String.join(",", dataTypes);
			writer.write(header);
			writer.write("\n");
		}
		return answerKeyCSVFile;
	}
}
