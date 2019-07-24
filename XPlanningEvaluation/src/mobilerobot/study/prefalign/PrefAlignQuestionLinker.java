package mobilerobot.study.prefalign;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import mobilerobot.study.utilities.QuestionUtils;
import mobilerobot.utilities.FileIOUtils;

public class PrefAlignQuestionLinker {

	// To ensure that random-agent-selection from each question dir is deterministic across different program runs
	private static final long INI_SEED = 0L;

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

	public LinkedPrefAlignQuestions[] createAllLinkedPrefAlignQuestions(int numQuestions)
			throws IOException, ParseException {
		File[][] allLinkedQuestionDirs = createAllLinkedQuestionDirs(numQuestions);
		int[][] allLinkedQuestionAgentIndices = createAllLinkedQuestionAgentIndices(allLinkedQuestionDirs);

		LinkedPrefAlignQuestions[] allLinkedPrefAlignQuestions = new LinkedPrefAlignQuestions[allLinkedQuestionDirs.length];
		for (int i = 0; i < allLinkedQuestionDirs.length; i++) {
			File[] linkedQuestionDirs = allLinkedQuestionDirs[i];
			int[] linkedQuestionAgentIndices = allLinkedQuestionAgentIndices[i];

			LinkedPrefAlignQuestions linkedPrefAlignQuestions = new LinkedPrefAlignQuestions(linkedQuestionDirs,
					linkedQuestionAgentIndices);
			allLinkedPrefAlignQuestions[i] = linkedPrefAlignQuestions;

			// Serialize each LinkedPrefAlignQuestions object and save it at /study/prefalign/serialized-linked-questions/
			// PrefAlignHITPublisher will de-serialize and use it for publishing HIT (e.g., create Assignment Review Policy)
			serializeLinkedPrefAlignQuestions(linkedPrefAlignQuestions);
		}
		return allLinkedPrefAlignQuestions;
	}

	private File[][] createAllLinkedQuestionDirs(int numQuestions) {
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

	private int[][] createAllLinkedQuestionAgentIndices(File[][] allLinkedQuestionDirs)
			throws IOException, ParseException {
		int numQuestions = allLinkedQuestionDirs[0].length;
		int[][] allLinkedQuestionAgentIndices = new int[mQuestionDirsGroupedByCostStruct.size()][numQuestions];

		// To ensure that random-agent-selection from each question dir is deterministic across different program runs
		long[][] seeds = generateSeedsForRandomAgentSelection(INI_SEED, allLinkedQuestionAgentIndices.length,
				numQuestions);

		for (int i = 0; i < allLinkedQuestionDirs.length; i++) {
			// Each list of questions is assigned to a single participant
			File[] linkedQuestionDirs = allLinkedQuestionDirs[i];

			for (int j = 0; j < numQuestions; j++) {
				File questionDir = linkedQuestionDirs[j];

				// questionDir can be null if, for a particular cost structure, there are fewer associated questions than numQuestions
				if (questionDir != null) {
					// For each question dir, randomly select an agent
					long seed = seeds[i][j];
					AgentRandomizer agentRand = new AgentRandomizer(questionDir, mAlignProb, mUnalignThreshold, seed);
					int agentIndex = agentRand.randomAgentIndex();

					allLinkedQuestionAgentIndices[i][j] = agentIndex;
				}
			}
		}

		return allLinkedQuestionAgentIndices;
	}

	private long[][] generateSeedsForRandomAgentSelection(long iniSeed, int numRows, int numColumns) {
		Random rand = new Random(iniSeed);
		long[][] seeds = new long[numRows][numColumns];
		for (int i = 0; i < numRows; i++) {
			for (int j = 0; j < numColumns; j++) {
				long randomLong = rand.nextLong();
				seeds[i][j] = randomLong;
			}
		}
		return seeds;
	}

	private void serializeLinkedPrefAlignQuestions(LinkedPrefAlignQuestions linkedPrefAlignQuestions)
			throws IOException {
		// Use document name of the first question in the link as name of the serialized LinkedPrefAlignQuestions .ser file
		String headQuestionDocName = linkedPrefAlignQuestions.getQuestionDocumentName(0, false);
		File objFile = FileIOUtils.createOutputFile(headQuestionDocName + ".ser");

		try (FileOutputStream fileOut = new FileOutputStream(objFile)) {
			try (ObjectOutputStream objectOut = new ObjectOutputStream(fileOut)) {
				objectOut.writeObject(linkedPrefAlignQuestions);
			}
		}
	}
}
