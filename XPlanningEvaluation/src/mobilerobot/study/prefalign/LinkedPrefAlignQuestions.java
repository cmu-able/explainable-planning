package mobilerobot.study.prefalign;

import java.io.File;
import java.io.Serializable;
import java.util.Arrays;

import mobilerobot.study.utilities.QuestionUtils;

public class LinkedPrefAlignQuestions implements Serializable {

	/**
	 * Auto-generated serial version UID
	 */
	private static final long serialVersionUID = -2764050754583200643L;

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private final File[] mLinkedQuestionDirs;
	private final int[] mLinkedQuestionAgentIndices;

	// Derived fields
	private final String[] mLinkedQuestionDocNames;
	private final String[] mLinkedQuestionDocNamesWithExplanation;

	public LinkedPrefAlignQuestions(File[] linkedQuestionDirs, int[] linkedQuestionAgentIndices) {
		mLinkedQuestionDirs = linkedQuestionDirs;
		mLinkedQuestionAgentIndices = linkedQuestionAgentIndices;

		mLinkedQuestionDocNames = createLinkedQuestionDocumentNames(linkedQuestionDirs, linkedQuestionAgentIndices,
				false);
		mLinkedQuestionDocNamesWithExplanation = createLinkedQuestionDocumentNames(linkedQuestionDirs,
				linkedQuestionAgentIndices, true);
	}

	private String[] createLinkedQuestionDocumentNames(File[] linkedQuestionDirs, int[] linkedQuestionAgentIndices,
			boolean withExplanation) {
		String[] linkedQuestionDocNames = new String[linkedQuestionDirs.length];
		for (int i = 0; i < linkedQuestionDirs.length; i++) {
			File questionDir = linkedQuestionDirs[i];
			int agentIndex = linkedQuestionAgentIndices[i];

			if (questionDir != null) {
				String questionDocName = QuestionUtils.getPrefAlignQuestionDocumentName(questionDir, agentIndex,
						withExplanation);
				linkedQuestionDocNames[i] = questionDocName;
			}
		}
		return linkedQuestionDocNames;
	}

	public int getNumQuestions() {
		return mLinkedQuestionDirs.length;
	}

	public File[] getLinkedQuestionDirs() {
		return mLinkedQuestionDirs;
	}

	public int[] getLinkedQuestionAgentIndices() {
		return mLinkedQuestionAgentIndices;
	}

	public String[] getLinkedQuestionDocumentNames(boolean withExplanation) {
		return withExplanation ? mLinkedQuestionDocNamesWithExplanation : mLinkedQuestionDocNames;
	}

	public File getQuestionDir(int questionIndex) {
		return mLinkedQuestionDirs[questionIndex];
	}

	public int getQuestionAgentIndex(int questionIndex) {
		return mLinkedQuestionAgentIndices[questionIndex];
	}

	public String getQuestionDocumentName(int questionIndex, boolean withExplanation) {
		return withExplanation ? mLinkedQuestionDocNamesWithExplanation[questionIndex]
				: mLinkedQuestionDocNames[questionIndex];
	}

	public boolean hasNextQuestion(int currentQuestionIndex) {
		return currentQuestionIndex < getNumQuestions() - 1 && mLinkedQuestionDirs[currentQuestionIndex + 1] != null;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof LinkedPrefAlignQuestions)) {
			return false;
		}
		LinkedPrefAlignQuestions linkedQuestions = (LinkedPrefAlignQuestions) obj;
		return Arrays.equals(linkedQuestions.mLinkedQuestionDirs, mLinkedQuestionDirs)
				&& Arrays.equals(linkedQuestions.mLinkedQuestionAgentIndices, mLinkedQuestionAgentIndices);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + Arrays.hashCode(mLinkedQuestionDirs);
			result = 31 * result + Arrays.hashCode(mLinkedQuestionAgentIndices);
			hashCode = result;
		}
		return hashCode;
	}
}
