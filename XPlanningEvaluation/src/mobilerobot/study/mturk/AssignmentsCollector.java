package mobilerobot.study.mturk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.ArrayUtils;
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
import software.amazon.awssdk.services.mturk.model.CreateAdditionalAssignmentsForHitRequest;
import software.amazon.awssdk.services.mturk.model.DeleteHitRequest;
import software.amazon.awssdk.services.mturk.model.ListAssignmentsForHitRequest;
import software.amazon.awssdk.services.mturk.model.ListAssignmentsForHitResponse;
import software.amazon.awssdk.services.mturk.model.RejectAssignmentRequest;
import software.amazon.awssdk.services.mturk.model.UpdateExpirationForHitRequest;

public class AssignmentsCollector {

	private static final String APPROVE_FEEDBACK = "Thank you for your participation.";

	private final MTurkClient mClient;

	public AssignmentsCollector(MTurkClient client) {
		mClient = client;
	}

	/**
	 * Collect all submitted assignments of a HIT so far. Grant "Participation Stamp" qualification to every Worker who
	 * submitted work.
	 * 
	 * This method should be invoked after the maximum number of submitted assignments for the HIT is reached.
	 * 
	 * @param hitIndex
	 * @param hitInfoCSVFile
	 * @param assignmentFilters
	 * @return HITProgress
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParseException
	 */
	public HITProgress collectHITProgress(int hitIndex, File hitInfoCSVFile, IAssignmentFilter... assignmentFilters)
			throws ParserConfigurationException, SAXException, IOException, ParseException {
		List<HITInfo> hitInfos = readAllHITInfos(hitInfoCSVFile);
		HITInfo hitInfo = hitInfos.get(hitIndex);

		// Collect all submitted assignments for this HIT
		List<Assignment> submittedAssignments = collectHITAssignments(hitInfo, AssignmentStatus.SUBMITTED);
		List<Assignment> passingAssignments = new ArrayList<>();

		for (Assignment submittedAssignment : submittedAssignments) {
			// Grant "Participation Stamp" to every Worker who submitted work, to prevent them from accepting new HIT
			String workerID = submittedAssignment.workerId();
			QualificationUtils.grantParticipationStampQualification(mClient, workerID);

			// Check each submitted assignment against all filters
			boolean accept = true;
			for (IAssignmentFilter filter : assignmentFilters) {
				accept &= filter.accept(submittedAssignment);

				if (!accept) {
					// If the assignment fails any filter, auto-reject it
					String rejectFeedback = filter.getRejectFeedback();
					RejectAssignmentRequest rejectRequest = RejectAssignmentRequest.builder()
							.assignmentId(submittedAssignment.assignmentId()).requesterFeedback(rejectFeedback).build();
					mClient.rejectAssignment(rejectRequest);

					// Extend the maximum number of assignments of this HIT by 1
					CreateAdditionalAssignmentsForHitRequest extendRequest = CreateAdditionalAssignmentsForHitRequest
							.builder().hitId(submittedAssignment.hitId()).numberOfAdditionalAssignments(1).build();
					mClient.createAdditionalAssignmentsForHIT(extendRequest);

					break;
				}
			}

			// Each assignment that passes all filters will be added to the HITProgress
			passingAssignments.add(submittedAssignment);
		}

		HITProgress hitProgress = new HITProgress(hitInfo);
		hitProgress.addAssignments(passingAssignments);

		return hitProgress;
	}

