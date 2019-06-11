package explanation.verbalization;

import language.domain.metrics.IQFunction;

public class VerbalizerSettings {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private QADecimalFormatter mDecimalFormatter = new QADecimalFormatter();
	private boolean mDescribeCosts = true;

	public VerbalizerSettings() {
		// Default Verbalizer settings:
		// describeCosts <- true
		// decimal formatter <- empty -- no formatting
	}

	public void setQADecimalFormatter(QADecimalFormatter decimalFormatter) {
		mDecimalFormatter = decimalFormatter;
	}

	public void putDecimalFormat(IQFunction<?, ?> qFunction, String decimalFormatPattern) {
		mDecimalFormatter.putDecimalFormat(qFunction, decimalFormatPattern);
	}

	public String formatQAValue(IQFunction<?, ?> qFunction, double qaValue) {
		return mDecimalFormatter.formatQAValue(qFunction, qaValue);
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
		return settings.mDecimalFormatter.equals(mDecimalFormatter) && settings.mDescribeCosts == mDescribeCosts;
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mDecimalFormatter.hashCode();
			result = 31 * result + Boolean.hashCode(mDescribeCosts);
			hashCode = result;
		}
		return hashCode;
	}

}
