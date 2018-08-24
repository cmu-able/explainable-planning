package solver.prismconnector;

public class PrismConnectorSettings {

	private boolean mUseExplicitModel;
	private String mOutputPath;
	private PrismConfiguration mPrismConfig;

	public PrismConnectorSettings(boolean useExplicitModel, String outputPath, PrismConfiguration prismConfig) {
		mUseExplicitModel = useExplicitModel;
		mOutputPath = outputPath;
		mPrismConfig = prismConfig;
	}

	public boolean useExplicitModel() {
		return mUseExplicitModel;
	}

	public String getOutputPath() {
		return mOutputPath;
	}

	public PrismConfiguration getPrismConfiguration() {
		return mPrismConfig;
	}
}
