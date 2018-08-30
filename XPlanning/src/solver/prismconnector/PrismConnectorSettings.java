package solver.prismconnector;

public class PrismConnectorSettings {

	private boolean mUseExplicitModel;
	private String mModelOutputPath;
	private String mAdvOutputPath;
	private PrismConfiguration mPrismConfig;

	public PrismConnectorSettings(boolean useExplicitModel, String modelOutputPath, String advOutputPath,
			PrismConfiguration prismConfig) {
		mUseExplicitModel = useExplicitModel;
		mModelOutputPath = modelOutputPath;
		mAdvOutputPath = advOutputPath;
		mPrismConfig = prismConfig;
	}

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
}
