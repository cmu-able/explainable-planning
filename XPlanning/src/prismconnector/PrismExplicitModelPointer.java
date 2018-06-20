package prismconnector;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class PrismExplicitModelPointer {

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

	public PrismExplicitModelPointer(String modelPath, String staFilename, String traFilename, String labFilename,
			String srewFilename) {
		mModelPath = modelPath;
		mStaFile = new File(modelPath, staFilename);
		mTraFile = new File(modelPath, traFilename);
		mLabFile = new File(modelPath, labFilename);
		mSrewFile = new File(modelPath, srewFilename);
		mIndexedSrewFiles = getSortedStateRewardsFiles(srewFilename);
	}

	private List<File> getSortedStateRewardsFiles(String srewFilename) {
		int extensionIndex = srewFilename.indexOf(".srew");
		String srewName = srewFilename.substring(0, extensionIndex);

		File modelDir = new File(mModelPath);
		File[] srewFiles = modelDir.listFiles(
				(dir, name) -> name.toLowerCase().startsWith(srewName) && name.toLowerCase().endsWith(".srew"));

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
