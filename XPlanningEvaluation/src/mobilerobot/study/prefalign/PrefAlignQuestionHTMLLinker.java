package mobilerobot.study.prefalign;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.parser.ParseException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import mobilerobot.study.utilities.HTMLGeneratorUtils;
import mobilerobot.study.utilities.MTurkHTMLQuestionUtils;
import mobilerobot.study.utilities.QuestionUtils;

public class PrefAlignQuestionHTMLLinker {

	private static final String[] DATA_TYPES = new String[] { "answer", "justification", "confidence" };
	private static final Map<String, String[]> DATA_TYPE_OPTIONS = new HashMap<>();
	static {
		DATA_TYPE_OPTIONS.put("answer", new String[] { "yes", "no" });
		DATA_TYPE_OPTIONS.put("confidence", new String[] { "high", "medium", "low" });
	}

	private PrefAlignQuestionLinker mQuestionLinker;
	private PrefAlignQuestionHTMLGenerator mQuestionHTMLGenerator;

	public PrefAlignQuestionHTMLLinker(File questionsRootDir, double alignProb, double unalignThreshold,
			PrefAlignQuestionHTMLGenerator questionHTMLGenerator) throws IOException, ParseException {
		mQuestionLinker = new PrefAlignQuestionLinker(questionsRootDir, alignProb, unalignThreshold);
		mQuestionLinker.groupQuestionDirsByCostStruct();
		mQuestionHTMLGenerator = questionHTMLGenerator;
	}

	public void createLinkedPrefAlignQuestions(int numQuestions, boolean withExplanation, File outDir)
			throws IOException, ParseException, URISyntaxException {
		File[][] allLinkedQuestionDirs = mQuestionLinker.getAllLinkedQuestionDirs(numQuestions);
		int[][] allLinkedQuestionAgentIndices = mQuestionLinker.getAllLinkedQuestionAgentIndices(allLinkedQuestionDirs);

		for (int i = 0; i < allLinkedQuestionDirs.length; i++) {
			File[] linkedQuestionDirs = allLinkedQuestionDirs[i];
			int[] linkedQuestionAgentIndices = allLinkedQuestionAgentIndices[i];

			Document[] questionDocs = createLinkedPrefAlignQuestionsDocuments(linkedQuestionDirs,
					linkedQuestionAgentIndices, withExplanation, outDir);
		}
	}

	private Document[] createLinkedPrefAlignQuestionsDocuments(File[] linkedQuestionDirs,
			int[] linkedQuestionAgentIndices, boolean withExplanation, File outDir)
			throws IOException, ParseException, URISyntaxException {
		int numQuestions = linkedQuestionDirs.length;
		Document[] questionDocs = new Document[numQuestions];

		for (int j = 0; j < numQuestions; j++) {
			File questionDir = linkedQuestionDirs[j];
			int agentIndex = linkedQuestionAgentIndices[j];

			Document questionDoc = mQuestionHTMLGenerator.createPrefAlignQuestionDocument(questionDir, agentIndex,
					withExplanation, outDir);

			// MTurk Crowd HTML
			Element crowdScript = MTurkHTMLQuestionUtils.getCrowdHTMLScript();
			Element mTurkCrowdFormDiv;
			Element crowdFormActionScript;

			if (j < numQuestions - 1) {
				// Intermediate crowd-form
				File nextQuestionDir = linkedQuestionDirs[j + 1];
				int nextAgentIndex = linkedQuestionAgentIndices[j + 1];
				String nextQuestionDocName = QuestionUtils.getPrefAlignQuestionDocumentName(nextQuestionDir,
						nextAgentIndex, withExplanation);

				String nextUrl = nextQuestionDocName + ".html";
				mTurkCrowdFormDiv = MTurkHTMLQuestionUtils.createIntermediateCrowdFormContainer(nextUrl);

				crowdFormActionScript = MTurkHTMLQuestionUtils.getIntermediateCrowdFormNextOnClickScript(j, DATA_TYPES,
						DATA_TYPE_OPTIONS);
			} else {
				// Last, submittable crowd-form
				mTurkCrowdFormDiv = MTurkHTMLQuestionUtils.createSubmittableCrowdFormContainer(numQuestions,
						DATA_TYPES);

				crowdFormActionScript = MTurkHTMLQuestionUtils.getSubmittableCrowdFormOnSubmitScript(j, numQuestions,
						DATA_TYPES, DATA_TYPE_OPTIONS);
			}

			questionDoc.body().appendChild(crowdScript);
			questionDoc.body().appendChild(mTurkCrowdFormDiv);
			questionDoc.body().appendChild(crowdFormActionScript);

			// Write question HTML document to file
			String questionDocName = QuestionUtils.getPrefAlignQuestionDocumentName(questionDir, agentIndex,
					withExplanation);
			HTMLGeneratorUtils.writeHTMLDocumentToFile(questionDoc, questionDocName, outDir);

			questionDocs[j] = questionDoc;
		}

		return questionDocs;
	}
}
