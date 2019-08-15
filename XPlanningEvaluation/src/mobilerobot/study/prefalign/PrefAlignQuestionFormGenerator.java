package mobilerobot.study.prefalign;

import java.util.Map;

import org.jsoup.nodes.Element;

import mobilerobot.study.mturk.MTurkHTMLQuestionUtils;

public class PrefAlignQuestionFormGenerator {

	private final int mNumQuestions;
	private final String[] mFillableDataTypes;
	private final Map<String, String[]> mFillableDataTypeOptions;
	private final Element[] mInputUIElements;

	public PrefAlignQuestionFormGenerator(int numQuestions, String[] fillableDataTypes,
			Map<String, String[]> fillableDataTypeOptions) {
		mNumQuestions = numQuestions;
		mFillableDataTypes = fillableDataTypes;
		mFillableDataTypeOptions = fillableDataTypeOptions;
		mInputUIElements = new Element[] { createPrefAlignCrowdQuestionContainer(),
				createJustificationCrowdQuestionContainer(), createConfidenceCrowdQuestionContainer() };
	}

	public Element[] createSubmittableFormElements(String questionDocName, int questionIndex) {
		// <crowd-form> will submit data to MTurk
		Element crowdFormContainer = MTurkHTMLQuestionUtils.createSubmittableCrowdFormContainer(questionDocName,
				mInputUIElements, mNumQuestions, mFillableDataTypes);

		// <crowd-form>'s "submit" action will:
		// - copy data from <crowd-form> UI to localStorage, 
		// - copy all questions' data from localStorage to <crowd-form> hidden inputs, and
		// - submit data to MTurk
		Element crowdFormOnSubmitScript = MTurkHTMLQuestionUtils.getSubmittableCrowdFormOnSubmitScript(questionIndex,
				mNumQuestions, mFillableDataTypes, mFillableDataTypeOptions);

		Element[] submitElements = new Element[2];
		submitElements[0] = crowdFormContainer;
		submitElements[1] = crowdFormOnSubmitScript;
		return submitElements;
	}

	public Element[] createIntermediateFormElements(String questionDocName, int questionIndex, String nextUrl) {
		// <crowd-form> only acts as input UI; its submit action is disabled
		Element crowdFormContainer = MTurkHTMLQuestionUtils.createIntermediateCrowdFormContainer(questionDocName,
				nextUrl, mInputUIElements);

		// <crowd-form>'s "next" action will copy data from <crowd-form> UI to localStorage
		Element crowdFormOnNextScript = MTurkHTMLQuestionUtils.getIntermediateCrowdFormNextOnClickScript(questionIndex,
				mFillableDataTypes, mFillableDataTypeOptions);

		Element[] intermediateFormElements = new Element[2];
		intermediateFormElements[0] = crowdFormContainer;
		intermediateFormElements[1] = crowdFormOnNextScript;
		return intermediateFormElements;
	}

	private static Element createPrefAlignCrowdQuestionContainer() {
		Element questionContainer = MTurkHTMLQuestionUtils.createCrowdQuestionContainer(
				"Is the agent's proposed policy the best one, with respect to the given cost profile?");
		String[] optionNames = { "answer-yes", "answer-no" };
		String[] optionLabels = { "Yes", "No" };
		Element answerOptions = MTurkHTMLQuestionUtils.createCrowdRadioGroup(optionNames, optionLabels);
		questionContainer.appendChild(answerOptions);
		return questionContainer;
	}

	private static Element createJustificationCrowdQuestionContainer() {
		Element questionContainer = MTurkHTMLQuestionUtils
				.createCrowdQuestionContainer("Please provide justification for your answer:");
		Element crowdTextArea = new Element("crowd-text-area");
		crowdTextArea.attr("id", "justification");
		crowdTextArea.attr("name", "justification");
		crowdTextArea.attr("label", "I gave the answer above because...");
		crowdTextArea.attr("rows", "5");
		crowdTextArea.attr("max-rows", "5");
		questionContainer.appendChild(crowdTextArea);
		return questionContainer;
	}

	private static Element createConfidenceCrowdQuestionContainer() {
		Element questionContainer = MTurkHTMLQuestionUtils
				.createCrowdQuestionContainer("How confident are you in your answer?");
		String[] optionNames = { "confidence-high", "confidence-medium", "confidence-low" };
		String[] optionLabels = { "Highly Confident", "Somewhat Confident", "Not Confident" };
		Element options = MTurkHTMLQuestionUtils.createCrowdRadioGroup(optionNames, optionLabels);
		questionContainer.appendChild(options);
		return questionContainer;
	}
}