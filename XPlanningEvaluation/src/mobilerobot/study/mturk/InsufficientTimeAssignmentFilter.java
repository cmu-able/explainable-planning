package mobilerobot.study.mturk;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import mobilerobot.utilities.FileIOUtils;
import software.amazon.awssdk.services.mturk.model.Assignment;

public class InsufficientTimeAssignmentFilter implements IAssignmentFilter {

	private static final long MIN_ELAPSED_TIME_MS = 30 * 1000L; // minimum of 30 seconds/question
	private static final String REJECT_FEEDBACK = "Sorry, we could not approve your submission as you took too little time answering at least one of the questions.";

	// Prefix pattern of each QuestionIdentifier
	private static final Pattern QUESTION_ID_PREFIX_PATTERN = Pattern.compile("(question[0-9]+-).+");

	private final Set<String> mValidationQuestionDocNames;

	public InsufficientTimeAssignmentFilter(Set<String> validationQuestionDocNames) {
		mValidationQuestionDocNames = validationQuestionDocNames;
	}

	@Override
	public boolean accept(Assignment assignment) throws ParserConfigurationException, SAXException, IOException {
		String answerXMLStr = assignment.answer();
		Document answerXML = FileIOUtils.parseXMLString(answerXMLStr);
		NodeList answerNodeList = answerXML.getElementsByTagName("Answer");

		// Get prefix(es) of validation question(s): question[i]-
		Set<String> validationQuestionPrefixes = getValidationQuestionPrefixes(answerNodeList);

		for (int i = 0; i < answerNodeList.getLength(); i++) {
			Node answerNode = answerNodeList.item(i);

			if (answerNode.getNodeType() == Node.ELEMENT_NODE) {
				Element answerElement = (Element) answerNode;
				String questionID = answerElement.getElementsByTagName("QuestionIdentifier").item(0).getTextContent();
				String questionIDPrefix = getQuestionIDPrefix(questionID);

				// Only consider elapsed time of non-validation questions
				if (questionID.matches("question[0-9]+-elapsedTime")
						&& !validationQuestionPrefixes.contains(questionIDPrefix)) {
					String elapsedTimeStr = answerElement.getElementsByTagName("FreeText").item(0).getTextContent();
					long elapsedTimeinMS = Long.parseLong(elapsedTimeStr);

					if (elapsedTimeinMS < MIN_ELAPSED_TIME_MS) {
						return false;
					}
				}
			}
		}
		return true;
	}

	private Set<String> getValidationQuestionPrefixes(NodeList answerNodeList) {
		Set<String> validationQuestionPrefixes = new HashSet<>();

		for (int i = 0; i < answerNodeList.getLength(); i++) {
			Node answerNode = answerNodeList.item(i);

			if (answerNode.getNodeType() == Node.ELEMENT_NODE) {
				Element answerElement = (Element) answerNode;
				String questionID = answerElement.getElementsByTagName("QuestionIdentifier").item(0).getTextContent();

				if (questionID.matches("question[0-9]+-ref")) {
					// question[i]-ref maps to the document name of that question, e.g., question-mission[n]-agent[m]-explanation
					String questionRef = answerElement.getElementsByTagName("FreeText").item(0).getTextContent();

					// If this question is a validation question, get its prefix: question[i]-
					if (mValidationQuestionDocNames.contains(questionRef)) {
						String questionIDPrefix = getQuestionIDPrefix(questionID);
						validationQuestionPrefixes.add(questionIDPrefix);
					}
				}
			}
		}
		return validationQuestionPrefixes;
	}

	private String getQuestionIDPrefix(String questionID) {
		Matcher m = QUESTION_ID_PREFIX_PATTERN.matcher(questionID);
		if (m.find()) {
			return m.group(1);
		}
		throw new IllegalArgumentException(
				"The QuestionIdentifier \"" + questionID + "\" does not have the correct prefix.");
	}

	@Override
	public String getRejectFeedback() {
		return REJECT_FEEDBACK;
	}

}
