package mobilerobot.study.mturk;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	private static final String TITLE = "Understanding mobile robot navigation planning";
	private static final String DESCRIPTION_FORMAT = "This is a study to examine how people interpret and understand the objectives of an autonomous agent (e.g., a robot) by observing its behavior. (GROUP %d)";
	private static final String DESCRIPTION_CG = String.format(DESCRIPTION_FORMAT, 1);
	private static final String DESCRIPTION_EG = String.format(DESCRIPTION_FORMAT, 2);
	private static final String KEYWORDS = "Research study, experiment, human-robot interaction, human-AI interaction, mobile robot indoor navigation";
	private static final String REWARD = "0.20";
	private static final long ASSIGNMENT_DURATION = 30 * 60L; // 30 minutes
	private static final long LIFE_TIME = 1 * 7 * 24 * 60 * 60L; // 1 week
	static final int MAX_ASSIGNMENTS = 10;

	private final MTurkClient mClient;
	private final Map<Boolean, String> mHITTypeIds = new HashMap<>();
	private final List<HITInfo> mPublishedHITInfos = new ArrayList<>();

	public HITPublisher(MTurkClient client) {
		mClient = client;
	}

	public HITInfo publishHIT(File questionXMLFile, boolean controlGroup, ReviewPolicy assignmentReviewPolicy)
			throws IOException, URISyntaxException {
		// Read the question XML into a String
		String question = new String(Files.readAllBytes(questionXMLFile.toPath()));

		String hitTypeId = mHITTypeIds.containsKey(controlGroup) ? mHITTypeIds.get(controlGroup)
				: createHITType(controlGroup);
		HIT hit = createHITWithHITType(hitTypeId, question, assignmentReviewPolicy);

		HITInfo hitInfo = new HITInfo(hit.hitId(), hitTypeId);

		// Keep track of all published HITs
		mPublishedHITInfos.add(hitInfo);

		return hitInfo;
	}

	public List<HITInfo> getAllPublishedHITInfos() {
		return mPublishedHITInfos;
	}

	private String createHITType(boolean controlGroup) throws IOException, URISyntaxException {
		CreateHitTypeRequest.Builder builder = CreateHitTypeRequest.builder();
		builder.title(TITLE);
		builder.description(controlGroup ? DESCRIPTION_CG : DESCRIPTION_EG);
		builder.keywords(KEYWORDS);
		builder.reward(REWARD);
		builder.assignmentDurationInSeconds(ASSIGNMENT_DURATION);

		QualificationRequirement localeRequirement = MTurkAPIUtils.getLocaleRequirement();
		QualificationRequirement consentRequirement = MTurkAPIUtils.createConsentRequirement(mClient);
		builder.qualificationRequirements(localeRequirement, consentRequirement);

		CreateHitTypeRequest createHITTypeRequest = builder.build();
		CreateHitTypeResponse response = mClient.createHITType(createHITTypeRequest);
		String hitTypeId = response.hitTypeId();

		// Only create HIT Type for each group once
		mHITTypeIds.put(controlGroup, hitTypeId);
		return hitTypeId;
	}

	private HIT createHITWithHITType(String hitTypeId, String question, ReviewPolicy assignmentReviewPolicy) {
		CreateHitWithHitTypeRequest.Builder builder = CreateHitWithHitTypeRequest.builder();
		builder.hitTypeId(hitTypeId);
		builder.question(question);
		builder.lifetimeInSeconds(LIFE_TIME);
		builder.maxAssignments(MAX_ASSIGNMENTS);
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
