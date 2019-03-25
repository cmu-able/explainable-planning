package solver.prismconnector.explicitmodel;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import solver.prismconnector.PrismRewardType;
import solver.prismconnector.QFunctionEncodingScheme;

public class PrismExplicitModelPointer {

	// Explicit model files
	private static final String STA_EXTENSION = ".sta";
	private static final String TRA_EXTENSION = ".tra";
	private static final String LAB_EXTENSION = ".lab";
	private static final String SREW_EXTENSION = ".srew";
	private static final String TREW_EXTENSION = ".trew";
	private static final String PROD_STA_FILENAME_SUFFIX = "_prod.sta";
	private static final String ADV_FILENAME_SUFFIX = "_adv.tra";

	// Prism MDP model file
	private static final String MDP_EXTENSION = ".mdp";

	// All extensions
	private static final String[] EXTENSIONS = { STA_EXTENSION, TRA_EXTENSION, LAB_EXTENSION, SREW_EXTENSION,
			TREW_EXTENSION, MDP_EXTENSION };

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	// Model directory
	private File mModelDir;

	// Explicit model files
	private File mStaFile;
	private File mProdStaFile;
	private File mTraFile;
	private File mAdvFile;
	private File mLabFile;
	private PrismRewardType mRewardType;
	private File mRewFile; // This is either .srew file or .trew file
	private List<File> mIndexedRewFiles = new ArrayList<>();

	// Prism MDP model file -- for debugging purposes
	private File mMDPFile;

	/**
	 * Use this constructor if the PRISM explicit model does not exist yet at modelPath. Create a modelPath directory if
	 * it doesn't already exist.
	 * 
	 * @param modelPath
	 * @param modelFilenamePrefix
	 * @param prismRewardType
	 */
	public PrismExplicitModelPointer(String modelPath, String modelFilenamePrefix, PrismRewardType prismRewardType) {
		mModelDir = new File(modelPath);
		mModelDir.mkdirs(); // only make directories when ones don't exist

		mStaFile = new File(modelPath, modelFilenamePrefix + STA_EXTENSION);
		mProdStaFile = new File(modelPath, modelFilenamePrefix + PROD_STA_FILENAME_SUFFIX);
		mTraFile = new File(modelPath, modelFilenamePrefix + TRA_EXTENSION);
		mAdvFile = new File(modelPath, modelFilenamePrefix + ADV_FILENAME_SUFFIX);
		mLabFile = new File(modelPath, modelFilenamePrefix + LAB_EXTENSION);
		mRewardType = prismRewardType;
		if (prismRewardType == PrismRewardType.STATE_REWARD) {
			mRewFile = new File(modelPath, modelFilenamePrefix + SREW_EXTENSION);
		} else {
			mRewFile = new File(modelPath, modelFilenamePrefix + TREW_EXTENSION);
		}
		// mIndexedRewFiles will be created once the PRISM explicit model is created

		// Prism MDP model file is for debugging purposes
		mMDPFile = new File(modelPath, modelFilenamePrefix + MDP_EXTENSION);
	}

	/**
	 * Use this constructor if the PRISM explicit model already exists at modelPath.
	 * 
	 * @param modelPath
	 */
	public PrismExplicitModelPointer(String modelPath, PrismRewardType prismRewardType) {
		mModelDir = new File(modelPath);

		if (!mModelDir.exists()) {
			throw new IllegalStateException("Directory " + modelPath + " must already exist");
		}

		mRewardType = prismRewardType;

		File[] prismFiles = mModelDir.listFiles((dir, filename) -> filterFilename(filename));

		for (File prismFile : prismFiles) {
			String filename = prismFile.getName().toLowerCase();
			if (filename.endsWith(PROD_STA_FILENAME_SUFFIX)) { // "{filename}_prod.sta"
				mProdStaFile = prismFile;
			} else if (filename.endsWith(STA_EXTENSION)) { // "{filename}.sta"
				mStaFile = prismFile;
			} else if (filename.endsWith(ADV_FILENAME_SUFFIX)) { // "{filename}_adv.tra"
				mAdvFile = prismFile;
			} else if (filename.endsWith(TRA_EXTENSION)) { // "{filename}.tra"
				mTraFile = prismFile;
			} else if (filename.endsWith(LAB_EXTENSION)) { // "{filename}.lab"
				mLabFile = prismFile;
			} else if ((mRewardType == PrismRewardType.STATE_REWARD && filename.endsWith(SREW_EXTENSION)
					|| mRewardType == PrismRewardType.TRANSITION_REWARD && filename.endsWith(TREW_EXTENSION))) {
				// "{filename}k.srew or .trew
				mIndexedRewFiles.add(prismFile);
			} else if (filename.endsWith(MDP_EXTENSION)) { // "{filename}.mdp"
				mMDPFile = prismFile;
			}
		}
		mIndexedRewFiles.sort((file1, file2) -> file1.compareTo(file2));
		mRewFile = getRewardFile(modelPath, getRewardFileExtension(prismRewardType));
	}

	private boolean filterFilename(String filename) {
		for (String extension : EXTENSIONS) {
			if (filename.toLowerCase().endsWith(extension)) {
				return true;
			}
		}
		return false;
	}

	private List<File> getSortedRewardsFiles() {
		String rewExtension = getRewardFileExtension(mRewardType);
		File[] rewFiles = mModelDir.listFiles((dir, name) -> name.toLowerCase().endsWith(rewExtension));

		// Sort .srew/trew files lexicographically based on their abstract pathnames
		// {name}1.srew/trew, {name}2.srew/trew, {name}3.srew/trew, ...
		Arrays.sort(rewFiles, (file1, file2) -> file1.compareTo(file2));
		return Arrays.asList(rewFiles);
	}