	private List<HITInfo> readAllHITInfos(File hitInfoCSVFile) throws IOException {
		List<HITInfo> hitInfos = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new FileReader(hitInfoCSVFile))) {
			String line = reader.readLine(); // Skip header line

			while ((line = reader.readLine()) != null) {
				String[] values = line.split(",");
				// values[0] is HIT Index
				String hitId = values[1];
				String hitTypeId = values[2];
				String[] questionDocNames = Arrays.copyOfRange(values, 3, values.length);

				HITInfo hitInfo = new HITInfo(hitId, hitTypeId);
				hitInfo.addQuestionDocumentNames(questionDocNames);

				hitInfos.add(hitInfo);
			}
		}
		return hitInfos;
	}

	public void writeAssignmentsToCSVFile(int hitIndex, HITProgress hitProgress, File currentAssignmentsCSVFile,
			String[] dataTypes, int numQuestions)
			throws IOException, ParserConfigurationException, SAXException, ParseException {
		HITInfo hitInfo = hitProgress.getHITInfo();
		List<Assignment> assignments = hitProgress.getCurrentAssignments();

		// Header:
		// HIT Index,HIT ID,HITType ID,Assignment ID,Worker ID,Total Time (seconds),question0-{dataType0}, ...
		File assignmentsCSVFile = currentAssignmentsCSVFile == null ? createAssignmentsCSVFile(dataTypes, numQuestions)
				: createAssignmentsCSVFile(currentAssignmentsCSVFile);

		try (BufferedWriter writer = Files.newBufferedWriter(assignmentsCSVFile.toPath(), StandardOpenOption.APPEND)) {
			for (Assignment assignment : assignments) {
				// HIT Index, HIT ID, and HITType ID in every row
				writer.write(Integer.toString(hitIndex));
				writer.write(",");
				writer.write(hitInfo.getHITId());
				writer.write(",");
				writer.write(hitInfo.getHITTypeId());
				writer.write(",");

				// Followed by: Assignment ID,Worker ID,Total Time (seconds),question0-{dataType0}, ... in each row
				List<String> assignmentData = getAssignmentData(assignment, dataTypes, numQuestions);
				String assignemntDataRow = String.join(",", assignmentData);
				writer.write(assignemntDataRow);
				writer.write("\n");
			}
		}
	}

	/**
	 * Approve all assignments in the given assignments.csv file that correspond to the given HIT index.
	 * 
	 * @param hitIndex
	 * @param assignmentsCSVFile
	 * @throws IOException
	 */
	public void approveAssignments(int hitIndex, File assignmentsCSVFile) throws IOException {
		try (BufferedReader reader = new BufferedReader(new FileReader(assignmentsCSVFile))) {
			String headerLine = reader.readLine(); // Header line
			String[] columnNames = headerLine.split(",");
			int assignmentIDIndex = ArrayUtils.indexOf(columnNames, "Assignment ID");

			String line;
			while ((line = reader.readLine()) != null) {
				String[] values = line.split(",");
				int hitIndexValue = Integer.parseInt(values[0]); // values[0] is HIT Index

				// Approve all assignments in assignments.csv file that correspond to the given HIT index
				if (hitIndexValue == hitIndex) {
					String assignmentIDValue = values[assignmentIDIndex];

					ApproveAssignmentRequest approveRequest = ApproveAssignmentRequest.builder()
							.assignmentId(assignmentIDValue).requesterFeedback(APPROVE_FEEDBACK).build();
					mClient.approveAssignment(approveRequest);
				}
			}
		}
	}

	/**
	 * Dispose HIT after approve or reject all of its submitted assignments.
	 * 
	 * @param hitProgress
	 */
	public void disposeHIT(HITProgress hitProgress) {
		// Check that all assignments of this HIT have already been approved or rejected
		List<Assignment> assignments = hitProgress.getCurrentAssignments();
		List<Assignment> pendingReviewAssignments = assignments.stream()
				.filter(assignment -> assignment.assignmentStatus() == AssignmentStatus.SUBMITTED)
				.collect(Collectors.toList());
		if (!pendingReviewAssignments.isEmpty()) {
			throw new IllegalStateException(
					"All assignments of HIT must be either approved or rejected before the HIT can be disposed.");
		}

		// Set HIT to expire now
		String hitID = hitProgress.getHITInfo().getHITId();
		UpdateExpirationForHitRequest updateHITRequest = UpdateExpirationForHitRequest.builder().hitId(hitID)
				.expireAt(Instant.now()).build();
		mClient.updateExpirationForHIT(updateHITRequest);

		// Delete HIT
		DeleteHitRequest deleteHITRequest = DeleteHitRequest.builder().hitId(hitID).build();
		mClient.deleteHIT(deleteHITRequest);
	}

	/**
	 * Create assignments.csv file with header: HIT Index,HIT ID,HITType ID,Assignment ID,Worker ID,Total Time
	 * (seconds),question0-{dataType0}, ...
	 * 
	 * @return assignments.csv file
	 * @throws IOException
	 */
	private File createAssignmentsCSVFile(String[] dataTypes, int numQuestions) throws IOException {
		File assignmentsCSVFile = FileIOUtils.createOutputFile("assignments.csv");
		try (BufferedWriter writer = Files.newBufferedWriter(assignmentsCSVFile.toPath())) {
			writer.write("HIT Index,HIT ID,HITType ID,Assignment ID,Worker ID,Total Time (seconds)");

			for (int i = 0; i < numQuestions; i++) {
				for (String dataType : dataTypes) {
					String columnName = String.format(MTurkHTMLQuestionUtils.QUESTION_KEY_FORMAT, i, dataType);
					writer.write(",");
					writer.write(columnName);
				}
			}
			writer.write("\n");
		}
		return assignmentsCSVFile;
	}

	private File createAssignmentsCSVFile(File currentAssignmentsCSVFile) throws IOException {
		// Copy the content from the current assignments.csv file to a new output assignments.csv file with the same
		// name
		Path outputAssignmentsCSVPath = Files.copy(currentAssignmentsCSVFile.toPath(),
				FileIOUtils.getOutputDir().toPath().resolve(currentAssignmentsCSVFile.getName()));
		return outputAssignmentsCSVPath.toFile();
	}

	private List<String> getAssignmentData(Assignment assignment, String[] dataTypes, int numQuestions)
			throws ParserConfigurationException, SAXException, IOException, ParseException {
		List<String> assignmentData = new ArrayList<>();

		// Assignment ID,Worker ID,Total Time (seconds),...
		assignmentData.add(assignment.assignmentId());
		assignmentData.add(assignment.workerId());
		Instant acceptTime = assignment.acceptTime();
		Instant submitTime = assignment.submitTime();
		Duration totalDuration = Duration.between(acceptTime, submitTime);
		long totalDurationSeconds = totalDuration.getSeconds();
		assignmentData.add(Long.toString(totalDurationSeconds));

		// Answer of each data type of each question
		for (int i = 0; i < numQuestions; i++) {
			for (String dataType : dataTypes) {
				String questionKey = String.format(MTurkHTMLQuestionUtils.QUESTION_KEY_FORMAT, i, dataType);
				String answer = AssignmentsCollector.getAssignmentAnswerFromFreeText(assignment, questionKey);
				assignmentData.add(answer);
			}
		}

		// Assignment ID,Worker ID,Total Time (seconds),question0-{dataType}, ...
		return assignmentData;
	}

	private List<Assignment> collectHITAssignments(HITInfo hitInfo, AssignmentStatus status) {
		// Get the completed assignments for this HIT so far
		ListAssignmentsForHitRequest listHITRequest = ListAssignmentsForHitRequest.builder().hitId(hitInfo.getHITId())
				.assignmentStatuses(status).build();

		ListAssignmentsForHitResponse listHITResponse = mClient.listAssignmentsForHIT(listHITRequest);
		return listHITResponse.assignments();
	}

	public static JSONObject getAssignmentAnswerJSONObjectFromFreeText(Assignment assignment)
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
					return (JSONObject) answerDataJsonArr.get(0);
				}
			}
		}

		throw new IllegalArgumentException("Invalid assignment's answer: " + answerXMLStr);
	}

	public static String getAssignmentAnswerFromFreeText(Assignment assignment, String questionKey)
			throws ParserConfigurationException, SAXException, IOException, ParseException {
		JSONObject answerDataJsonObj = getAssignmentAnswerJSONObjectFromFreeText(assignment);
		return (String) answerDataJsonObj.get(questionKey);
	}
}
