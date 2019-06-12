package explanation.verbalization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import language.domain.metrics.IQFunction;

public class VerbalizerSettings {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private QADecimalFormatter mDecimalFormatter = new QADecimalFormatter();
	private boolean mDescribeCosts = true;
	private List<String> mOrderedQFunctionNames = new ArrayList<>();
	private Map<String, List<String>> mOrderedEventNames = new HashMap<>();

	public VerbalizerSettings() {
		// Default Verbalizer settings:
		// * describeCosts <- true
		// * decimal formatter <- empty -- no formatting
		// * order of QAs <- empty -- no fixed order
		// * order of events of non-standard QA <- empty -- no fixed order
	}

	public void setQADecimalFormatter(QADecimalFormatter decimalFormatter) {
		mDecimalFormatter = decimalFormatter;
	}

	public QADecimalFormatter getQADecimalFormatter() {
		return mDecimalFormatter;
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

	public void appendQFunctionName(String qFunctionName) {
		mOrderedQFunctionNames.add(qFunctionName);
	}

	public List<String> getOrderedQFunctionNames() {
		return mOrderedQFunctionNames;
	}

	public void appendEvent(String nonStdQFunctionName, String eventName) {
		if (!mOrderedEventNames.containsKey(nonStdQFunctionName)) {
			mOrderedEventNames.put(nonStdQFunctionName, new ArrayList<>());
		}
		List<String> orderedEventNames = mOrderedEventNames.get(nonStdQFunctionName);
		orderedEventNames.add(eventName);
	}

	public List<String> getOrderedEventNames(String nonStdQFunctionName) {
		return mOrderedEventNames.get(nonStdQFunctionName);
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
		return settings.mDecimalFormatter.equals(mDecimalFormatter) && settings.mDescribeCosts == mDescribeCosts
				&& settings.mOrderedQFunctionNames.equals(mOrderedQFunctionNames)
				&& settings.mOrderedEventNames.equals(mOrderedEventNames);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mDecimalFormatter.hashCode();
			result = 31 * result + Boolean.hashCode(mDescribeCosts);
			result = 31 * result + mOrderedQFunctionNames.hashCode();
			result = 31 * result + mOrderedEventNames.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
