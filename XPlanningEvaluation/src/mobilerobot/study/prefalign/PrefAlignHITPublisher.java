package mobilerobot.study.prefalign;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.json.simple.parser.ParseException;

import mobilerobot.study.mturk.HITInfo;
import mobilerobot.study.mturk.HITPublisher;
import mobilerobot.study.mturk.MTurkAPIUtils;
import mobilerobot.utilities.FileIOUtils;
import software.amazon.awssdk.services.mturk.MTurkClient;
import software.amazon.awssdk.services.mturk.model.ReviewPolicy;

public class PrefAlignHITPublisher {

	/**
	 * http://<bucket-name>.s3-website.<AWS-region>.amazonaws.com
	 */
	private static final String S3_AWS_URL_FORMAT = "http://%s.s3-website.%s.amazonaws.com";
	private static final String XPLANNING_S3_BUCKET_NAME = "xplanning-bucket";
	private static final String XPLANNING_S3_REGION = "us-east-2";

	/**
	 * First page of the PrefAlign study, with a parameter pointing to the first PrefAlign question in a HIT
	 */
	private static final String FIRST_QUESTION_REL_URL_FORMAT = "/study/prefalign/instruction.html?headQuestion=%s";

	private final HITPublisher mHITPublisher;
	private final File mHITInfoCSVFile;

	public PrefAlignHITPublisher(MTurkClient client) throws IOException {
		mHITPublisher = new HITPublisher(client);
		mHITInfoCSVFile = createHITInfoCSVFile();
	}

	public void publishAllHITs(boolean controlGroup, Set<String> validationQuestionDocNames)
			throws URISyntaxException, IOException, ClassNotFoundException, ParseException {
		LinkedPrefAlignQuestions[] allLinkedPrefAlignQuestions = readAllLinkedPrefAlignQuestions();

		for (LinkedPrefAlignQuestions linkedPrefAlignQuestions : allLinkedPrefAlignQuestions) {
			boolean withExplanation = !controlGroup;

			// All PrefAlign question document names in this HIT will be written to hitInfo.csv
			String[] linkedQuestionDocNames = linkedPrefAlignQuestions.getLinkedQuestionDocumentNames(withExplanation);

			// ExternalQuestion xml filename is the same as 1st PrefAlign question document name
			String headQuestionDocName = linkedQuestionDocNames[0];
			File questionXMLFile = HITPublisher.getExternalQuestionXMLFile(headQuestionDocName + ".xml");

			// Assignment review policy for auto-reject
			ReviewPolicy assignmentReviewPolicy = MTurkAPIUtils.getAssignmentReviewPolicy(linkedPrefAlignQuestions,
					validationQuestionDocNames);

			HITInfo hitInfo = mHITPublisher.publishHIT(questionXMLFile, controlGroup, assignmentReviewPolicy);

			writeHITInfoToCSVFile(hitInfo, linkedQuestionDocNames);
		}
	}

	private File createHITInfoCSVFile() throws IOException {
		File hitInfoCSVFile = FileIOUtils.createOutputFile("hitInfo.csv");
		try (BufferedWriter writer = Files.newBufferedWriter(hitInfoCSVFile.toPath())) {
			writer.write("HIT ID,HITType ID,Document Names\n");
		}
		return hitInfoCSVFile;
	}

