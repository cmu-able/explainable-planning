package mobilerobot.study.prefalign.mturk;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import mobilerobot.study.mturk.HITGroupInfo;
import mobilerobot.study.mturk.HITInfo;
import mobilerobot.study.mturk.HITPublisher;
import mobilerobot.study.mturk.MTurkAPIUtils;
import mobilerobot.study.prefalign.LinkedPrefAlignQuestions;
import mobilerobot.study.prefalign.PrefAlignQuestionLinker;
import mobilerobot.utilities.FileIOUtils;
import software.amazon.awssdk.services.mturk.MTurkClient;

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

	/**
	 * HIT Group information
	 */
	private static final String TITLE = "Understanding mobile robot navigation planning";
	private static final String DESCRIPTION_FORMAT = "This is a study to examine how people interpret and understand the objectives of an autonomous agent (e.g., a robot) by observing its behavior. (GROUP %d)";
	private static final String DESCRIPTION_CG = String.format(DESCRIPTION_FORMAT, 1);
	private static final String DESCRIPTION_EG = String.format(DESCRIPTION_FORMAT, 2);
	private static final String KEYWORDS = "Research study, experiment, human-robot interaction, human-AI interaction, mobile robot indoor navigation";
	private static final String REWARD = "3.50"; // assume 17.5 minutes/HIT; pay rate $12/hour
	private static final long ASSIGNMENT_DURATION = 40 * 60L; // 40 minutes
	private static final long LIFE_TIME = 1 * 7 * 24 * 60 * 60L; // 1 week
	static final int MAX_ASSIGNMENTS = 3; // maximum of 3 Worker(s) can complete each HIT

	private final HITPublisher mHITPublisher;

	public PrefAlignHITPublisher(MTurkClient client) {
		mHITPublisher = new HITPublisher(client);
	}

	public HITInfo publishHIT(boolean controlGroup, int hitIndex)
			throws ClassNotFoundException, URISyntaxException, IOException {
		boolean withExplanation = !controlGroup;

		// Read serialized LinkedPrefAlignQuestions objects that contain validation questions
		LinkedPrefAlignQuestions[] allLinkedPrefAlignQuestions = readAllLinkedPrefAlignQuestionsWithValidation(
				withExplanation);

		// Publish only 1 HIT at a time
		LinkedPrefAlignQuestions linkedPrefAlignQuestions = allLinkedPrefAlignQuestions[hitIndex];

		// All PrefAlign question document names in this HIT will be written to hitInfo.csv
		String[] linkedQuestionDocNames = linkedPrefAlignQuestions.getLinkedQuestionDocumentNames(withExplanation);

		// ExternalQuestion xml filename is the same as 1st PrefAlign question document name
		String headQuestionDocName = linkedQuestionDocNames[0];
		File questionXMLFile = HITPublisher.getExternalQuestionXMLFile(getClass(), headQuestionDocName + ".xml");

		// Cannot use Assignment Review Policy for auto-reject
		// because, due to the use of <crowd-form>, all answers are in a single <FreeText>

		HITInfo hitInfo = mHITPublisher.publishHIT(questionXMLFile, createHITGroupInfo(controlGroup), null);

		// Add document names of the linked questions in this HIT to HITInfo
		hitInfo.addQuestionDocumentNames(linkedQuestionDocNames);

		return hitInfo;
	}

	public void writeHITInfoToCSVFile(int hitIndex, HITInfo hitInfo, File currentHITInfoCSVFile) throws IOException {
		mHITPublisher.writeHITInfoToCSVFile(hitIndex, hitInfo, currentHITInfoCSVFile);
	}

	private HITGroupInfo createHITGroupInfo(boolean controlGroup) {
		HITGroupInfo hitGroupInfo = new HITGroupInfo();
		hitGroupInfo.setTitle(TITLE);
		hitGroupInfo.setDescription(controlGroup ? DESCRIPTION_CG : DESCRIPTION_EG);
		hitGroupInfo.setKeywords(KEYWORDS);
		hitGroupInfo.setReward(REWARD);
		hitGroupInfo.setAssignmentDuration(ASSIGNMENT_DURATION);
		hitGroupInfo.setLifetimeInSeconds(LIFE_TIME);
		hitGroupInfo.setMaxAssignments(MAX_ASSIGNMENTS);
		return hitGroupInfo;
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

	/**
	 * LinkedPrefAlignQuestions objects in the returned array will be sorted by the filenames of their corresponding
	 * .ser files.
	 * 
	 * @param withExplanation
	 * @return LinkedPrefAlignQuestions objects sorted by the filenames of their corresponding .ser files
	 * @throws URISyntaxException
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	private static LinkedPrefAlignQuestions[] readAllLinkedPrefAlignQuestionsWithValidation(boolean withExplanation)
			throws URISyntaxException, ClassNotFoundException, IOException {
		// Read serialized LinkedPrefAlignQuestions objects that contain validation questions
		String servLinkedQuestionsDirname = withExplanation ? "serialized-vlinked-questions-explanation"
				: "serialized-vlinked-questions";
		File servLinkedQuestionsDir = FileIOUtils.getResourceDir(PrefAlignQuestionLinker.class,
				servLinkedQuestionsDirname);
		return PrefAlignQuestionLinker.readAllLinkedPrefAlignQuestions(servLinkedQuestionsDir);
	}

	private static Path getQuestionHTMLFileRelativePath(String questionDocName, boolean withExplanation)
			throws URISyntaxException {
		String questionDocHTMLFilename = questionDocName + ".html";
		// Use linked PrefAlign questions that contain validation questions
		File linkedQuestionsRootDir = withExplanation
				? FileIOUtils.getResourceDir(PrefAlignQuestionLinker.class, "vlinked-questions-explanation")
				: FileIOUtils.getResourceDir(PrefAlignQuestionLinker.class, "vlinked-questions");
		File questionDocHTMLFile = FileIOUtils.searchFileRecursively(linkedQuestionsRootDir, questionDocHTMLFilename);

		// Directory where instruction.html is located
		File baseDir = new File(PrefAlignQuestionLinker.class.getResource(".").toURI());

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
			ParserConfigurationException, TransformerException {
		String option = args[0]; // args[0]: option
		String clientType = args[1]; // args[1]: -prod or -sandbox

		MTurkClient client;
		if (clientType.equals("-prod")) {
			client = MTurkAPIUtils.getProductionClient();
		} else if (clientType.equals("-sandbox")) {
			client = MTurkAPIUtils.getSandboxClient();
		} else {
			throw new IllegalArgumentException("Need MTurk client type argument");
		}

		if (option.equals("createExternalQuestions")) {
			createAllExternalQuestionXMLFiles(false);
			createAllExternalQuestionXMLFiles(true);
		} else if (option.equals("publishHIT")) {
			int hitIndex = Integer.parseInt(args[2]); // args[2]: HIT index
			boolean withExplanation = args.length > 3 && args[3].equals("-e"); // args[3]: explanation flag
			String hitInfoCSVFilename = null; // args[4] or args[3] if no -e flag: current hitInfo.csv filename if
												// exists
			if (args.length > 4) {
				hitInfoCSVFilename = args[4]; // args[4]: current hitInfo.csv filename
			} else if (!withExplanation && args.length > 3) {
				hitInfoCSVFilename = args[3]; // args[3] and no -e flag: current hitInfo.csv filename
			}
			File currentHITInfoCSVFile = hitInfoCSVFilename != null
					? FileIOUtils.getFile(PrefAlignHITPublisher.class, "hit-info", hitInfoCSVFilename)
					: null;

			PrefAlignHITPublisher publisher = new PrefAlignHITPublisher(client);
			HITInfo hitInfo = publisher.publishHIT(!withExplanation, hitIndex);
			publisher.writeHITInfoToCSVFile(hitIndex, hitInfo, currentHITInfoCSVFile);
		} else if (option.equals("deleteHITs")) {
			String hitTypeId = args[2];
			MTurkAPIUtils.deleteHITs(client, hitTypeId);
		} else if (option.equals("approveAssignmentsOfReviewableHITs")) {
			String hitTypeId = args[2];
			MTurkAPIUtils.approveAssignmentsOfReviewableHITs(client, hitTypeId);
		}
	}

}