	/**
	 * If there are multiple reward structures, this file "pointer" doesn't correspond to any actual file. It is only
	 * used as an input parameter to PRISM's explicit reward export.
	 * 
	 * @param modelPath
	 * @param rewExtension
	 * @return {reward filename}.srew/trew
	 */
	private File getRewardFile(String modelPath, String rewExtension) {
		if (mIndexedRewFiles.size() > 1) {
			File rew1File = mIndexedRewFiles.get(0);
			String rew1Filename = rew1File.getName(); // <name>1.srew/trew
			String rewFilenamePrefix = rew1Filename.substring(0, rew1Filename.indexOf(rewExtension) - 1); // <name>
			return new File(modelPath, rewFilenamePrefix + rewExtension); // <name>.srew/trew
		}
		return mIndexedRewFiles.get(0);
	}

	private String getRewardFileExtension(PrismRewardType prismRewardType) {
		return prismRewardType == PrismRewardType.STATE_REWARD ? SREW_EXTENSION : TREW_EXTENSION;
	}

	public File getExplicitModelDirectory() {
		return mModelDir;
	}

	public File getMDPFile() {
		return mMDPFile;
	}

	public File getStatesFile() {
		return mStaFile;
	}

	public boolean productStatesFileExists() {
		return mProdStaFile.exists();
	}

	public File getProductStatesFile() {
		return mProdStaFile;
	}

	public File getTransitionsFile() {
		return mTraFile;
	}

	public File getAdversaryFile() {
		return mAdvFile;
	}

	public File getLabelsFile() {
		return mLabFile;
	}

	public PrismRewardType getPrismRewardType() {
		return mRewardType;
	}

	/**
	 * If there are multiple reward structures, this file "pointer" doesn't correspond to any actual file. It is only
	 * used as an input parameter to PRISM's explicit reward export.
	 * 
	 * @return State rewards (.srew) file
	 */
	public File getStateRewardsFile() {
		checkRewardType(PrismRewardType.STATE_REWARD);
		return mRewFile;
	}

	/**
	 * If there are multiple reward structures, this file "pointer" doesn't correspond to any actual file. It is only
	 * used as input parameter to PRISM's explicit reward export.
	 * 
	 * @return Transition rewards (.trew) file
	 */
	public File getTransitionRewardsFile() {
		checkRewardType(PrismRewardType.TRANSITION_REWARD);
		return mRewFile;
	}

	/**
	 * If there are multiple reward structures, this method returns an actual .srew file of a given index.
	 * 
	 * @param rewardStructIndex
	 * @return State rewards ({i}.srew) file of a given index
	 */
	public File getIndexedStateRewardsFile(int rewardStructIndex) {
		checkRewardType(PrismRewardType.STATE_REWARD);
		return getIndexRewardsFile(rewardStructIndex);
	}

	/**
	 * If there are multiple reward structures, this method returns an actual .trew file of a given index.
	 * 
	 * @param rewardStructIndex
	 * @return Transition rewards ({i}.trew) file of a given index
	 */
	public File getIndexedTransitionRewardsFile(int rewardStructIndex) {
		checkRewardType(PrismRewardType.TRANSITION_REWARD);
		return getIndexRewardsFile(rewardStructIndex);
	}

	/**
	 * Get the .srew/.trew file at a given PRISM reward-structure index.
	 * 
	 * @param rewardStructIndex
	 *            : PRISM reward-structure index (starts at 1)
	 * @return Reward structure (.srew/.trew) file at the given index
	 */
	private File getIndexRewardsFile(int rewardStructIndex) {
		if (mIndexedRewFiles.isEmpty()) {
			mIndexedRewFiles.addAll(getSortedRewardsFiles());
		}
		return mIndexedRewFiles.get(rewardStructIndex - QFunctionEncodingScheme.START_REW_STRUCT_INDEX);
	}

	private void checkRewardType(PrismRewardType prismRewardType) {
		if (mRewardType != prismRewardType) {
			String rewExtension = getRewardFileExtension(prismRewardType);
			throw new UnsupportedOperationException(String.format("No %s rewards file", rewExtension));
		}
	}

	public int getNumRewardStructs() {
		if (mIndexedRewFiles.isEmpty()) {
			mIndexedRewFiles.addAll(getSortedRewardsFiles());
		}
		return mIndexedRewFiles.size();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof PrismExplicitModelPointer)) {
			return false;
		}
		PrismExplicitModelPointer pointer = (PrismExplicitModelPointer) obj;
		return pointer.mModelDir.equals(mModelDir) && pointer.mStaFile.equals(mStaFile)
				&& pointer.mProdStaFile.equals(mProdStaFile) && pointer.mTraFile.equals(mTraFile)
				&& pointer.mAdvFile.equals(mAdvFile) && pointer.mLabFile.equals(mLabFile)
				&& pointer.mRewardType.equals(mRewardType) && pointer.mRewFile.equals(mRewFile)
				&& pointer.mIndexedRewFiles.equals(mIndexedRewFiles) && pointer.mMDPFile.equals(mMDPFile);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mModelDir.hashCode();
			result = 31 * result + mStaFile.hashCode();
			result = 31 * result + mProdStaFile.hashCode();
			result = 31 * result + mTraFile.hashCode();
			result = 31 * result + mAdvFile.hashCode();
			result = 31 * result + mLabFile.hashCode();
			result = 31 * result + mRewardType.hashCode();
			result = 31 * result + mRewFile.hashCode();
			result = 31 * result + mIndexedRewFiles.hashCode();
			result = 31 * result + mMDPFile.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
