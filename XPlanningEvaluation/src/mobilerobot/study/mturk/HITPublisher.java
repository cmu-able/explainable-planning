package mobilerobot.study.mturk;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import software.amazon.awssdk.services.mturk.MTurkClient;
import software.amazon.awssdk.services.mturk.model.CreateHitTypeRequest;
import software.amazon.awssdk.services.mturk.model.CreateHitTypeResponse;
import software.amazon.awssdk.services.mturk.model.CreateHitWithHitTypeRequest;
import software.amazon.awssdk.services.mturk.model.CreateHitWithHitTypeResponse;
import software.amazon.awssdk.services.mturk.model.HIT;

public class HITPublisher {

	private static final String TITLE = "Title";
	private static final String DESCRIPTION_CG = "Description (control group)";
	private static final String DESCRIPTION_EG = "Description (experimental group)";
	private static final String KEYWORDS = "Keywords";
	private static final String REWARD = "0.20";
	private static final long ASSIGNMENT_DURATION = 30 * 60L; // 30 minutes
	private static final long LIFE_TIME = 1 * 7 * 24 * 60 * 60L; // 1 week
	private static final int MAX_ASSIGNMENTS = 10;

	private final MTurkClient mClient;
	private final List<HITInfo> mPublishedHITInfos = new ArrayList<>();

	public HITPublisher(MTurkClient client) {
		mClient = client;
	}

	public HITInfo publishHIT(File questionXMLFile, boolean controlGroup) throws IOException {
		// Read the question XML into a String
		String question = new String(Files.readAllBytes(questionXMLFile.toPath()));

		String hitTypeId = createHITType(controlGroup);
		HIT hit = createHITWithHITType(hitTypeId, question);

		HITInfo hitInfo = new HITInfo(hit.hitId(), hitTypeId);
		mPublishedHITInfos.add(hitInfo);
		return hitInfo;
	}

	public List<HITInfo> getAllPublishedHITInfos() {
		return mPublishedHITInfos;
	}

	private String createHITType(boolean controlGroup) {
		CreateHitTypeRequest.Builder builder = CreateHitTypeRequest.builder();
		builder.title(TITLE);
		builder.description(controlGroup ? DESCRIPTION_CG : DESCRIPTION_EG);
		builder.keywords(KEYWORDS);
		builder.reward(REWARD);
		builder.assignmentDurationInSeconds(ASSIGNMENT_DURATION);
		builder.qualificationRequirements(MTurkAPIUtils.getLocaleRequirement());
		CreateHitTypeRequest createHITTypeRequest = builder.build();
		CreateHitTypeResponse response = mClient.createHITType(createHITTypeRequest);
		return response.hitTypeId();
	}

	private HIT createHITWithHITType(String hitTypeId, String question) {
		CreateHitWithHitTypeRequest.Builder builder = CreateHitWithHitTypeRequest.builder();
		builder.hitTypeId(hitTypeId);
		builder.question(question);
		builder.lifetimeInSeconds(LIFE_TIME);
		builder.maxAssignments(MAX_ASSIGNMENTS);
		CreateHitWithHitTypeRequest createHITWithHITTypeRequest = builder.build();
		CreateHitWithHitTypeResponse response = mClient.createHITWithHITType(createHITWithHITTypeRequest);
		return response.hit();
	}

}
