package explanation.verbalization;

public class VerbalizerSettings {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private boolean mDescribeCosts = true;

	public VerbalizerSettings() {
		// Default Verbalizer settings
	}

	public void setDescribeCosts(boolean describeCosts) {
		mDescribeCosts = describeCosts;
	}

	public boolean getDescribeCosts() {
		return mDescribeCosts;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof VerbalizerSettings)) {
			return false;
		}
		VerbalizerSettings settings = (VerbalizerSettings) obj;
		return settings.mDescribeCosts == mDescribeCosts;
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + Boolean.hashCode(mDescribeCosts);
			hashCode = result;
		}
		return hashCode;
	}

}