	private void writeHITInfoToCSVFile(HITInfo hitInfo, String[] linkedQuestionDocNames) throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(mHITInfoCSVFile.toPath())) {
			writer.write(hitInfo.getHITId());
			writer.write(",");
			writer.write(hitInfo.getHITTypeId());
			for (String questionDocName : linkedQuestionDocNames) {
				writer.write(",");
				writer.write(questionDocName);
			}
			writer.write("\n");
		}
	}

	public static File[] createAllExternalQuestionXMLFiles(boolean withExplanation) throws ClassNotFoundException,
			URISyntaxException, IOException, ParserConfigurationException, TransformerException {
		LinkedPrefAlignQuestions[] allLinkedPrefAlignQuestions = readAllLinkedPrefAlignQuestions();
		File[] allQuestionXMLFiles = new File[allLinkedPrefAlignQuestions.length];

		for (int i = 0; i < allLinkedPrefAlignQuestions.length; i++) {
			LinkedPrefAlignQuestions linkedPrefAlignQuestions = allLinkedPrefAlignQuestions[i];

			// Document name of the 1st PrefAlign question in this HIT
			String headQuestionDocName = linkedPrefAlignQuestions.getQuestionDocumentName(0, withExplanation);

			// Relative path between instruction.html and the 1st PrefAlign question HTML file
			Path headQuestionFileRelPath = getQuestionHTMLFileRelativePath(headQuestionDocName, withExplanation);

			// ExternalURL contains a parameter that points to the 1st PrefAlign question HTML document
			String externalURL = createExternalURL(headQuestionFileRelPath.toString());

			File questionXMLFile = HITPublisher.createExternalQuestionXMLFile(externalURL,
					headQuestionDocName + ".xml");
			allQuestionXMLFiles[i] = questionXMLFile;
		}

		return allQuestionXMLFiles;
	}

	private static LinkedPrefAlignQuestions[] readAllLinkedPrefAlignQuestions()
			throws URISyntaxException, IOException, ClassNotFoundException {
		File serLinkedQuestionsDir = FileIOUtils.getResourceDir(PrefAlignHITPublisher.class,
				"serialized-linked-questions");
		File[] serLinkedQuestionsFiles = serLinkedQuestionsDir.listFiles();

		LinkedPrefAlignQuestions[] allLinkedPrefAlignQuestions = new LinkedPrefAlignQuestions[serLinkedQuestionsFiles.length];

		for (int i = 0; i < serLinkedQuestionsFiles.length; i++) {
			File serLinkedQuestionsFile = serLinkedQuestionsFiles[i];

			try (FileInputStream fileIn = new FileInputStream(serLinkedQuestionsFile)) {
				try (ObjectInputStream objectIn = new ObjectInputStream(fileIn)) {
					LinkedPrefAlignQuestions linkedPrefAlignQuestions = (LinkedPrefAlignQuestions) objectIn
							.readObject();

					allLinkedPrefAlignQuestions[i] = linkedPrefAlignQuestions;
				}
			}
		}
		return allLinkedPrefAlignQuestions;
	}

	private static Path getQuestionHTMLFileRelativePath(String questionDocName, boolean withExplanation)
			throws URISyntaxException {
		String questionDocHTMLFilename = questionDocName + ".html";
		File linkedQuestionsDir = withExplanation
				? FileIOUtils.getResourceDir(PrefAlignHITPublisher.class, "linked-questions-explanation")
				: FileIOUtils.getResourceDir(PrefAlignHITPublisher.class, "linked-questions");
		File questionDocHTMLFile = FileIOUtils.searchFileRecursively(linkedQuestionsDir, questionDocHTMLFilename);
		File instructionHTMLFile = FileIOUtils.getFile(PrefAlignHITPublisher.class, "instruction.html");

		// To make both paths have the same root
		Path baseAbsPath = instructionHTMLFile.toPath().toAbsolutePath();
		Path questionAbsPath = questionDocHTMLFile.toPath().toAbsolutePath();
		// Relative path between instruction.html and question-mission[i]-agent[j].html (or question-mission[i]-agent[j]-explanation.html)
		return baseAbsPath.relativize(questionAbsPath);
	}

	/**
	 * Create ExternalURL for ExternalQuestion.
	 * 
	 * @param headQuestionDocName
	 * @return http://<bucket-name>.s3-website.<AWS-region>.amazonaws.com/study/prefalign/instruction.html?headQuestion=<rel-path>
	 */
	private static String createExternalURL(String headQuestionDocName) {
		String baseURL = String.format(S3_AWS_URL_FORMAT, XPLANNING_S3_BUCKET_NAME, XPLANNING_S3_REGION);
		String headQuestionRelURL = String.format(FIRST_QUESTION_REL_URL_FORMAT, headQuestionDocName);
		return baseURL + headQuestionRelURL;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}