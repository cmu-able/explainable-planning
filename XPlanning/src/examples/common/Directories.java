package examples.common;

import java.nio.file.Path;

public class Directories {

	public static final String PRISM_MODELS_OUTPUT_PATH = "/Users/rsukkerd/Projects/explainable-planning/XPlanning/tmpdata/prism/models";
	public static final String PRISM_ADVS_OUTPUT_PATH = "/Users/rsukkerd/Projects/explainable-planning/XPlanning/tmpdata/prism/advs";
	public static final String POLICIES_OUTPUT_PATH = "/Users/rsukkerd/Projects/explainable-planning/XPlanning/tmpdata/policies";
	public static final String EXPLANATIONS_OUTPUT_PATH = "/Users/rsukkerd/Projects/explainable-planning/XPlanning/tmpdata/explanations";

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Path mPoliciesOutputPath;
	private Path mExplanationOutputPath;
	private Path mPrismOutputModelsPath;
	private Path mPrismOutputAdvsPath;

	public Directories(Path policiesOutputPath, Path explanationOutputPath, Path prismOutputPath) {
		mPoliciesOutputPath = policiesOutputPath;
		mExplanationOutputPath = explanationOutputPath;
		mPrismOutputModelsPath = prismOutputPath.resolve("models");
		mPrismOutputAdvsPath = prismOutputPath.resolve("advs");
	}

	public Path getPoliciesOutputPath() {
		return mPoliciesOutputPath;
	}

	public Path getExplanationOutputPath() {
		return mExplanationOutputPath;
	}

	public Path getPrismModelsOutputPath() {
		return mPrismOutputModelsPath;
	}

	public Path getPrismAdvsOutputPath() {
		return mPrismOutputAdvsPath;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof Directories)) {
			return false;
		}
		Directories dirs = (Directories) obj;
		return dirs.mPoliciesOutputPath.equals(mPoliciesOutputPath)
				&& dirs.mExplanationOutputPath.equals(mExplanationOutputPath)
				&& dirs.mPrismOutputModelsPath.equals(mPrismOutputModelsPath)
				&& dirs.mPrismOutputAdvsPath.equals(mPrismOutputAdvsPath);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mPoliciesOutputPath.hashCode();
			result = 31 * result + mExplanationOutputPath.hashCode();
			result = 31 * result + mPrismOutputModelsPath.hashCode();
			result = 31 * result + mPrismOutputAdvsPath.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
