package mobilerobot.study.prefalign;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mobilerobot.study.mturk.HITAssignmentsCollector;
import mobilerobot.study.mturk.HITInfo;
import software.amazon.awssdk.services.mturk.MTurkClient;
import software.amazon.awssdk.services.mturk.model.Assignment;

public class PrefAlignHITAssignmentsCollector {

	private final HITAssignmentsCollector mHITCollector;

	public PrefAlignHITAssignmentsCollector(MTurkClient client) {
		mHITCollector = new HITAssignmentsCollector(client);
	}

	public Map<HITInfo, List<Assignment>> collectAllPendingReviewHITAssignments(File hitInfoCSVFile)
			throws IOException {
		Map<HITInfo, List<Assignment>> allPendingReviewAssignments = new HashMap<>();
		for (HITInfo hitInfo : readAllHITInfos(hitInfoCSVFile)) {
			List<Assignment> pendingReviewAssignments = mHITCollector.collectPendingReviewHITAssignments(hitInfo);
			allPendingReviewAssignments.put(hitInfo, pendingReviewAssignments);
		}
		return allPendingReviewAssignments;
	}

	private List<HITInfo> readAllHITInfos(File hitInfoCSVFile) throws IOException {
		List<HITInfo> hitInfos = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new FileReader(hitInfoCSVFile))) {
			String line = reader.readLine(); // Skip header line

			while ((line = reader.readLine()) != null) {
				String[] values = line.split(",");
				String hitId = values[0];
				String hitTypeId = values[1];
				HITInfo hitInfo = new HITInfo(hitId, hitTypeId);

				hitInfos.add(hitInfo);
			}
		}
		return hitInfos;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
