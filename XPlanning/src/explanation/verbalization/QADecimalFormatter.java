package explanation.verbalization;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import language.domain.metrics.IQFunction;

public class QADecimalFormatter {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Map<IQFunction<?, ?>, DecimalFormat> mDecimalFormats = new HashMap<>();

	public void putDecimalFormat(IQFunction<?, ?> qFunction, String decimalFormatPattern) {
		DecimalFormat df = new DecimalFormat(decimalFormatPattern);
		df.setRoundingMode(RoundingMode.HALF_UP);
		mDecimalFormats.put(qFunction, df);
	}

	public String formatQAValue(IQFunction<?, ?> qFunction, double qaValue) {
		if (!mDecimalFormats.containsKey(qFunction)) {
			return Double.toString(qaValue);
		}
		DecimalFormat df = mDecimalFormats.get(qFunction);
		return df.format(qaValue);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof QADecimalFormatter)) {
			return false;
		}
		QADecimalFormatter formatter = (QADecimalFormatter) obj;
		return formatter.mDecimalFormats.equals(mDecimalFormats);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mDecimalFormats.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
