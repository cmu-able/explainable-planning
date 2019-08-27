package mobilerobot.study.prefalign;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import mobilerobot.study.utilities.Histogram;
import mobilerobot.study.utilities.QuestionUtils;
import mobilerobot.utilities.FileIOUtils;

public class PrefAlignQuestionLinker {

	private static final double ALIGN_PROB = 0.5;
	private static final double UNALIGN_THRESHOLD = 0.95;

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

			// Serialize each LinkedPrefAlignQuestions object at /output/
			// The output .ser files will be moved to /study/prefalign/serialized-linked-questions/
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

	private static long[][] generateSeedsForRandomAgentSelection(long iniSeed, int numRows, int numColumns) {
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

	public static void serializeLinkedPrefAlignQuestions(LinkedPrefAlignQuestions linkedPrefAlignQuestions)
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

	public static LinkedPrefAlignQuestions[] readAllLinkedPrefAlignQuestions(File serLinkedQuestionsDir)
			throws IOException, ClassNotFoundException {
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

	public static LinkedPrefAlignQuestions[] insertValidationQuestions(
			LinkedPrefAlignQuestions[] allLinkedPrefAlignQuestions, boolean controlGroup)
			throws URISyntaxException, IOException, ParseException {
		File validationQuestionsRootDir = FileIOUtils.getResourceDir(PrefAlignQuestionLinker.class,
				"validation-questions");
		File[] validationQuestionDirs = QuestionUtils.listQuestionDirs(validationQuestionsRootDir);

		int numLinks = allLinkedPrefAlignQuestions.length;
		int numValidationQuestionsPerLink = validationQuestionDirs.length / numLinks;
		int numTotalQuestionsPerLink = allLinkedPrefAlignQuestions[0].getNumQuestions() + numValidationQuestionsPerLink;

		long[][] seeds = generateSeedsForRandomAgentSelection(INI_SEED, numLinks, numValidationQuestionsPerLink);

		// Directories for all linked PrefAlign questions, including validation questions
		File[][] allLinkedQuestionDirs = new File[numLinks][numTotalQuestionsPerLink];

		LinkedPrefAlignQuestions[] res = new LinkedPrefAlignQuestions[numLinks];

		for (int i = 0; i < numLinks; i++) {
			LinkedPrefAlignQuestions linkedQuestions = allLinkedPrefAlignQuestions[i];

			// Link that includes validation question(s) so far
			File[] currLinkedQuestionDirs = linkedQuestions.getLinkedQuestionDirs();
			int[] currLinkedQuestionAgentIndices = linkedQuestions.getLinkedQuestionAgentIndices();

			// All PrefAlign questions in a link have the same simple cost structure
			JSONObject costStructJsonObj = QuestionUtils.getSimpleCostStructureJSONObject(currLinkedQuestionDirs[0]);

			// Find validation question(s) for each link
			for (File validationQuestionDir : validationQuestionDirs) {
				JSONObject validationCostStructJsonObj = QuestionUtils
						.getSimpleCostStructureJSONObject(validationQuestionDir);

				// Validation question must have the same cost structure as the linked questions
				if (costStructJsonObj.equals(validationCostStructJsonObj)) {
					// Insert each validation question into the middle of the link, which includes validation question(s) so far
					int middleIndex = currLinkedQuestionDirs.length / 2;
					currLinkedQuestionDirs = ArrayUtils.insert(middleIndex, currLinkedQuestionDirs,
							validationQuestionDir);

					// For control group, validation question(s) can have either aligned or unaligned agent(s)
					// For experimental group, validation question(s) can only have aligned agent(s)
					int validationAgentIndex = 0; // Aligned agent always has index 0
					if (controlGroup) {
						// Randomly select agent for each validation question
						long seed = seeds[i][numTotalQuestionsPerLink - currLinkedQuestionDirs.length];
						AgentRandomizer validationAgentRand = new AgentRandomizer(validationQuestionDir, ALIGN_PROB,
								UNALIGN_THRESHOLD, seed);
						validationAgentIndex = validationAgentRand.randomAgentIndex();
					}
					currLinkedQuestionAgentIndices = ArrayUtils.insert(middleIndex, currLinkedQuestionAgentIndices,
							validationAgentIndex);
				}
			}

			// Copy each link of question dirs, including validation question(s), into allLinkedQuestionDirs
			System.arraycopy(currLinkedQuestionDirs, 0, allLinkedQuestionDirs[i], 0, currLinkedQuestionDirs.length);

			LinkedPrefAlignQuestions updatedLinkedQuestions = new LinkedPrefAlignQuestions(currLinkedQuestionDirs,
					currLinkedQuestionAgentIndices);
			res[i] = updatedLinkedQuestions;

			// Serialized each updated link, which includes validation question(s)
			serializeLinkedPrefAlignQuestions(updatedLinkedQuestions);
		}

		return res;
	}

	public static void main(String[] args)
			throws URISyntaxException, IOException, ParseException, ClassNotFoundException {
		String option = args[0];

		if (option.equals("linkQuestions")) {
			File questionsRootDir = FileIOUtils.getQuestionsResourceDir(PrefAlignQuestionLinker.class);
			int numQuestions = Integer.parseInt(args[1]);

			PrefAlignQuestionLinker questionLinker = new PrefAlignQuestionLinker(questionsRootDir, ALIGN_PROB,
					UNALIGN_THRESHOLD);
			questionLinker.groupQuestionDirsByCostStruct();

			// Serialize each LinkedPrefAlignQuestions object
			// The output .ser files will be moved to /study/prefalign/serialized-linked-questions/
			questionLinker.createAllLinkedPrefAlignQuestions(numQuestions);
		} else if (option.equals("insertValidationQuestions")) {
			boolean controlGroup = !(args.length > 1 && args[1].equals("-e"));

			// Read serialized LinkedPrefAlignQuestions objects that do not contain validation questions
			File serLinkedQuestionsDir = FileIOUtils.getResourceDir(PrefAlignHITPublisher.class,
					"serialized-linked-questions");
			LinkedPrefAlignQuestions[] allLinkedPrefAlignQuestions = readAllLinkedPrefAlignQuestions(
					serLinkedQuestionsDir);

			// Insert validation question(s) into each link and serialize the new LinkedPrefAlignQuestions
			// The output .ser files will be moved to /study/prefalign/serialized-vlinked-questions/ 
			// or /study/prefalign/serialized-vlinked-questions-explanation/
			insertValidationQuestions(allLinkedPrefAlignQuestions, controlGroup);
		} else if (option.equals("agentScoreDistribution")) {
			int numBins = Integer.parseInt(args[1]);

			// Only get agent-score distribution from non-validation questions
			// Read serialized LinkedPrefAlignQuestions objects that do not contain validation questions
			File serLinkedQuestionsDir = FileIOUtils.getResourceDir(PrefAlignHITPublisher.class,
					"serialized-linked-questions");
			LinkedPrefAlignQuestions[] allLinkedPrefAlignQuestions = readAllLinkedPrefAlignQuestions(
					serLinkedQuestionsDir);

			Histogram distribution = QuestionUtils.getAgentScoreDistribution(allLinkedPrefAlignQuestions, numBins);
			distribution.printHistogram();
		}
	}
}
