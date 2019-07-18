package mobilerobot.study.prefalign;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.parser.ParseException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import mobilerobot.study.mturk.MTurkHTMLQuestionUtils;
import mobilerobot.study.utilities.ExplanationHTMLGenerator;
import mobilerobot.study.utilities.HTMLGeneratorUtils;
import mobilerobot.study.utilities.HTMLTableSettings;
import mobilerobot.study.utilities.QuestionUtils;
import mobilerobot.utilities.FileIOUtils;

public class PrefAlignQuestionHTMLLinker {

	private static final String[] DATA_TYPES = new String[] { "answer", "justification", "confidence" };
	private static final Map<String, String[]> DATA_TYPE_OPTIONS = new HashMap<>();
	static {
		DATA_TYPE_OPTIONS.put("answer", new String[] { "yes", "no" });
		DATA_TYPE_OPTIONS.put("confidence", new String[] { "high", "medium", "low" });
	}

	private static final double ALIGN_PROB = 0.5;
	private static final double UNALIGN_THRESHOLD = 0.95;

	private PrefAlignQuestionHTMLGenerator mQuestionHTMLGenerator;

	public PrefAlignQuestionHTMLLinker(PrefAlignQuestionHTMLGenerator questionHTMLGenerator) {
		mQuestionHTMLGenerator = questionHTMLGenerator;
	}

	public File[][] createAllLinkedQuestionFiles(File[][] allLinkedQuestionDirs, int[][] allLinkedQuestionAgentIndices,
			boolean withExplanation, File rootOutDir) throws IOException, ParseException, URISyntaxException {
		File[][] allLinkedQuestionFiles = new File[allLinkedQuestionDirs.length][allLinkedQuestionDirs[0].length];

		for (int i = 0; i < allLinkedQuestionDirs.length; i++) {
			File[] linkedQuestionDirs = allLinkedQuestionDirs[i];
			int[] linkedQuestionAgentIndices = allLinkedQuestionAgentIndices[i];

			// Linked html questions (of the same cost function) will be placed in the same directory
			File subOutDir = new File(rootOutDir, "linked-questions-set" + i);
			File[] linkedQuestionFiles = createLinkedQuestionFiles(linkedQuestionDirs, linkedQuestionAgentIndices,
					withExplanation, subOutDir);

			allLinkedQuestionFiles[i] = linkedQuestionFiles;
		}

		return allLinkedQuestionFiles;
	}

	private File[] createLinkedQuestionFiles(File[] linkedQuestionDirs, int[] linkedQuestionAgentIndices,
			boolean withExplanation, File outDir) throws IOException, ParseException, URISyntaxException {
		int numQuestions = linkedQuestionDirs.length;
		File[] linkedQuestionFiles = new File[numQuestions];

		for (int j = 0; j < numQuestions; j++) {
			File questionDir = linkedQuestionDirs[j];
			int agentIndex = linkedQuestionAgentIndices[j];

			// questionDir can be null if, for a particular cost structure, there are fewer associated questions than numQuestions
			if (questionDir == null) {
				// No more questionDir
				break;
			}

			String questionDocName = QuestionUtils.getPrefAlignQuestionDocumentName(questionDir, agentIndex,
					withExplanation);

			Document questionDoc = mQuestionHTMLGenerator.createPrefAlignQuestionDocument(questionDir, agentIndex,
					withExplanation, outDir);

			// MTurk Crowd HTML
			Element crowdScript = MTurkHTMLQuestionUtils.getCrowdHTMLScript();
			Element mTurkCrowdFormDiv;
			Element crowdFormActionScript;

			boolean hasNextQuestionDir = j < numQuestions - 1 && linkedQuestionDirs[j + 1] != null;

			if (hasNextQuestionDir) {
				// Intermediate crowd-form
				File nextQuestionDir = linkedQuestionDirs[j + 1];
				int nextAgentIndex = linkedQuestionAgentIndices[j + 1];
				String nextQuestionDocName = QuestionUtils.getPrefAlignQuestionDocumentName(nextQuestionDir,
						nextAgentIndex, withExplanation);

				String nextUrl = nextQuestionDocName + ".html";
				mTurkCrowdFormDiv = MTurkHTMLQuestionUtils.createIntermediateCrowdFormContainer(questionDocName,
						nextUrl);

				crowdFormActionScript = MTurkHTMLQuestionUtils.getIntermediateCrowdFormNextOnClickScript(j, DATA_TYPES,
						DATA_TYPE_OPTIONS);
			} else {
				// Last, submittable crowd-form
				mTurkCrowdFormDiv = MTurkHTMLQuestionUtils.createSubmittableCrowdFormContainer(questionDocName,
						numQuestions, DATA_TYPES);

				crowdFormActionScript = MTurkHTMLQuestionUtils.getSubmittableCrowdFormOnSubmitScript(j, numQuestions,
						DATA_TYPES, DATA_TYPE_OPTIONS);
			}

			questionDoc.body().appendChild(crowdScript);
			questionDoc.body().appendChild(mTurkCrowdFormDiv);
			questionDoc.body().appendChild(crowdFormActionScript);

			// Write question HTML document to file
			File questionFile = HTMLGeneratorUtils.writeHTMLDocumentToFile(questionDoc, questionDocName, outDir);

			linkedQuestionFiles[j] = questionFile;
		}

		return linkedQuestionFiles;
	}

	public static void main(String[] args) throws URISyntaxException, IOException, ParseException {
		String rootOutDirname = args[0];
		int numQuestions = Integer.parseInt(args[1]);

		File questionsRootDir = FileIOUtils.getQuestionsResourceDir(PrefAlignQuestionHTMLLinker.class);
		File rootOutDir = new File(rootOutDirname);
		File rootOutDirExplanation = new File(rootOutDirname + "-explanation");

		linkAllPrefAlignHTMLQuestions(questionsRootDir, numQuestions, ALIGN_PROB, UNALIGN_THRESHOLD, rootOutDir,
				rootOutDirExplanation);
	}

	public static void linkAllPrefAlignHTMLQuestions(File questionsRootDir, int numQuestions, double alignProb,
			double unalignThreshold, File rootOutDir, File rootOutDirExplanation)
			throws IOException, ParseException, URISyntaxException {
		PrefAlignQuestionLinker questionLinker = new PrefAlignQuestionLinker(questionsRootDir, alignProb,
				unalignThreshold);
		questionLinker.groupQuestionDirsByCostStruct();

		File[][] allLinkedQuestionDirs = questionLinker.getAllLinkedQuestionDirs(numQuestions);
		int[][] allLinkedQuestionAgentIndices = questionLinker.getAllLinkedQuestionAgentIndices(allLinkedQuestionDirs);

		HTMLTableSettings tableSettings = ExplanationHTMLGenerator.getMobileRobotHTMLTableSettings();
		PrefAlignQuestionHTMLGenerator questionHTMLGenerator = new PrefAlignQuestionHTMLGenerator(tableSettings);

		PrefAlignQuestionHTMLLinker questionHTMLLinker = new PrefAlignQuestionHTMLLinker(questionHTMLGenerator);

		// Both explanation and no-explanation groups will have the exact same questions in the exact same order
		questionHTMLLinker.createAllLinkedQuestionFiles(allLinkedQuestionDirs, allLinkedQuestionAgentIndices, false,
				rootOutDir);
		questionHTMLLinker.createAllLinkedQuestionFiles(allLinkedQuestionDirs, allLinkedQuestionAgentIndices, true,
				rootOutDirExplanation);
	}
}
