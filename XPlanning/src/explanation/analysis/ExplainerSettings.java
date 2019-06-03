package explanation.analysis;

import solver.prismconnector.PrismConnectorSettings;

public class ExplainerSettings {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private PrismConnectorSettings mPrismConnectorSettings;
	private DifferenceScaler mDiffScaler;

	public ExplainerSettings(PrismConnectorSettings prismConnectorSettings) {
		mPrismConnectorSettings = prismConnectorSettings;
	}

	public PrismConnectorSettings getPrismConnectorSettings() {
		return mPrismConnectorSettings;
	}

	public void setDifferenceScaler(DifferenceScaler diffScaler) {
		mDiffScaler = diffScaler;
	}

	public DifferenceScaler getDifferenceScaler() {
		return mDiffScaler;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof ExplainerSettings)) {
			return false;
		}
		ExplainerSettings settings = (ExplainerSettings) obj;
		return settings.mPrismConnectorSettings.equals(mPrismConnectorSettings) && (settings.mDiffScaler == mDiffScaler
				|| settings.mDiffScaler != null && settings.mDiffScaler.equals(mDiffScaler));
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mPrismConnectorSettings.hashCode();
			result = 31 * result + (mDiffScaler == null ? 0 : mDiffScaler.hashCode());
			hashCode = result;
		}
		return hashCode;
	}
}
