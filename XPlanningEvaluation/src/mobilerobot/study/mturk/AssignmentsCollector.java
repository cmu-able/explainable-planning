package mobilerobot.study.mturk;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import mobilerobot.utilities.FileIOUtils;
import software.amazon.awssdk.services.mturk.MTurkClient;
import software.amazon.awssdk.services.mturk.model.ApproveAssignmentRequest;
import software.amazon.awssdk.services.mturk.model.Assignment;
import software.amazon.awssdk.services.mturk.model.AssignmentStatus;
import software.amazon.awssdk.services.mturk.model.HIT;
import software.amazon.awssdk.services.mturk.model.ListAssignmentsForHitRequest;
import software.amazon.awssdk.services.mturk.model.ListAssignmentsForHitResponse;
import software.amazon.awssdk.services.mturk.model.RejectAssignmentRequest;

public class AssignmentsCollector {

	private static final String APPROVE_FEEDBACK = "Thank you for your participation.";

	private final MTurkClient mClient;
	private final List<HITInfo> mHITInfos = new ArrayList<>();

	public AssignmentsCollector(MTurkClient client, File hitInfoCSVFile) throws IOException {
		mClient = client;
		readAllHITInfos(hitInfoCSVFile);
	}

	public AssignmentsCollector(MTurkClient client, String hitTypeId) {
		mClient = client;
		List<HIT> hits = MTurkAPIUtils.getHITs(client, hitTypeId);
		for (HIT hit : hits) {
			HITInfo hitInfo = new HITInfo(hit.hitId(), hitTypeId);

			mHITInfos.add(hitInfo);
		}
	}

	private void readAllHITInfos(File hitInfoCSVFile) throws IOException {
		try (BufferedReader reader = new BufferedReader(new FileReader(hitInfoCSVFile))) {
			String line = reader.readLine(); // Skip header line

			while ((line = reader.readLine()) != null) {
				String[] values = line.split(",");
				String hitId = values[0];
				String hitTypeId = values[1];
				HITInfo hitInfo = new HITInfo(hitId, hitTypeId);

				mHITInfos.add(hitInfo);
			}
		}
	}

	public List<HITProgress> collectAllHITsProgress(IAssignmentFilter... assignmentFilters)
			throws ParserConfigurationException, SAXException, IOException {
		List<HITProgress> hitsProgress = new ArrayList<>();
		for (HITInfo hitInfo : mHITInfos) {
			// Collect all submitted assignments for this HIT
			List<Assignment> submittedAssignments = collectHITAssignments(hitInfo, AssignmentStatus.SUBMITTED);
			List<Assignment> passingAssignments = new ArrayList<>();

			for (Assignment submittedAssignment : submittedAssignments) {
				boolean accept = true;

				// Check each submitted assignment against all filters
				for (IAssignmentFilter filter : assignmentFilters) {
					accept &= filter.accept(submittedAssignment);

					if (!accept) {
						// If the assignment fails any filter, auto-reject it
						String rejectFeedback = filter.getRejectFeedback();
						RejectAssignmentRequest rejectRequest = RejectAssignmentRequest.builder()
								.assignmentId(submittedAssignment.assignmentId()).requesterFeedback(rejectFeedback)
								.build();
						mClient.rejectAssignment(rejectRequest);
						break;
					}
				}

				// Each assignment that passes all filters will be added to the HITProgress
				passingAssignments.add(submittedAssignment);
			}

			HITProgress hitProgress = new HITProgress(hitInfo);
			hitProgress.addAssignments(passingAssignments);

			hitsProgress.add(hitProgress);
		}
		return hitsProgress;
	}

	List<Assignment> collectHITAssignments(HITInfo hitInfo, AssignmentStatus status) {
		// Get the maximum # of completed assignments for this HIT
		ListAssignmentsForHitRequest listHITRequest = ListAssignmentsForHitRequest.builder().hitId(hitInfo.getHITId())
				.assignmentStatuses(status).maxResults(HITPublisher.MAX_ASSIGNMENTS).build();

		ListAssignmentsForHitResponse listHITResponse = mClient.listAssignmentsForHIT(listHITRequest);
		return listHITResponse.assignments();
	}

	public void approveAllAssignments(List<HITProgress> hitsProgress) {
		for (HITProgress hitProgress : hitsProgress) {
			List<Assignment> assignments = hitProgress.getCurrentAssignments();
			approveAssignments(assignments);
		}
	}

	void approveAssignments(List<Assignment> assignments) {
		for (Assignment assignment : assignments) {
			ApproveAssignmentRequest approveRequest = ApproveAssignmentRequest.builder()
					.assignmentId(assignment.assignmentId()).requesterFeedback(APPROVE_FEEDBACK).build();

			mClient.approveAssignment(approveRequest);
		}
	}

	public static String getAssignmentAnswerFromFreeText(Assignment assignment, String questionKey)
			throws ParserConfigurationException, SAXException, IOException, ParseException {
		String answerXMLStr = assignment.answer();
		Document answerXML = FileIOUtils.parseXMLString(answerXMLStr);
		NodeList answerNodeList = answerXML.getElementsByTagName("Answer");

		for (int i = 0; i < answerNodeList.getLength(); i++) {
			Node answerNode = answerNodeList.item(i);

			if (answerNode.getNodeType() == Node.ELEMENT_NODE) {
				Element answerElement = (Element) answerNode;
				String mturkQuestionID = answerElement.getElementsByTagName("QuestionIdentifier").item(0)
						.getTextContent();

				// Answer data are under "taskAnswers"
				if (mturkQuestionID.equals("taskAnswers")) {
					// String inside <FreeText> is in JSON format of an array with 1 element
					String answerDataStr = answerElement.getElementsByTagName("FreeText").item(0).getTextContent();
					JSONParser jsonParser = new JSONParser();
					JSONArray answerDataJsonArr = (JSONArray) jsonParser.parse(answerDataStr);
					JSONObject answerDataJsonObj = (JSONObject) answerDataJsonArr.get(0);
					return (String) answerDataJsonObj.get(questionKey);
				}
			}
		}

		return null;
	}
}
