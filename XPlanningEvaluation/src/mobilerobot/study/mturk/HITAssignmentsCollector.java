package mobilerobot.study.mturk;

import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import mobilerobot.utilities.FileIOUtils;
import software.amazon.awssdk.services.mturk.MTurkClient;
import software.amazon.awssdk.services.mturk.model.Assignment;
import software.amazon.awssdk.services.mturk.model.AssignmentStatus;
import software.amazon.awssdk.services.mturk.model.GetHitRequest;
import software.amazon.awssdk.services.mturk.model.GetHitResponse;
import software.amazon.awssdk.services.mturk.model.HITStatus;
import software.amazon.awssdk.services.mturk.model.ListAssignmentsForHitRequest;
import software.amazon.awssdk.services.mturk.model.ListAssignmentsForHitResponse;
import software.amazon.awssdk.services.mturk.model.RejectAssignmentRequest;

public class HITAssignmentsCollector {

	private static final long MIN_ELAPSED_TIME_MS = 30 * 1000L; // minimum of 30 seconds/question
	private static final String REJECT_FEEDBACK = "Sorry, we could not approve your submission as you took too little time answering at least one of the questions.";

	private final MTurkClient mClient;

	public HITAssignmentsCollector(MTurkClient client) {
		mClient = client;
	}

	public HITStatus collectHITStatus(HITInfo hitInfo) {
		GetHitRequest getHITRequest = GetHitRequest.builder().hitId(hitInfo.getHITId()).build();
		GetHitResponse getHITResponse = mClient.getHIT(getHITRequest);
		return getHITResponse.hit().hitStatus();
	}

	public List<Assignment> collectPendingReviewHITAssignments(HITInfo hitInfo)
			throws ParserConfigurationException, SAXException, IOException {
		// Collect the assignments that have been submitted for this HIT
		// These assignments have not failed the Assignment Review Policy
		List<Assignment> submittedAssignments = collectHITAssignments(hitInfo, AssignmentStatus.SUBMITTED);
		autoRejectSubmittedHITAssignments(submittedAssignments);

		// Collect the remaining assignments that have not been auto-rejected
		// These assignments will be reviewed manually to be approved or rejected
		return collectHITAssignments(hitInfo, AssignmentStatus.SUBMITTED);
	}

	private List<Assignment> collectHITAssignments(HITInfo hitInfo, AssignmentStatus status) {
		// Get the maximum # of completed assignments for this HIT
		ListAssignmentsForHitRequest listHITRequest = ListAssignmentsForHitRequest.builder().hitId(hitInfo.getHITId())
				.assignmentStatuses(status).maxResults(HITPublisher.MAX_ASSIGNMENTS).build();

		ListAssignmentsForHitResponse listHITResponse = mClient.listAssignmentsForHIT(listHITRequest);
		return listHITResponse.assignments();
	}

	private void autoRejectSubmittedHITAssignments(List<Assignment> assignments)
			throws ParserConfigurationException, SAXException, IOException {
		for (Assignment assignment : assignments) {
			boolean autoReject = autoRejectAssignmentInsufficientTime(assignment);

			// Reject any assignment that took too little time
			if (autoReject) {
				RejectAssignmentRequest rejectRequest = RejectAssignmentRequest.builder()
						.assignmentId(assignment.assignmentId()).requesterFeedback(REJECT_FEEDBACK).build();

				mClient.rejectAssignment(rejectRequest);
			}
		}
	}

	private boolean autoRejectAssignmentInsufficientTime(Assignment assignment)
			throws ParserConfigurationException, SAXException, IOException {
		String answerXMLStr = assignment.answer();
		Document answerXML = FileIOUtils.parseXMLString(answerXMLStr);
		NodeList answerNodeList = answerXML.getElementsByTagName("Answer");

		for (int i = 0; i < answerNodeList.getLength(); i++) {
			Node answerNode = answerNodeList.item(i);

			if (answerNode.getNodeType() == Node.ELEMENT_NODE) {
				Element answerElement = (Element) answerNode;
				String questionID = answerElement.getElementsByTagName("QuestionIdentifier").item(0).getTextContent();

				if (questionID.matches("question[0-9]+-elapsedTime")) {
					String elapsedTimeStr = answerElement.getElementsByTagName("FreeText").item(0).getTextContent();
					long elapsedTimeinMS = Long.parseLong(elapsedTimeStr);

					if (elapsedTimeinMS < MIN_ELAPSED_TIME_MS) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public List<Assignment> collectApprovedHITAssignments(HITInfo hitInfo) {
		// Collect the assignments that have been manually approved for this HIT
		return collectHITAssignments(hitInfo, AssignmentStatus.APPROVED);
	}
}
