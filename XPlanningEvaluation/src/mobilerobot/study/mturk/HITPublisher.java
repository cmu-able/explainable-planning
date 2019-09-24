package mobilerobot.study.mturk;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import mobilerobot.utilities.FileIOUtils;
import software.amazon.awssdk.services.mturk.MTurkClient;
import software.amazon.awssdk.services.mturk.model.CreateHitTypeRequest;
import software.amazon.awssdk.services.mturk.model.CreateHitTypeResponse;
import software.amazon.awssdk.services.mturk.model.CreateHitWithHitTypeRequest;
import software.amazon.awssdk.services.mturk.model.CreateHitWithHitTypeResponse;
import software.amazon.awssdk.services.mturk.model.HIT;
import software.amazon.awssdk.services.mturk.model.QualificationRequirement;
import software.amazon.awssdk.services.mturk.model.ReviewPolicy;

public class HITPublisher {

	private final MTurkClient mClient;

	public HITPublisher(MTurkClient client) {
		mClient = client;
	}

	/**
	 * Publish HIT on MTurk. Only 1 HIT will be published at a time.
	 * 
	 * @param questionXMLFile
	 * @param controlGroup
	 * @param assignmentReviewPolicy
	 * @return HITInfo
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public HITInfo publishHIT(File questionXMLFile, HITGroupInfo hitGroupInfo, ReviewPolicy assignmentReviewPolicy)
			throws IOException, URISyntaxException {
		// Read the question XML into a String
		String question = new String(Files.readAllBytes(questionXMLFile.toPath()));

		String hitTypeId = createHITType(hitGroupInfo);
		HIT hit = createHITWithHITType(hitTypeId, question, hitGroupInfo, assignmentReviewPolicy);

		return new HITInfo(hit.hitId(), hitTypeId);
	}

	private String createHITType(HITGroupInfo hitGroupInfo) throws IOException, URISyntaxException {
		CreateHitTypeRequest.Builder builder = CreateHitTypeRequest.builder();
		builder.title(hitGroupInfo.getTitle());
		builder.description(hitGroupInfo.getDescription());
		builder.keywords(hitGroupInfo.getKeywords());
		builder.reward(hitGroupInfo.getReward());
		builder.assignmentDurationInSeconds(hitGroupInfo.getAssignmentDuration());

		QualificationRequirement localeRequirement = QualificationUtils.createLocaleRequirement();
		boolean isSandbox = mClient.serviceName().contains("sandbox");
		QualificationRequirement mastersRequirement = QualificationUtils
				.createMastersQualificationRequirement(isSandbox);
		QualificationRequirement testRequirement = QualificationUtils.createTestQualificationRequirement(mClient);
		QualificationRequirement firstParticipationRequirement = QualificationUtils
				.createFirstParticipationRequirement(mClient);
		builder.qualificationRequirements(localeRequirement, mastersRequirement, testRequirement,
				firstParticipationRequirement);

		CreateHitTypeRequest createHITTypeRequest = builder.build();
		CreateHitTypeResponse response = mClient.createHITType(createHITTypeRequest);
		return response.hitTypeId();
	}

	private HIT createHITWithHITType(String hitTypeId, String question, HITGroupInfo hitGroupInfo,
			ReviewPolicy assignmentReviewPolicy) {
		CreateHitWithHitTypeRequest.Builder builder = CreateHitWithHitTypeRequest.builder();
		builder.hitTypeId(hitTypeId);
		builder.question(question);
		builder.lifetimeInSeconds(hitGroupInfo.getLifetimeInSeconds());
		builder.maxAssignments(hitGroupInfo.getMaxAssignments());
		if (assignmentReviewPolicy != null) {
			builder.assignmentReviewPolicy(assignmentReviewPolicy);
		}
		CreateHitWithHitTypeRequest createHITWithHITTypeRequest = builder.build();
		CreateHitWithHitTypeResponse response = mClient.createHITWithHITType(createHITWithHITTypeRequest);
		return response.hit();
	}

	public static File createExternalQuestionXMLFile(String externalURL, String questionXMLFilename)
			throws ParserConfigurationException, IOException, TransformerException {
		Document doc = FileIOUtils.createXMLDocument();

		// <ExternalQuestion xmlns="[the ExternalQuestion schema URL]">
		Element externalQuestionElement = doc.createElement("ExternalQuestion");
		externalQuestionElement.setAttribute("xmlns",
				"http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2006-07-14/ExternalQuestion.xsd");
		doc.appendChild(externalQuestionElement);

		// <ExternalURL>...</ExternalURL>
		Element externalURLElement = doc.createElement("ExternalURL");
		Text externalURLText = doc.createTextNode(externalURL);
		externalURLElement.appendChild(externalURLText);
		externalQuestionElement.appendChild(externalURLElement);

		// <FrameHeight>0</FrameHeight>
		Element frameHeightElement = doc.createElement("FrameHeight");
		Text frameHeightText = doc.createTextNode("0");
		frameHeightElement.appendChild(frameHeightText);
		externalQuestionElement.appendChild(frameHeightElement);

		File questionXMLFile = FileIOUtils.createOutputFile(questionXMLFilename);
		FileIOUtils.writeXMLDocumentToFile(doc, questionXMLFile);

		return questionXMLFile;
	}

	public static File getExternalQuestionXMLFile(String questionXMLFilename)
			throws FileNotFoundException, URISyntaxException {
		return FileIOUtils.getFile(HITPublisher.class, "external-questions", questionXMLFilename);
	}

}
