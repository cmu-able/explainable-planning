package mobilerobot.study.utilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HTMLTableSettings {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private List<String> mOrderedQANames = new ArrayList<>();
	private Map<String, List<String>> mOrderedEventNamesMap = new HashMap<>();

	public void appendQAName(String qaName) {
		mOrderedQANames.add(qaName);
	}

	public void appendEventName(String eventBasedQAName, String eventName) {
		if (!mOrderedEventNamesMap.containsKey(eventBasedQAName)) {
			mOrderedEventNamesMap.put(eventBasedQAName, new ArrayList<>());
		}

		List<String> orderedEventNames = mOrderedEventNamesMap.get(eventBasedQAName);
		orderedEventNames.add(eventName);
	}

	public List<String> getOrderedQANames() {
		return mOrderedQANames;
	}

	public boolean hasEventBasedQA() {
		return !mOrderedEventNamesMap.isEmpty();
	}

	public boolean isEventBasedQA(String qaName) {
		return mOrderedEventNamesMap.containsKey(qaName);
	}

	public List<String> getOrderedEventNames(String eventBasedQAName) {
		return mOrderedEventNamesMap.get(eventBasedQAName);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof HTMLTableSettings)) {
			return false;
		}
		HTMLTableSettings settings = (HTMLTableSettings) obj;
		return settings.mOrderedQANames.equals(mOrderedQANames)
				&& settings.mOrderedEventNamesMap.equals(mOrderedEventNamesMap);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mOrderedQANames.hashCode();
			result = 31 * result + mOrderedEventNamesMap.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
