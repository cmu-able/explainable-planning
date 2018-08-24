package solver.prismconnector;

public class PrismTranslatorUtils {

	private static final String[] REPLACED_CHARS = { ".", "\\(", "\\)", "-" };
	private static final String[] REPLACING_WORDS = { "_DOT_", "_LP_", "_RP_", "_DASH_" };

	private PrismTranslatorUtils() {
		throw new IllegalStateException("Utility class");
	}

	public static String sanitizeNameString(String name) {
		String sanitizedName = name;
		for (int i = 0; i < REPLACED_CHARS.length; i++) {
			String replacedCharRegex = "[" + REPLACED_CHARS[i] + "]";
			sanitizedName = sanitizedName.replaceAll(replacedCharRegex, REPLACING_WORDS[i]);
		}
		return sanitizedName;
	}

	public static String desanitizeNameString(String sanitizedName) {
		String desanitizedName = sanitizedName;
		for (int i = 0; i < REPLACING_WORDS.length; i++) {
			desanitizedName = desanitizedName.replaceAll(REPLACING_WORDS[i], REPLACED_CHARS[i]);
		}
		return desanitizedName;
	}
}
