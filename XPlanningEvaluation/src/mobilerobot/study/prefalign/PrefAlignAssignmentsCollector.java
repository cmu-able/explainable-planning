package mobilerobot.study.prefalign;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import mobilerobot.study.mturk.AssignmentsCollector;
import mobilerobot.study.mturk.HITInfo;
import mobilerobot.study.mturk.HITProgress;
import mobilerobot.utilities.FileIOUtils;
import software.amazon.awssdk.services.mturk.MTurkClient;
import software.amazon.awssdk.services.mturk.model.Assignment;

public class PrefAlignAssignmentsCollector {

	private final AssignmentsCollector mAssignmentsCollector;

	public PrefAlignAssignmentsCollector(MTurkClient client, File hitInfoCSVFile) throws IOException {
		mAssignmentsCollector = new AssignmentsCollector(client, hitInfoCSVFile);
	}

	public File writeAssignmentsToCSVFile(String[] dataTypes, int numQuestions)
			throws ParserConfigurationException, SAXException, IOException {
		File assignmentsCSVFile = createAssignmentsCSVFile(dataTypes, numQuestions);
		List<HITProgress> hitsProgress = mAssignmentsCollector.collectAllHITsProgress();

		for (HITProgress hitProgress : hitsProgress) {
			HITInfo hitInfo = hitProgress.getHITInfo();
			List<Assignment> assignments = hitProgress.getCurrentAssignments();
			writeAssignmentsToCSVFile(hitInfo, assignments, assignmentsCSVFile);
		}

		return assignmentsCSVFile;
	}

	private File createAssignmentsCSVFile(String[] dataTypes, int numQuestions) throws IOException {
		File hitInfoCSVFile = FileIOUtils.createOutputFile("assignments.csv");
		try (BufferedWriter writer = Files.newBufferedWriter(hitInfoCSVFile.toPath())) {
			writer.write("HIT ID,HITType ID,Assignment ID,Worker ID,Total Time (seconds)");

			for (int i = 0; i < numQuestions; i++) {
				for (String dataType : dataTypes) {
					String columnName = String.format("question%d-%s", i, dataType);
					writer.write(",");
					writer.write(columnName);
				}
			}
			writer.write("\n");
		}
		return hitInfoCSVFile;
	}

	private void writeAssignmentsToCSVFile(HITInfo hitInfo, List<Assignment> assignments, File assignmentsCSVFile)
			throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(assignmentsCSVFile.toPath(), StandardOpenOption.APPEND)) {
			writer.write(hitInfo.getHITId());
			writer.write(",");
			writer.write(hitInfo.getHITTypeId());

			for (Assignment assignment : assignments) {
				Instant acceptTime = assignment.acceptTime();
				Instant submitTime = assignment.submitTime();
				Duration totalDuration = Duration.between(acceptTime, submitTime);
				long totalDurationSeconds = totalDuration.getSeconds();

				writer.write(",");
				writer.write(assignment.assignmentId());
				writer.write(",");
				writer.write(assignment.workerId());
				writer.write(",");
				writer.write(Long.toString(totalDurationSeconds));
			}

			writer.write("\n");
		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
