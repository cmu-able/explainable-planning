package explanation.verbalization;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import language.domain.metrics.IQFunction;

public class VerbalizerSettings {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Map<IQFunction<?, ?>, DecimalFormat> mDecimalFormats = new HashMap<>();
	private boolean mDescribeCosts = true;

	public VerbalizerSettings() {
		// Default Verbalizer settings; describeCosts <- true
	}

	public void putDecimalFormat(IQFunction<?, ?> qFunction, String decimalFormatPattern) {
		DecimalFormat df = new DecimalFormat(decimalFormatPattern);
		df.setRoundingMode(RoundingMode.HALF_UP);
		mDecimalFormats.put(qFunction, df);
	}

	public String formatQAValue(IQFunction<?, ?> qFunction, double qaValue) {
		DecimalFormat df = mDecimalFormats.get(qFunction);
		return df.format(qaValue);
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
		return settings.mDecimalFormats.equals(mDecimalFormats) && settings.mDescribeCosts == mDescribeCosts;
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mDecimalFormats.hashCode();
			result = 31 * result + Boolean.hashCode(mDescribeCosts);
			hashCode = result;
		}
		return hashCode;
	}

}
