package explanation.verbalization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import language.domain.metrics.IEvent;
import language.domain.metrics.IQFunction;
import language.domain.metrics.ITransitionStructure;
import language.domain.metrics.NonStandardMetricQFunction;
import language.domain.models.IAction;
import language.mdp.QSpace;

public class VerbalizerSettings {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private QADecimalFormatter mDecimalFormatter = new QADecimalFormatter();
	private boolean mRelContrast = true;
	private boolean mDescribeCosts = true;
	private List<String> mOrderedQFunctionNames = new ArrayList<>();
	private Map<String, List<String>> mOrderedEventNames = new HashMap<>();

	public VerbalizerSettings() {
		// Default Verbalizer settings:
		// * relContrast <- true
		// * describeCosts <- true
		// * decimal formatter <- empty -- no formatting
		// * order of QAs <- empty -- no predefined order
		// * order of events of non-standard QA <- empty -- no predefined order
	}

	public void setQADecimalFormatter(QADecimalFormatter decimalFormatter) {
		mDecimalFormatter = decimalFormatter;
	}

	public QADecimalFormatter getQADecimalFormatter() {
		return mDecimalFormatter;
	}

	public String formatQAValue(IQFunction<?, ?> qFunction, double qaValue) {
		return mDecimalFormatter.formatQAValue(qFunction, qaValue);
	}

	public void setRelativeContrast(boolean diffContrast) {
		mRelContrast = diffContrast;
	}

	public boolean useRelativeContrast() {
		return mRelContrast;
	}

	public void setDescribeCosts(boolean describeCosts) {
		mDescribeCosts = describeCosts;
	}

	public boolean describeCosts() {
		return mDescribeCosts;
	}

	public void appendQFunctionName(String qFunctionName) {
		mOrderedQFunctionNames.add(qFunctionName);
	}

	public List<IQFunction<IAction, ITransitionStructure<IAction>>> getOrderedQFunctions(QSpace qSpace) {
		List<IQFunction<IAction, ITransitionStructure<IAction>>> orderedQFunctions = new ArrayList<>();
		if (!mOrderedEventNames.isEmpty()) {
			// There is a predefined ordering of QAs
			for (String qFunctionName : mOrderedQFunctionNames) {
				IQFunction<IAction, ITransitionStructure<IAction>> qFunction = qSpace.getQFunction(IQFunction.class,
						qFunctionName);
				orderedQFunctions.add(qFunction);
			}
		} else {
			// There is no predefined ordering of QAs
			// Use the ordering of QAs from QSpace
			for (IQFunction<IAction, ITransitionStructure<IAction>> qFunction : qSpace) {
				orderedQFunctions.add(qFunction);
			}
		}
		return orderedQFunctions;
	}

	public void appendEventName(String nonStdQFunctionName, String eventName) {
		if (!mOrderedEventNames.containsKey(nonStdQFunctionName)) {
			mOrderedEventNames.put(nonStdQFunctionName, new ArrayList<>());
		}
		List<String> orderedEventNames = mOrderedEventNames.get(nonStdQFunctionName);
		orderedEventNames.add(eventName);
	}

	public <E extends IEvent<?, ?>> List<E> getOrderedEvents(NonStandardMetricQFunction<?, ?, E> nonStdQFunction) {
		List<E> orderedEvents = new ArrayList<>();
		if (mOrderedEventNames.containsKey(nonStdQFunction.getName())) {
			// There is a predefined ordering of events of this non-standard QA
			for (String eventName : mOrderedEventNames.get(nonStdQFunction.getName())) {
				E event = nonStdQFunction.getEventBasedMetric().getEvent(eventName);
				orderedEvents.add(event);
			}
		} else {
			// There is no predefined ordering of events of this non-standard QA
			// Use the ordering of events from the event-based metric
			for (E event : nonStdQFunction.getEventBasedMetric().getEvents()) {
				orderedEvents.add(event);
			}
		}
		return orderedEvents;
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
		return settings.mDecimalFormatter.equals(mDecimalFormatter) && settings.mRelContrast == mRelContrast
				&& settings.mDescribeCosts == mDescribeCosts
				&& settings.mOrderedQFunctionNames.equals(mOrderedQFunctionNames)
				&& settings.mOrderedEventNames.equals(mOrderedEventNames);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mDecimalFormatter.hashCode();
			result = 31 * result + Boolean.hashCode(mRelContrast);
			result = 31 * result + Boolean.hashCode(mDescribeCosts);
			result = 31 * result + mOrderedQFunctionNames.hashCode();
			result = 31 * result + mOrderedEventNames.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
