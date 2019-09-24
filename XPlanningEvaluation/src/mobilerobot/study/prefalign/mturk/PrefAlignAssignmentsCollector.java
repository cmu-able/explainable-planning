package mobilerobot.study.prefalign.mturk;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.json.simple.parser.ParseException;
import org.xml.sax.SAXException;

import mobilerobot.study.mturk.AssignmentsCollector;
import mobilerobot.study.mturk.HITInfo;
import mobilerobot.study.mturk.HITProgress;
import mobilerobot.study.mturk.MTurkAPIUtils;
import mobilerobot.study.mturk.MTurkHTMLQuestionUtils;
import mobilerobot.utilities.FileIOUtils;
import software.amazon.awssdk.services.mturk.MTurkClient;
import software.amazon.awssdk.services.mturk.model.Assignment;

public class PrefAlignAssignmentsCollector {

	private final AssignmentsCollector mAssignmentsCollector;
	private final File mAssignmentsCSVFile;
	private final String[] mDataTypes;
	private final int mNumQuestions;

	public PrefAlignAssignmentsCollector(MTurkClient client, File hitInfoCSVFile, String[] dataTypes, int numQuestions)
			throws IOException {
		mAssignmentsCollector = new AssignmentsCollector(client, hitInfoCSVFile);
		mAssignmentsCSVFile = createAssignmentsCSVFile(dataTypes, numQuestions);
		mDataTypes = dataTypes;
		mNumQuestions = numQuestions;
	}

	public File writeAssignmentsToCSVFile()
			throws ParserConfigurationException, SAXException, IOException, ParseException {
		List<HITProgress> hitsProgress = mAssignmentsCollector.collectAllHITsProgress();

		for (HITProgress hitProgress : hitsProgress) {
			HITInfo hitInfo = hitProgress.getHITInfo();
			List<Assignment> assignments = hitProgress.getCurrentAssignments();
			writeAssignmentsToCSVFile(hitInfo, assignments);
		}

		return mAssignmentsCSVFile;
	}

	private File createAssignmentsCSVFile(String[] dataTypes, int numQuestions) throws IOException {
		File assignmentsCSVFile = FileIOUtils.createOutputFile("assignments.csv");
		try (BufferedWriter writer = Files.newBufferedWriter(assignmentsCSVFile.toPath())) {
			writer.write("HIT ID,HITType ID,Assignment ID,Worker ID,Total Time (seconds)");

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

	private void writeAssignmentsToCSVFile(HITInfo hitInfo, List<Assignment> assignments)
			throws IOException, ParserConfigurationException, SAXException, ParseException {
		try (BufferedWriter writer = Files.newBufferedWriter(mAssignmentsCSVFile.toPath(), StandardOpenOption.APPEND)) {

			for (Assignment assignment : assignments) {
				// HIT ID and HITType ID in every row
				writer.write(hitInfo.getHITId());
				writer.write(",");
				writer.write(hitInfo.getHITTypeId());
				writer.write(",");

				// Followed by: Assignment ID,Worker ID,Total Time (seconds),question0-{dataType}, ... in each row
				List<String> assignmentData = getAssignmentData(assignment);
				String assignemntDataRow = String.join(",", assignmentData);
				writer.write(assignemntDataRow);
				writer.write("\n");
			}
		}
	}

	private List<String> getAssignmentData(Assignment assignment)
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
		for (int i = 0; i < mNumQuestions; i++) {
			for (String dataType : mDataTypes) {
				String questionKey = String.format(MTurkHTMLQuestionUtils.QUESTION_KEY_FORMAT, i, dataType);
				String answer = AssignmentsCollector.getAssignmentAnswerFromFreeText(assignment, questionKey);
				assignmentData.add(answer);
			}
		}

		// Assignment ID,Worker ID,Total Time (seconds),question0-{dataType}, ...
		return assignmentData;
	}

	public static void main(String[] args)
			throws URISyntaxException, IOException, ParserConfigurationException, SAXException, ParseException {
		String hitInfoCSVFilename = args[0];
		File hitInfoCSVFile = FileIOUtils.getFile(AssignmentsCollector.class, "hit-info", hitInfoCSVFilename);

		String clientType = args[1];
		MTurkClient client;
		if (clientType.equals("-prod")) {
			client = MTurkAPIUtils.getProductionClient();
		} else if (clientType.equals("-sandbox")) {
			client = MTurkAPIUtils.getSandboxClient();
		} else {
			throw new IllegalArgumentException("Need MTurk client type argument");
		}

		String[] dataTypes = { "ref", "total-cost", "answer", "confidence", "elapsedTime" };
		int numQuestions = 4;
		PrefAlignAssignmentsCollector assignmentsCollector = new PrefAlignAssignmentsCollector(client, hitInfoCSVFile,
				dataTypes, numQuestions);
		assignmentsCollector.writeAssignmentsToCSVFile();
	}

}
