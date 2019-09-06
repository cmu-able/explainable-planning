package mobilerobot.study.utilities;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import mobilerobot.utilities.FileIOUtils;

public class ExplanationUtils {

	private static final String EVENT_BASED_QA_VALUE_REGEX = "(: it \\b(?:would|will)\\b [a-z]+ ([^\\.]+))\\."; // :it will [verb] [breakdown]
	private static final String ZERO_UNIT_REGEX = "\\s0 [a-z]+"; // 0 [unit(s)]
	private static final String ZERO_PENALTY_REGEX = "(0-penalty)";

	private ExplanationUtils() {
		throw new IllegalStateException("Utility class");
	}

	public static File getExplanationJSONFile(File explanationDir) {
		// There is only 1 [explanation name].json per explanation dir
		return FileIOUtils.listFilesWithContainFilter(explanationDir, "explanation", ".json")[0];
	}

	public static String[] getExplanationParagraphs(File explanationDir) throws IOException, ParseException {
		JSONObject explanationJsonObj = getExplanationJSONObject(explanationDir);
		String explanationText = (String) explanationJsonObj.get("Explanation");

		// Each paragraph in the explanation text corresponds to a policy (either a solution policy or an alternative policy)
		return explanationText.split("\n\n");
	}

	public static JSONObject getPolicyQAValuesJSONObject(File explanationDir, String policyJsonFilename)
			throws IOException, ParseException {
		JSONObject explanationJsonObj = getExplanationJSONObject(explanationDir);
		return (JSONObject) explanationJsonObj.get(policyJsonFilename);
	}

	public static String removeZeroValueComponents(String policyExplanation) {
		Pattern pattern = Pattern.compile(EVENT_BASED_QA_VALUE_REGEX);
		Matcher matcher = pattern.matcher(policyExplanation);

		if (matcher.find()) {
			String eventBasedQAValueBreakdown = matcher.group(2);
			String[] components = eventBasedQAValueBreakdown.split(", ");
			components[components.length - 1] = components[components.length - 1].replace("and ", "");
			List<String> nonZeroComponents = new ArrayList<>();

			Pattern zeroUnitPattern = Pattern.compile(ZERO_UNIT_REGEX);
			Pattern zeroPenaltyPattern = Pattern.compile(ZERO_PENALTY_REGEX);

			for (String component : components) {
				Matcher zeroUnitMatcher = zeroUnitPattern.matcher(component);
				Matcher zeroPenaltyMatcher = zeroPenaltyPattern.matcher(component);

				if (!zeroUnitMatcher.find() && !zeroPenaltyMatcher.find()) {
					nonZeroComponents.add(component);
				}
			}

			if (nonZeroComponents.isEmpty()) {
				String breakdownExplanation = matcher.group(1);
				return policyExplanation.replace(breakdownExplanation, "");
			}

			String nonZeroConcat;
			if (nonZeroComponents.size() > 2) {
				nonZeroConcat = String.join(", ", nonZeroComponents.subList(0, nonZeroComponents.size() - 1));
				nonZeroConcat += ", and " + nonZeroComponents.get(nonZeroComponents.size() - 1);
			} else {
				nonZeroConcat = String.join(" and ", nonZeroComponents);
			}
			return policyExplanation.replace(eventBasedQAValueBreakdown, nonZeroConcat);
		} else {
			return policyExplanation;
		}
	}

	private static JSONObject getExplanationJSONObject(File explanationDir) throws IOException, ParseException {
		File explanationJsonFile = getExplanationJSONFile(explanationDir);
		return FileIOUtils.readJSONObjectFromFile(explanationJsonFile);
	}
}
