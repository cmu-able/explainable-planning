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

	public File[][] createAllLinkedQuestionFiles(LinkedPrefAlignQuestions[] allLinkedPrefAlignQuestions,
			boolean withExplanation, File rootOutDir) throws IOException, ParseException, URISyntaxException {
		int numQuestions = allLinkedPrefAlignQuestions[0].getNumQuestions();
		File[][] allLinkedQuestionFiles = new File[allLinkedPrefAlignQuestions.length][numQuestions];

		for (int i = 0; i < allLinkedPrefAlignQuestions.length; i++) {
			LinkedPrefAlignQuestions linkedPrefAlignQuestions = allLinkedPrefAlignQuestions[i];

			// Linked html questions (of the same cost function) will be placed in the same directory
			File subOutDir = new File(rootOutDir, "linked-questions-set" + i);
			File[] linkedQuestionFiles = createLinkedQuestionFiles(linkedPrefAlignQuestions, withExplanation,
					subOutDir);

			allLinkedQuestionFiles[i] = linkedQuestionFiles;
		}

		return allLinkedQuestionFiles;
	}

	private File[] createLinkedQuestionFiles(LinkedPrefAlignQuestions linkedPrefAlignQuestions, boolean withExplanation,
			File outDir) throws IOException, ParseException, URISyntaxException {
		int numQuestions = linkedPrefAlignQuestions.getNumQuestions();
		File[] linkedQuestionFiles = new File[numQuestions];

		for (int j = 0; j < numQuestions; j++) {
			File questionDir = linkedPrefAlignQuestions.getQuestionDir(j);
			int agentIndex = linkedPrefAlignQuestions.getQuestionAgentIndex(j);

			// questionDir can be null if, for a particular cost structure, there are fewer associated questions than numQuestions
			if (questionDir == null) {
				// No more questionDir
				break;
			}

			String questionDocName = linkedPrefAlignQuestions.getQuestionDocumentName(j, withExplanation);
			Document questionDoc = mQuestionHTMLGenerator.createPrefAlignQuestionDocument(questionDir, agentIndex,
					withExplanation, outDir);

			// MTurk Crowd HTML
			Element crowdScript = MTurkHTMLQuestionUtils.getCrowdHTMLScript();
			Element mTurkCrowdFormDiv;
			Element crowdFormActionScript;

			if (linkedPrefAlignQuestions.hasNextQuestion(j)) {
				// Intermediate crowd-form
				String nextQuestionDocName = linkedPrefAlignQuestions.getQuestionDocumentName(j + 1, withExplanation);

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

		LinkedPrefAlignQuestions[] allLinkedPrefAlignQuestions = questionLinker
				.getAllLinkedPrefAlignQuestions(numQuestions);

		HTMLTableSettings tableSettings = ExplanationHTMLGenerator.getMobileRobotHTMLTableSettings();
		PrefAlignQuestionHTMLGenerator questionHTMLGenerator = new PrefAlignQuestionHTMLGenerator(tableSettings);

		PrefAlignQuestionHTMLLinker questionHTMLLinker = new PrefAlignQuestionHTMLLinker(questionHTMLGenerator);

		// Both explanation and no-explanation groups will have the exact same questions in the exact same order
		questionHTMLLinker.createAllLinkedQuestionFiles(allLinkedPrefAlignQuestions, false, rootOutDir);
		questionHTMLLinker.createAllLinkedQuestionFiles(allLinkedPrefAlignQuestions, true, rootOutDirExplanation);
	}
}
