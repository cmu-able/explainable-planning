package solver.prismconnector;

public class PrismConnectorSettings {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private String mModelOutputPath;
	private String mAdvOutputPath;
	private PrismConfiguration mPrismConfig;

	public PrismConnectorSettings(String modelOutputPath, String advOutputPath, PrismConfiguration prismConfig) {
		mModelOutputPath = modelOutputPath;
		mAdvOutputPath = advOutputPath;
		mPrismConfig = prismConfig;
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

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof PrismConnectorSettings)) {
			return false;
		}
		PrismConnectorSettings settings = (PrismConnectorSettings) obj;
		return settings.mModelOutputPath.equals(mModelOutputPath) && settings.mAdvOutputPath.equals(mAdvOutputPath)
				&& settings.mPrismConfig.equals(mPrismConfig);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mModelOutputPath.hashCode();
			result = 31 * result + mAdvOutputPath.hashCode();
			result = 31 * result + mPrismConfig.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
