package prismconnector;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PrismExplicitModelPointer {

	private static final String STA_EXTENSION = ".sta";
	private static final String TRA_EXTENSION = ".tra";
	private static final String LAB_EXTENSION = ".lab";
	private static final String SREW_EXTENSION = ".srew";
	private static final String PROD_STA_FILENAME = "prod.sta";

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private File mModelDir;
	private File mStaFile;
	private File mProdStaFile;
	private File mTraFile;
	private File mLabFile;
	private File mSrewFile;
	private List<File> mIndexedSrewFiles = new ArrayList<>();

	/**
	 * Use this constructor if the PRISM explicit model does not exist yet at modelPath. Create a modelPath directory if
	 * it doesn't already exist.
	 * 
	 * @param modelPath
	 * @param filenamePrefix
	 */
	public PrismExplicitModelPointer(String modelPath, String filenamePrefix) {
		mModelDir = new File(modelPath);
		if (!mModelDir.exists()) {
			mModelDir.mkdirs();
		}
		mStaFile = new File(modelPath, filenamePrefix + STA_EXTENSION);
		mProdStaFile = new File(modelPath, PROD_STA_FILENAME);
		mTraFile = new File(modelPath, filenamePrefix + TRA_EXTENSION);
		mLabFile = new File(modelPath, filenamePrefix + LAB_EXTENSION);
		mSrewFile = new File(modelPath, filenamePrefix + SREW_EXTENSION);
		// mIndexedSrewFiles will be created once the PRISM explicit model is created
	}

	/**
	 * Use this constructor if the PRISM explicit model already exists at modelPath.
	 * 
	 * @param modelPath
	 */
	public PrismExplicitModelPointer(String modelPath) {
		mModelDir = new File(modelPath);
		File[] prismFiles = mModelDir.listFiles((dir, name) -> name.toLowerCase().endsWith(STA_EXTENSION)
				|| name.toLowerCase().endsWith(TRA_EXTENSION) || name.toLowerCase().endsWith(LAB_EXTENSION));

		for (File prismFile : prismFiles) {
			String filename = prismFile.getName().toLowerCase();
			if (filename.equals(PROD_STA_FILENAME)) {
				mProdStaFile = prismFile;
			} else if (filename.endsWith(STA_EXTENSION)) {
				mStaFile = prismFile;
			} else if (filename.endsWith(TRA_EXTENSION)) {
				mTraFile = prismFile;
			} else if (filename.endsWith(LAB_EXTENSION)) {
				mLabFile = prismFile;
			} else if (filename.endsWith(SREW_EXTENSION)) {
				mIndexedSrewFiles.add(prismFile);
			}
		}
		mIndexedSrewFiles.sort((file1, file2) -> file1.compareTo(file2));

		if (mIndexedSrewFiles.size() > 1) {
			File srew1File = mIndexedSrewFiles.get(0);
			String srew1Filename = srew1File.getName(); // <name>1.srew
			String srewFilenamePrefix = srew1Filename.substring(0, srew1Filename.indexOf(SREW_EXTENSION) - 1); // <name>
			mSrewFile = new File(modelPath, srewFilenamePrefix + SREW_EXTENSION); // <name>.srew
		} else {
			mSrewFile = mIndexedSrewFiles.get(0);
		}
	}

	private List<File> getSortedStateRewardsFiles() {
		File[] srewFiles = mModelDir.listFiles((dir, name) -> name.toLowerCase().endsWith(SREW_EXTENSION));

		// Sort .srew files lexicographically based on their abstract pathnames
		// {name}1.srew, {name}2.srew, {name}3.srew, ...
		Arrays.sort(srewFiles, (file1, file2) -> file1.compareTo(file2));
		return Arrays.asList(srewFiles);
	}

	public File getExplicitModelDirectory() {
		return mModelDir;
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

	public File getLabelsFile() {
		return mLabFile;
	}

	public File getStateRewardsFile() {
		return mSrewFile;
	}

	public File getIndexedStateRewardsFile(int rewardStructIndex) {
		if (mIndexedSrewFiles.isEmpty()) {
			mIndexedSrewFiles.addAll(getSortedStateRewardsFiles());
		}
		return mIndexedSrewFiles.get(rewardStructIndex - 1);
	}

	public int getNumRewardStructs() {
		if (mIndexedSrewFiles.isEmpty()) {
			mIndexedSrewFiles.addAll(getSortedStateRewardsFiles());
		}
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
		return pointer.mModelDir.equals(mModelDir) && pointer.mStaFile.equals(mStaFile)
				&& pointer.mProdStaFile.equals(mProdStaFile) && pointer.mTraFile.equals(mTraFile)
				&& pointer.mLabFile.equals(mLabFile) && pointer.mSrewFile.equals(mSrewFile)
				&& pointer.mIndexedSrewFiles.equals(mIndexedSrewFiles);
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
			result = 31 * result + mLabFile.hashCode();
			result = 31 * result + mSrewFile.hashCode();
			result = 31 * result + mIndexedSrewFiles.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
