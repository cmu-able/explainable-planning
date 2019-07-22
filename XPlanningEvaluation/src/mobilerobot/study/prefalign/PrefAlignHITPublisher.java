package mobilerobot.study.prefalign;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.URISyntaxException;
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
		mHITInfoCSVFile = FileIOUtils.createOutputFile("hitInfo.csv");
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
					String questionDocName = linkedPrefAlignQuestions.getQuestionDocumentName(0, withExplanation);
					File questionXMLFile = createQuestionXMLFile(questionDocName);

					ReviewPolicy assignmentReviewPolicy = MTurkAPIUtils
							.getAssignmentReviewPolicy(linkedPrefAlignQuestions, validationQuestionDocNames);
					mHITPublisher.publishHIT(questionXMLFile, controlGroup, assignmentReviewPolicy);
				}
			}
		}
	}

	private File createQuestionXMLFile(String questionDocName) {
		// TODO
		return null;
	}

	private void writeHITInfoToCSVFile(HITInfo hitInfo) {
		// TODO
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
