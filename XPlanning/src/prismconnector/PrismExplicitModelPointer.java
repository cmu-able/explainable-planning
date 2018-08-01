package prismconnector;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class PrismExplicitModelPointer {

	private static final String STA_EXTENSION = ".sta";
	private static final String TRA_EXTENSION = ".tra";
	private static final String LAB_EXTENSION = ".lab";
	private static final String SREW_EXTENSION = ".srew";

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private String mModelPath;
	private File mStaFile;
	private File mTraFile;
	private File mLabFile;
	private File mSrewFile;
	private List<File> mIndexedSrewFiles;

	public PrismExplicitModelPointer(String modelPath, String filenamePrefix) {
		mModelPath = modelPath;
		mSrewFile = new File(modelPath, filenamePrefix + STA_EXTENSION);
		mTraFile = new File(modelPath, filenamePrefix + TRA_EXTENSION);
		mLabFile = new File(modelPath, filenamePrefix + LAB_EXTENSION);
		mSrewFile = new File(modelPath, filenamePrefix + SREW_EXTENSION);
		mIndexedSrewFiles = getSortedStateRewardsFiles(filenamePrefix);
	}

	private List<File> getSortedStateRewardsFiles(String filenamePrefix) {
		File modelDir = new File(mModelPath);
		File[] srewFiles = modelDir.listFiles((dir, name) -> name.toLowerCase().startsWith(filenamePrefix)
				&& name.toLowerCase().endsWith(SREW_EXTENSION));

		// Sort .srew files lexicographically based on their abstract pathnames
		// {name}1.srew, {name}2.srew, {name}3.srew, ...
		Arrays.sort(srewFiles);
		return Arrays.asList(srewFiles);
	}

	public String getExplicitModelPath() {
		return mModelPath;
	}

	public File getStatesFile() {
		return mStaFile;
	}

	public File getTransitionsFile() {
		return mTraFile;
	}

	public File getLabelsFile() {
		return mLabFile;
	}

	public File getStateRewardsFile() {
		return mSrewFile;
	}

	public List<File> getIndexedStateRewardsFiles() {
		return mIndexedSrewFiles;
	}

	public File getIndexedStateRewardsFile(int rewardStructIndex) {
		return mIndexedSrewFiles.get(rewardStructIndex - 1);
	}

	public int getNumRewardStructs() {
		return mIndexedSrewFiles.size();
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
		return pointer.mModelPath.equals(mModelPath) && pointer.mStaFile.equals(mStaFile)
				&& pointer.mTraFile.equals(mTraFile) && pointer.mLabFile.equals(mLabFile)
				&& pointer.mSrewFile.equals(mSrewFile) && pointer.mIndexedSrewFiles.equals(mIndexedSrewFiles);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mModelPath.hashCode();
			result = 31 * result + mStaFile.hashCode();
			result = 31 * result + mTraFile.hashCode();
			result = 31 * result + mLabFile.hashCode();
			result = 31 * result + mSrewFile.hashCode();
			result = 31 * result + mIndexedSrewFiles.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
