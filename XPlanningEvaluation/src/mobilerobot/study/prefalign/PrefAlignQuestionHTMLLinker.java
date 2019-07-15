package mobilerobot.study.prefalign;

import java.io.File;
import java.io.IOException;

import org.json.simple.parser.ParseException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import mobilerobot.study.utilities.MTurkHTMLQuestionUtils;

public class PrefAlignQuestionHTMLLinker {

	private static final String[] DATA_TYPES = new String[] { "answer", "justification", "confidence" };

	private PrefAlignQuestionLinker mQuestionLinker;

	public PrefAlignQuestionHTMLLinker(File questionsRootDir, double alignProb, double unalignThreshold)
			throws IOException, ParseException {
		mQuestionLinker = new PrefAlignQuestionLinker(questionsRootDir, alignProb, unalignThreshold);
		mQuestionLinker.groupQuestionDirsByCostStruct();
	}

	public void createLinkedPrefAlignQuestions(int numQuestions, boolean withExplanation)
			throws IOException, ParseException {
		File[][] allLinkedQuestionFiles = mQuestionLinker.getAllLinkedPrefAlignQuestionFiles(numQuestions,
				withExplanation);

		for (int i = 0; i < allLinkedQuestionFiles.length; i++) {
			File[] linkedQuestionHTMLFiles = allLinkedQuestionFiles[i];
		}
	}

	private Document createLinkedPrefAlignQuestionsDocument(File[] linkedQuestionHTMLFiles) throws IOException {
		int numQuestions = linkedQuestionHTMLFiles.length;
		for (int j = 0; j < numQuestions; j++) {
			File questionHTMLFile = linkedQuestionHTMLFiles[j];
			Document doc = Jsoup.parse(questionHTMLFile, "UTF-8", "../questions/");

			// MTurk Crowd HTML
			Element crowdScript = MTurkHTMLQuestionUtils.getCrowdHTMLScript();
			Element mTurkCrowdFormDiv;

			if (j < numQuestions - 1) {
				// Intermediate crowd-form
				String nextUrl = ""; // TODO
				mTurkCrowdFormDiv = MTurkHTMLQuestionUtils.createIntermediateCrowdFormContainer(nextUrl);
			} else {
				// Last, submittable crowd-form
				mTurkCrowdFormDiv = MTurkHTMLQuestionUtils.createSubmittableCrowdFormContainer(numQuestions,
						DATA_TYPES);
			}

			doc.body().appendChild(crowdScript);
			doc.body().appendChild(mTurkCrowdFormDiv);
		}

		return null;
	}
}
