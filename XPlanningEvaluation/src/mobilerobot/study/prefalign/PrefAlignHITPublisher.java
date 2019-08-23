package mobilerobot.study.prefalign;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.json.simple.parser.ParseException;

import mobilerobot.study.mturk.HITInfo;
import mobilerobot.study.mturk.HITPublisher;
import mobilerobot.study.mturk.MTurkAPIUtils;
import mobilerobot.study.utilities.QuestionUtils;
import mobilerobot.utilities.FileIOUtils;
import software.amazon.awssdk.services.mturk.MTurkClient;
import software.amazon.awssdk.services.mturk.model.ReviewPolicy;

public class PrefAlignHITPublisher {

	/**
	 * https://<bucket-name>.s3.<AWS-region>.amazonaws.com
	 */
	private static final String S3_AWS_URL_FORMAT = "https://%s.s3.%s.amazonaws.com";
	private static final String XPLANNING_S3_BUCKET_NAME = "xplanning-bucket";
	private static final String XPLANNING_S3_REGION = "us-east-2";

	/**
	 * First page of the PrefAlign study, with a parameter pointing to the first PrefAlign question in a HIT
	 */
	private static final String FIRST_QUESTION_REL_URL_FORMAT = "/resources/mobilerobot/study/prefalign/instruction.html?headQuestion=%s";

	private final HITPublisher mHITPublisher;
	private final File mHITInfoCSVFile;

	public PrefAlignHITPublisher(MTurkClient client) throws IOException {
		mHITPublisher = new HITPublisher(client);
		mHITInfoCSVFile = createHITInfoCSVFile();
	}

	public void publishAllHITs(boolean controlGroup, Set<String> validationQuestionDocNames)
			throws URISyntaxException, IOException, ClassNotFoundException, ParseException {
		boolean withExplanation = !controlGroup;

		// Read serialized LinkedPrefAlignQuestions objects that contain validation questions
		LinkedPrefAlignQuestions[] allLinkedPrefAlignQuestions = readAllLinkedPrefAlignQuestionsWithValidation(
				withExplanation);

		for (LinkedPrefAlignQuestions linkedPrefAlignQuestions : allLinkedPrefAlignQuestions) {
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
		try (BufferedWriter writer = Files.newBufferedWriter(mHITInfoCSVFile.toPath(), StandardOpenOption.APPEND)) {
			writer.write(hitInfo.getHITId());
			writer.write(",");
			writer.write(hitInfo.getHITTypeId());
			for (String questionDocName : linkedQuestionDocNames) {
				if (questionDocName != null) {
					writer.write(",");
					writer.write(questionDocName);
				}
			}
			writer.write("\n");
		}
	}

	public static File[] createAllExternalQuestionXMLFiles(boolean withExplanation) throws ClassNotFoundException,
			URISyntaxException, IOException, ParserConfigurationException, TransformerException {
		// Read serialized LinkedPrefAlignQuestions objects that contain validation questions
		LinkedPrefAlignQuestions[] allLinkedPrefAlignQuestions = readAllLinkedPrefAlignQuestionsWithValidation(
				withExplanation);
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

	private static LinkedPrefAlignQuestions[] readAllLinkedPrefAlignQuestionsWithValidation(boolean withExplanation)
			throws URISyntaxException, ClassNotFoundException, IOException {
		// Read serialized LinkedPrefAlignQuestions objects that contain validation questions
		String servLinkedQuestionsDirname = withExplanation ? "serialized-vlinked-questions-explanation"
				: "serialized-vlinked-questions";
		File servLinkedQuestionsDir = FileIOUtils.getResourceDir(PrefAlignHITPublisher.class,
				servLinkedQuestionsDirname);
		return PrefAlignQuestionLinker.readAllLinkedPrefAlignQuestions(servLinkedQuestionsDir);
	}

	private static Path getQuestionHTMLFileRelativePath(String questionDocName, boolean withExplanation)
			throws URISyntaxException {
		String questionDocHTMLFilename = questionDocName + ".html";
		// Use linked PrefAlign questions that contain validation questions
		File linkedQuestionsRootDir = withExplanation
				? FileIOUtils.getResourceDir(PrefAlignHITPublisher.class, "vlinked-questions-explanation")
				: FileIOUtils.getResourceDir(PrefAlignHITPublisher.class, "vlinked-questions");
		File questionDocHTMLFile = FileIOUtils.searchFileRecursively(linkedQuestionsRootDir, questionDocHTMLFilename);

		// Directory where instruction.html is located
		File baseDir = new File(PrefAlignHITPublisher.class.getResource(".").toURI());

		// To make both paths have the same root
		Path baseAbsPath = baseDir.toPath().toAbsolutePath();
		Path questionAbsPath = questionDocHTMLFile.toPath().toAbsolutePath();
		// Relative path between dir of instruction.html and question-mission[i]-agent[j](-explanation).html
		return baseAbsPath.relativize(questionAbsPath);
	}

	/**
	 * Create ExternalURL for ExternalQuestion.
	 * 
	 * @param headQuestionDocName
	 * @return https://<bucket-name>.s3.<AWS-region>.amazonaws.com/resources/mobilerobot/study/prefalign/instruction.html?headQuestion=<rel-path>
	 */
	private static String createExternalURL(String headQuestionDocName) {
		String baseURL = String.format(S3_AWS_URL_FORMAT, XPLANNING_S3_BUCKET_NAME, XPLANNING_S3_REGION);
		String headQuestionRelURL = String.format(FIRST_QUESTION_REL_URL_FORMAT, headQuestionDocName);
		return baseURL + headQuestionRelURL;
	}

	public static void main(String[] args) throws ClassNotFoundException, URISyntaxException, IOException,
			ParserConfigurationException, TransformerException, ParseException {
		String option = args[0];

		if (option.equals("createExternalQuestions")) {
			createAllExternalQuestionXMLFiles(false);
			createAllExternalQuestionXMLFiles(true);
		} else if (option.equals("publishHITs")) {
			boolean withExplanation = args.length > 1 && args[1].equals("-e");
			Set<String> validationQuestionDocNames = QuestionUtils.getValidationQuestionDocNames();
			PrefAlignHITPublisher publisher = new PrefAlignHITPublisher(MTurkAPIUtils.getSandboxClient());
			publisher.publishAllHITs(!withExplanation, validationQuestionDocNames);
		} else if (option.equals("deleteHITs")) {
			String hitTypeId = args[1];
			MTurkAPIUtils.deleteHITs(MTurkAPIUtils.getSandboxClient(), hitTypeId);
		} else if (option.equals("approveAssignmentsOfReviewableHITs")) {
			String hitTypeId = args[1];
			MTurkAPIUtils.approveAssignmentsOfReviewableHITs(MTurkAPIUtils.getSandboxClient(), hitTypeId);
		}
	}

}
