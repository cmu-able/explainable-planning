package solver.prismconnector;

public class PrismConnectorSettings {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private boolean mUseExplicitModel;
	private String mModelOutputPath;
	private String mAdvOutputPath;
	private PrismConfiguration mPrismConfig;
	private PrismRewardType mPrismRewardType;

	public PrismConnectorSettings(boolean useExplicitModel, String modelOutputPath, String advOutputPath,
			PrismConfiguration prismConfig, PrismRewardType prismRewardType) {
		mUseExplicitModel = useExplicitModel;
		mModelOutputPath = modelOutputPath;
		mAdvOutputPath = advOutputPath;
		mPrismConfig = prismConfig;
		mPrismRewardType = prismRewardType;
	}

	/**
	 * 
	 * @return Whether to use PRISM explicit model (.sta, .tra, .lab, .srew/.trew files) to perform numerical query
	 *         (e.g., for QA values)
	 */
	public boolean useExplicitModel() {
		return mUseExplicitModel;
	}

	public String getModelOutputPath() {
		return mModelOutputPath;
	}

	public String getAdversaryOutputPath() {
		return mAdvOutputPath;
	}

	public PrismConfiguration getPrismConfiguration() {
		return mPrismConfig;
	}

	public PrismRewardType getPrismRewardType() {
		return mPrismRewardType;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof PrismConnectorSettings)) {
			return false;
		}
		PrismConnectorSettings settings = (PrismConnectorSettings) obj;
		return settings.mUseExplicitModel == mUseExplicitModel && settings.mModelOutputPath.equals(mModelOutputPath)
				&& settings.mAdvOutputPath.equals(mAdvOutputPath) && settings.mPrismConfig.equals(mPrismConfig)
				&& settings.mPrismRewardType == mPrismRewardType;
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + Boolean.hashCode(mUseExplicitModel);
			result = 31 * result + mModelOutputPath.hashCode();
			result = 31 * result + mAdvOutputPath.hashCode();
			result = 31 * result + mPrismConfig.hashCode();
			result = 31 * result + mPrismRewardType.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
