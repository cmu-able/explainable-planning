package solver.prismconnector;

import language.exceptions.VarNotFoundException;
import language.mdp.IStateVarTuple;
import language.qfactors.IStateVarBoolean;
import language.qfactors.IStateVarInt;
import language.qfactors.IStateVarValue;
import language.qfactors.StateVar;

public class PrismTranslatorUtils {
	static final String INDENT = "  ";

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

	/**
	 * Build an expression from a given predicate.
	 * 
	 * @param predicate
	 * @return {varName_1}={value OR encoded int value} & ... & {varName_m}={value OR encoded int value}
	 * @throws VarNotFoundException
	 */
	public static String buildExpression(IStateVarTuple predicate, ValueEncodingScheme encodings)
			throws VarNotFoundException {
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for (StateVar<IStateVarValue> var : predicate) {
			String varName = var.getName();
			IStateVarValue value = var.getValue();

			if (!first) {
				builder.append(" & ");
			} else {
				first = false;
			}
			builder.append(varName);
			builder.append("=");

			if (value instanceof IStateVarInt || value instanceof IStateVarBoolean) {
				builder.append(value);
			} else {
				Integer encodedValue = encodings.getEncodedIntValue(var.getDefinition(), value);
				builder.append(encodedValue);
			}
		}
		return builder.toString();
	}
}
