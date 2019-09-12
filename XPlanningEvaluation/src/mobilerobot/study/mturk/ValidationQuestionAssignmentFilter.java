package mobilerobot.study.mturk;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.json.simple.parser.ParseException;
import org.xml.sax.SAXException;

import mobilerobot.study.prefalign.LinkedPrefAlignQuestions;
import mobilerobot.study.prefalign.analysis.PrefAlignQuestionUtils;
import software.amazon.awssdk.services.mturk.model.Assignment;

public class ValidationQuestionAssignmentFilter implements IAssignmentFilter {

	private static final String REJECT_FEEDBACK = "Sorry, we could not approve your submission as you did not correctly answer one or more validation question(s).";

	private final LinkedPrefAlignQuestions mLinkedQuestions;
	private final boolean mWithExplanation;
	private final Set<String> mValidationQuestionDocNames;

	public ValidationQuestionAssignmentFilter(LinkedPrefAlignQuestions linkedQuestions, boolean withExplanation,
			Set<String> validationQuestionDocNames) {
		mLinkedQuestions = linkedQuestions;
		mWithExplanation = withExplanation;
		mValidationQuestionDocNames = validationQuestionDocNames;
	}

	@Override
	public boolean accept(Assignment assignment)
			throws ParserConfigurationException, SAXException, IOException, ParseException {
		for (int i = 0; i < mLinkedQuestions.getNumQuestions(); i++) {
			String questionDocName = mLinkedQuestions.getQuestionDocumentName(i, mWithExplanation);

			if (mValidationQuestionDocNames.contains(questionDocName)) {
				File questionDir = mLinkedQuestions.getQuestionDir(i);
				int agentIndex = mLinkedQuestions.getQuestionAgentIndex(i);

				// Correct answer: "yes" or "no" answer
				String correctAnswer = PrefAlignQuestionUtils.getAgentAlignmentAnswer(questionDir, agentIndex);

				// Answer from assignment: "question[i]-answer"
				String questionKey = String.format(MTurkHTMLQuestionUtils.QUESTION_KEY_FORMAT, i, "answer");
				String answer = AssignmentsCollector.getAssignmentAnswerFromFreeText(assignment, questionKey);

				if (!answer.equals(correctAnswer)) {
					return false;
				}
			}
		}

		return true;
	}

	@Override
	public String getRejectFeedback() {
		return REJECT_FEEDBACK;
	}

}
