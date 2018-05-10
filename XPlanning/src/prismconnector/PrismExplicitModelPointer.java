package prismconnector;

import java.io.File;
import java.util.ArrayList;
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
	private List<File> mSrewFiles = new ArrayList<>();

	public PrismExplicitModelPointer(String modelPath, String staFilename, String traFilename, String labFilename,
			String srewFilename, int numRewardStructs) {
		mModelPath = modelPath;
		mStaFile = new File(modelPath, staFilename);
		mTraFile = new File(modelPath, traFilename);
		mLabFile = new File(modelPath, labFilename);
		for (int i = 1; i <= numRewardStructs; i++) {
			int extensionIndex = srewFilename.indexOf(".srew");
			String srewName = srewFilename.substring(0, extensionIndex);
			String indexedSrewFilename = srewName + i + ".srew";
			File srewFile = new File(modelPath, indexedSrewFilename);
			mSrewFiles.add(srewFile);
		}

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

	public List<File> getStateRewardsFiles() {
		return mSrewFiles;
	}

	public File getStateRewardsFile(int rewardStructIndex) {
		return mSrewFiles.get(rewardStructIndex - 1);
	}

	public int getNumRewardStructs() {
		return mSrewFiles.size();
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
				&& pointer.mSrewFiles.equals(mSrewFiles);
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
			result = 31 * result + mSrewFiles.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
