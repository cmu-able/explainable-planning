package mobilerobot.study.prefalign;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Set;

import org.json.simple.parser.ParseException;

import mobilerobot.study.mturk.HITInfo;
import mobilerobot.study.mturk.HITPublisher;
import mobilerobot.study.mturk.MTurkAPIUtils;
import mobilerobot.utilities.FileIOUtils;
import software.amazon.awssdk.services.mturk.MTurkClient;
import software.amazon.awssdk.services.mturk.model.ReviewPolicy;

public class PrefAlignHITPublisher {

	private final HITPublisher mHITPublisher;
	private final File mHITInfoCSVFile;

	public PrefAlignHITPublisher(MTurkClient client) throws IOException {
		mHITPublisher = new HITPublisher(client);
		mHITInfoCSVFile = createHITInfoCSVFile();
	}

	public void publishAllHITs(boolean controlGroup, Set<String> validationQuestionDocNames)
			throws URISyntaxException, IOException, ClassNotFoundException, ParseException {
		File serializedLinkedQuestionsDir = FileIOUtils.getResourceDir(getClass(), "serialized-linked-questions");

		for (File file : serializedLinkedQuestionsDir.listFiles()) {
			try (FileInputStream fileIn = new FileInputStream(file)) {
				try (ObjectInputStream objectIn = new ObjectInputStream(fileIn)) {
					LinkedPrefAlignQuestions linkedPrefAlignQuestions = (LinkedPrefAlignQuestions) objectIn
							.readObject();

					boolean withExplanation = !controlGroup;
					// All PrefAlign question document names in this HIT will be written to hitInfo.csv
					String[] linkedQuestionDocNames = linkedPrefAlignQuestions
							.getLinkedQuestionDocumentNames(withExplanation);
					String headQuestionDocName = linkedQuestionDocNames[0];
					File questionXMLFile = createQuestionXMLFile(headQuestionDocName);

					ReviewPolicy assignmentReviewPolicy = MTurkAPIUtils
							.getAssignmentReviewPolicy(linkedPrefAlignQuestions, validationQuestionDocNames);
					HITInfo hitInfo = mHITPublisher.publishHIT(questionXMLFile, controlGroup, assignmentReviewPolicy);

					writeHITInfoToCSVFile(hitInfo, linkedQuestionDocNames);
				}
			}
		}
	}

	private File createQuestionXMLFile(String headQuestionDocName) {
		// TODO
		return null;
	}

	private File createHITInfoCSVFile() throws IOException {
		File hitInfoCSVFile = FileIOUtils.createOutputFile("hitInfo.csv");
		try (BufferedWriter writer = Files.newBufferedWriter(hitInfoCSVFile.toPath())) {
			writer.write("HIT ID, HITType ID, Document Names\n");
		}
		return hitInfoCSVFile;
	}

	private void writeHITInfoToCSVFile(HITInfo hitInfo, String[] linkedQuestionDocNames) throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(mHITInfoCSVFile.toPath())) {
			writer.write(hitInfo.getHITId());
			writer.write(", ");
			writer.write(hitInfo.getHITTypeId());
			for (String questionDocName : linkedQuestionDocNames) {
				writer.write(", ");
				writer.write(questionDocName);
			}
			writer.write("\n");
		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
