package mobilerobot.study.prefalign;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import mobilerobot.study.utilities.QuestionUtils;

public class PrefAlignQuestionLinker {

	private Map<JSONObject, Set<File>> mQuestionDirsGroupedByCostStruct = new HashMap<>();
	private File mRootDir;
	private double mAlignProb;
	private double mUnalignThreshold;

	public PrefAlignQuestionLinker(File rootDir, double alignProb, double unalignThreshold) {
		mRootDir = rootDir;
		mAlignProb = alignProb;
		mUnalignThreshold = unalignThreshold;
	}

	public void groupQuestionDirsByCostStruct() throws IOException, ParseException {
		for (File questionDir : QuestionUtils.listQuestionDirs(mRootDir)) {
			JSONObject costStructJsonObj = QuestionUtils.getSimpleCostStructureJSONObject(questionDir);

			if (!mQuestionDirsGroupedByCostStruct.containsKey(costStructJsonObj)) {
				mQuestionDirsGroupedByCostStruct.put(costStructJsonObj, new HashSet<>());
			}

			mQuestionDirsGroupedByCostStruct.get(costStructJsonObj).add(questionDir);
		}
	}

	public LinkedPrefAlignQuestions[] getAllLinkedPrefAlignQuestions(int numQuestions)
			throws IOException, ParseException {
		File[][] allLinkedQuestionDirs = getAllLinkedQuestionDirs(numQuestions);
		int[][] allLinkedQuestionAgentIndices = getAllLinkedQuestionAgentIndices(allLinkedQuestionDirs);

		LinkedPrefAlignQuestions[] allLinkedPrefAlignQuestions = new LinkedPrefAlignQuestions[allLinkedQuestionDirs.length];
		for (int i = 0; i < allLinkedQuestionDirs.length; i++) {
			File[] linkedQuestionDirs = allLinkedQuestionDirs[i];
			int[] linkedQuestionAgentIndices = allLinkedQuestionAgentIndices[i];

			LinkedPrefAlignQuestions linkedPrefAlignQuestions = new LinkedPrefAlignQuestions(linkedQuestionDirs,
					linkedQuestionAgentIndices);
			allLinkedPrefAlignQuestions[i] = linkedPrefAlignQuestions;
		}
		return allLinkedPrefAlignQuestions;
	}

	private File[][] getAllLinkedQuestionDirs(int numQuestions) {
		File[][] allLinkedQuestionDirs = new File[mQuestionDirsGroupedByCostStruct.size()][numQuestions];
		int i = 0;
		for (Set<File> questionDirGroup : mQuestionDirsGroupedByCostStruct.values()) {
			// Arbitrary selection and ordering of questions, for each cost structure
			Iterator<File> iter = questionDirGroup.iterator();
			for (int j = 0; j < numQuestions; j++) {
				if (iter.hasNext()) {
					File questionDir = iter.next();
					allLinkedQuestionDirs[i][j] = questionDir;
				}
			}
			i++;
		}
		return allLinkedQuestionDirs;
	}

	private int[][] getAllLinkedQuestionAgentIndices(File[][] allLinkedQuestionDirs)
			throws IOException, ParseException {
		int numQuestions = allLinkedQuestionDirs[0].length;
		int[][] allLinkedQuestionAgentIndices = new int[mQuestionDirsGroupedByCostStruct.size()][numQuestions];

		for (int i = 0; i < allLinkedQuestionDirs.length; i++) {
			// Each list of questions is assigned to a single participant
			File[] linkedQuestionDirs = allLinkedQuestionDirs[i];

			for (int j = 0; j < numQuestions; j++) {
				File questionDir = linkedQuestionDirs[j];

				// questionDir can be null if, for a particular cost structure, there are fewer associated questions than numQuestions
				if (questionDir != null) {
					// For each question dir, randomly select an agent
					AgentRandomizer agentRand = new AgentRandomizer(questionDir, mAlignProb, mUnalignThreshold);
					int agentIndex = agentRand.randomAgentIndex();

					allLinkedQuestionAgentIndices[i][j] = agentIndex;
				}
			}
		}

		return allLinkedQuestionAgentIndices;
	}
}
