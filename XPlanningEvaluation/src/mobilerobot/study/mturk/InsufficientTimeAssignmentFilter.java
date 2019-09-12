package mobilerobot.study.mturk;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.xml.sax.SAXException;

import software.amazon.awssdk.services.mturk.model.Assignment;

public class InsufficientTimeAssignmentFilter implements IAssignmentFilter {

	private static final long MIN_ELAPSED_TIME_MS = 30 * 1000L; // minimum of 30 seconds/question
	private static final String REJECT_FEEDBACK = "Sorry, we could not approve your submission as you took too little time answering at least one of the questions.";

	// Prefix pattern of each question key
	private static final Pattern QUESTION_KEY_PREFIX_PATTERN = Pattern.compile("(question[0-9]+-).+");

	private final Set<String> mValidationQuestionDocNames;

	public InsufficientTimeAssignmentFilter(Set<String> validationQuestionDocNames) {
		mValidationQuestionDocNames = validationQuestionDocNames;
	}

	@Override
	public boolean accept(Assignment assignment)
			throws ParserConfigurationException, SAXException, IOException, ParseException {
		JSONObject answerJsonObj = AssignmentsCollector.getAssignmentAnswerJSONObjectFromFreeText(assignment);

		// Get prefix(es) of validation question(s): question[i]-
		Set<String> validationQuestionPrefixes = getValidationQuestionPrefixes(assignment);

		for (String validationQuestionPrefix : validationQuestionPrefixes) {
			// question[i]-elapsedTime
			String elapsedTimeKey = validationQuestionPrefix + "elapsedTime";

			String elapsedTimeStr = (String) answerJsonObj.get(elapsedTimeKey);
			long elapsedTimeinMS = Long.parseLong(elapsedTimeStr);

			if (elapsedTimeinMS < MIN_ELAPSED_TIME_MS) {
				return false;
			}
		}

		return true;
	}

	@Override
	public String getRejectFeedback() {
		return REJECT_FEEDBACK;
	}

	private Set<String> getValidationQuestionPrefixes(Assignment assignment)
			throws ParserConfigurationException, SAXException, IOException, ParseException {
		JSONObject answerJsonObj = AssignmentsCollector.getAssignmentAnswerJSONObjectFromFreeText(assignment);
		Set<String> validationQuestionPrefixes = new HashSet<>();

		for (Object key : answerJsonObj.keySet()) {
			String questionKey = (String) key;

			if (questionKey.matches("question[0-9]+-ref")) {
				// question[i]-ref maps to the document name of that question, e.g., question-mission[n]-agent[m]-explanation
				String questionRef = (String) answerJsonObj.get(questionKey);

				// If this question is a validation question, get its prefix: question[i]-
				if (mValidationQuestionDocNames.contains(questionRef)) {
					String questionIDPrefix = getQuestionIDPrefix(questionKey);
					validationQuestionPrefixes.add(questionIDPrefix);
				}
			}
		}

		return validationQuestionPrefixes;
	}

	private String getQuestionIDPrefix(String questionID) {
		Matcher m = QUESTION_KEY_PREFIX_PATTERN.matcher(questionID);
		if (m.find()) {
			return m.group(1);
		}
		throw new IllegalArgumentException(
				"The QuestionIdentifier \"" + questionID + "\" does not have the correct prefix.");
	}

}
