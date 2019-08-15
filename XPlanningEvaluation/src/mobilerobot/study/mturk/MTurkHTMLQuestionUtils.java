package mobilerobot.study.mturk;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

import org.jsoup.nodes.Element;

import mobilerobot.study.utilities.JSTimingUtils;

public class MTurkHTMLQuestionUtils {

	/**
	 * This is the format of QuestionIdentifier in QuestionFormAnswers MTurk data structure
	 */
	public static final String QUESTION_ID_FORMAT = "question%d-%s";

	/**
	 * Additional data type collected for each question
	 */
	private static final String ELAPSED_TIME = "elapsedTime";
	private static final String[] AUX_DATA_TYPES = { "ref", ELAPSED_TIME };

	/**
	 * MTurk parameter "assignmentId"
	 */
	private static final String ASSIGNMENT_ID = "assignmentId";

	private static final String W3_CONTAINER = "w3-container";
	private static final String W3_MARGIN = "w3-margin";

	private static final String CROWD_FORM = "crowd-form";
	private static final String SCRIPT = "script";

	private MTurkHTMLQuestionUtils() {
		throw new IllegalStateException("Utility class");
	}

	public static Element getCrowdHTMLScript() {
		Element crowdHTMLScript = new Element(SCRIPT);
		crowdHTMLScript.attr("src", "https://assets.crowd.aws/crowd-html-elements.js");
		return crowdHTMLScript;
	}

	public static Element createSubmittableCrowdFormContainer(String questionDocName, Element[] inputUIElements,
			int numQuestions, String[] fillableDataTypes) {
		Element container = createCrowdFormContainerWithoutButton(questionDocName, inputUIElements);
		Element crowdForm = container.selectFirst(CROWD_FORM);

		// Hidden inputs in the submittable <crowd-form>, for each question:
		// - question[i]-answer
		// - question[i]-justification
		// - question[i]-confidence
		// - question[i]-ref: additional data type
		// - question[i]-elapsedTime: additional data type
		//
		// All hidden inputs have empty values initially, but will be filled in with values from localStorage once submit.
		String[] allHiddenInputDataTypes = getAllDataTypes(fillableDataTypes);

		for (int i = 0; i < numQuestions; i++) {
			for (String dataType : allHiddenInputDataTypes) {
				String hiddenInputName = String.format(QUESTION_ID_FORMAT, i, dataType);
				addHiddenInputToForm(hiddenInputName, null, crowdForm);
			}
		}

		// Crowd submit button
		Element crowdSubmitButtonContainer = createCrowdSubmitButtonContainer();

		crowdForm.appendChild(crowdSubmitButtonContainer);
		return container;
	}

	public static Element createSubmittableForm(String externalSubmitUrl, int numQuestions,
			String[] fillableDataTypes) {
		Element form = new Element("form");
		form.attr("action", externalSubmitUrl);
		form.attr("method", "POST");
		form.attr("id", "mturk-form");
		form.attr("name", "mturk-form");
		form.attr("style", "display:none");

		// Field assignmentId will be processed by MTurk
		addHiddenInputToForm(ASSIGNMENT_ID, null, form);

		// Hidden inputs in the submittable form:
		// for each question:
		// - question[i]-answer
		// - question[i]-justification
		// - question[i]-confidence
		// - question[i]-ref: additional data type
		// - question[i]-elapsedTime: additional data type
		//
		// All hidden inputs have empty values initially, but will be filled in with values from localStorage once submit.
		String[] allHiddenInputDataTypes = getAllDataTypes(fillableDataTypes);

		for (int i = 0; i < numQuestions; i++) {
			for (String dataType : allHiddenInputDataTypes) {
				String hiddenInputName = String.format(QUESTION_ID_FORMAT, i, dataType);
				addHiddenInputToForm(hiddenInputName, null, form);
			}
		}

		// No need for <input type="submit" ...> since this form is hidden
		return form;
	}

	private static void addHiddenInputToForm(String hiddenInputName, String value, Element form) {
		Element hiddenInput = new Element("input");
		hiddenInput.attr("type", "hidden");
		hiddenInput.attr("id", hiddenInputName);
		hiddenInput.attr("name", hiddenInputName);
		hiddenInput.attr("value", value == null ? "" : value);

		form.appendChild(hiddenInput);
	}

	public static Element getSubmittableCrowdFormOnSubmitScript(int questionIndex, int numQuestions,
			String[] fillableDataTypes, Map<String, String[]> fillableDataTypeOptions) {
		String timeMeasurementSnippet = JSTimingUtils
				.getTimeMeasurementSnippet(String.format(QUESTION_ID_FORMAT, questionIndex, ELAPSED_TIME));
		String crowdFormInputToLocalStorageFunction = getCrowdFormInputToLocalStorageFunction(questionIndex,
				fillableDataTypes, fillableDataTypeOptions);
		String localStorageToSubmittableCrowdFormFunction = getLocalStorageToSubmittableCrowdFormFunction(numQuestions,
				fillableDataTypes);
		String submitDataLogic = getSubmitDataLogic();

		Element script = new Element(SCRIPT);
		script.appendText(timeMeasurementSnippet);
		script.appendText(crowdFormInputToLocalStorageFunction);
		script.appendText(localStorageToSubmittableCrowdFormFunction);
		script.appendText(submitDataLogic);
		return script;
	}

	private static String[] getAllCrowdFormInputDataTypes(String[] fillableDataTypes) {
		// All input data types in crowd-form, for each question, include:
		// - fillable data types: answer, justification, and confidence
		// - hidden data type: ref
		String[] refDataType = Arrays.copyOfRange(AUX_DATA_TYPES, 0, 1);
		return Stream.concat(Arrays.stream(fillableDataTypes), Arrays.stream(refDataType)).toArray(String[]::new);
	}

	private static String[] getAllDataTypes(String[] fillableDataTypes) {
		// All data types that will be stored in localStorage and submit to MTurk, for each question, include:
		// - fillable data types: answer, justification, and confidence
		// - additional data type: ref, elapsedTime
		return Stream.concat(Arrays.stream(fillableDataTypes), Arrays.stream(AUX_DATA_TYPES)).toArray(String[]::new);
	}

	private static String getSubmitDataLogic() {
		StringBuilder builder = new StringBuilder();
		builder.append("document.querySelector(\"crowd-form\").onsubmit = function() {\n");
		builder.append("\trecordElapsedTimeToLocalStorage();\n");
		builder.append("\tcrowdFormInputToLocalStorage();\n");
		builder.append("\tlocalStorageToSubmittableCrowdForm();\n");
		// Clear localStorage after retrieving the data and filling them in the submittable <crowd-form>
		builder.append("\tlocalStorage.clear();\n");
		builder.append("}");
		return builder.toString();
	}

	private static String getCrowdFormInputToLocalStorageFunction(int questionIndex, String[] fillableDataTypes,
			Map<String, String[]> fillableDataTypeOptions) {
		String inputValueOptionFormat = "document.getElementById(\"%s\").checked";
		String inputValueTextFormat = "document.getElementById(\"%s\").value";

		String localStorageSetStrValueFormat = "localStorage.setItem(\"%s\", \"%s\")";
		String localStorageSetValueFormat = "localStorage.setItem(\"%s\", %s)";

		StringBuilder builder = new StringBuilder();
		builder.append("function crowdFormInputToLocalStorage() {\n");
		builder.append("\tif (typeof(Storage) !== \"undefined\") {\n");

		// All crowd-form input data types include fillable data types and "ref"
		// Input from fillable data types and "ref" will be stored in localStorage
		String[] allCrowdFormInputDataTypes = getAllCrowdFormInputDataTypes(fillableDataTypes);

		for (String dataType : allCrowdFormInputDataTypes) {
			String localStorageKey = String.format(QUESTION_ID_FORMAT, questionIndex, dataType);

			if (fillableDataTypeOptions.containsKey(dataType)) {
				// Multiple-choice input
				String[] options = fillableDataTypeOptions.get(dataType);

				boolean firstOption = true;

				for (String option : options) {
					if (firstOption) {
						builder.append("\t\tif (");
						firstOption = false;
					} else {
						builder.append("\t\telse if (");
					}

					String inputOptionId = dataType + "-" + option;

					builder.append(String.format(inputValueOptionFormat, inputOptionId));
					builder.append(") {\n");
					builder.append("\t\t\t");
					builder.append(String.format(localStorageSetStrValueFormat, localStorageKey, option));
					builder.append(";\n");
					builder.append("\t\t}\n");
				}
			} else {
				// Text input
				String inputValueText = String.format(inputValueTextFormat, dataType);

				builder.append("\t\t");
				builder.append(String.format(localStorageSetValueFormat, localStorageKey, inputValueText));
				builder.append(";\n");
			}

			builder.append("\n");
		}

		builder.append("\t}\n");
		builder.append("}");

		return builder.toString();
	}

	private static String getLocalStorageToSubmittableCrowdFormFunction(int numQuestions, String[] fillableDataTypes) {
		String hiddenInputValueFormat = "document.getElementById(\"%s\").value";
		String localStorageValueFormat = "localStorage.getItem(\"%s\")";

		StringBuilder builder = new StringBuilder();
		builder.append("function localStorageToSubmittableCrowdForm() {\n");
		builder.append("\tif (typeof(Storage) !== \"undefined\") {\n");

		// Copy the following data from localStorage to the submittable <crowd-form>, for each question:
		// - question[i]-answer
		// - question[i]-justification
		// - question[i]-confidence
		// - question[i]-ref: additional data type
		// - question[i]-elapsedTime: additional data type

		for (int i = 0; i < numQuestions; i++) {
			// Add "ref" and "elapsedTime" to data types to be filled in the submittable form's hidden inputs
			String[] allHiddenInputDataTypes = getAllDataTypes(fillableDataTypes);

			for (String dataType : allHiddenInputDataTypes) {
				String hiddenInputName = String.format(QUESTION_ID_FORMAT, i, dataType);

				builder.append("\t\t");
				builder.append(String.format(hiddenInputValueFormat, hiddenInputName));
				builder.append(" = ");
				builder.append(String.format(localStorageValueFormat, hiddenInputName));
				builder.append(";\n");
			}
		}

		// localStorage also contains MTurk parameters, but there is no need to copy those to the <crowd-form>

		builder.append("\t}\n");
		builder.append("}");
		return builder.toString();
	}

	public static Element createIntermediateCrowdFormContainer(String questionDocName, String nextUrl,
			Element... inputUIElements) {
		Element container = createCrowdFormContainerWithoutButton(questionDocName, inputUIElements);
		Element crowdForm = container.selectFirst(CROWD_FORM);

		// Next button
		Element nextButtonContainer = createNextButtonContainer(nextUrl);

		// Disable submit-action of intermediate <crowd-form>
		Element hiddenSubmitButton = createCrowdSubmitButton(true);

		crowdForm.appendChild(nextButtonContainer);
		crowdForm.appendChild(hiddenSubmitButton);
		return container;
	}

	public static Element getIntermediateCrowdFormNextOnClickScript(int questionIndex, String[] fillableDataTypes,
			Map<String, String[]> fillableDataTypeOptions) {
		String timeMeasurementSnippet = JSTimingUtils
				.getTimeMeasurementSnippet(String.format(QUESTION_ID_FORMAT, questionIndex, ELAPSED_TIME));
		String crowdFormInputToLocalStorageFunction = getCrowdFormInputToLocalStorageFunction(questionIndex,
				fillableDataTypes, fillableDataTypeOptions);

		String saveDataLogic = getSaveDataLogic();
		Element script = new Element(SCRIPT);
		script.appendText(timeMeasurementSnippet);
		script.appendText(crowdFormInputToLocalStorageFunction);
		script.appendText(saveDataLogic);
		return script;
	}

	private static String getSaveDataLogic() {
		StringBuilder builder = new StringBuilder();
		builder.append("document.getElementById(\"save-next\").onclick = function() {\n");
		builder.append("\trecordElapsedTimeToLocalStorage();\n");
		builder.append("\tcrowdFormInputToLocalStorage();\n");
		builder.append("\tvar assignmentId = localStorage.getItem(\"assignmentId\");\n");
		builder.append("\tvar hitId = localStorage.getItem(\"hitId\");\n");
		builder.append("\tvar turkSubmitTo = localStorage.getItem(\"turkSubmitTo\");\n");
		builder.append("\tvar workerId = localStorage.getItem(\"workerId\");\n");
		builder.append("\tvar params = \"?assignmentId=\" + assignmentId;\n");
		builder.append("\tparams += \"&hitId=\" + hitId;\n");
		builder.append("\tparams += \"&turkSubmitTo=\" + turkSubmitTo;\n");
		builder.append("\tparams += \"&workerId=\" + workerId;\n");
		builder.append("\tthis.href += params;\n");
		builder.append("}");
		return builder.toString();
	}

	private static Element createCrowdFormContainerWithoutButton(String questionDocName, Element... inputUIElements) {
		Element container = createBlankCrowdFormContainer(questionDocName);
		Element crowdForm = container.selectFirst(CROWD_FORM);
		for (Element inputUIElement : inputUIElements) {
			crowdForm.appendChild(inputUIElement);
		}
		return container;
	}

	private static Element createBlankCrowdFormContainer(String questionDocName) {
		Element container = createBlankContainerWithMargin();
		container.addClass("w3-sand");
		container.addClass("w3-card");

		Element crowdForm = new Element(CROWD_FORM);
		// This hidden input is attached to each crowd-form (either intermediate and submittable crowd-form)
		// <input type="hidden" id="ref" name="ref" value=[questionDocName]>
		addHiddenInputToForm("ref", questionDocName, crowdForm);

		container.appendChild(crowdForm);
		return container;
	}

	private static Element createNextButtonContainer(String nextUrl) {
		Element container = createBlankContainerWithMargin();

		Element nextButton = new Element("crowd-button");
		nextButton.attr("id", "save-next");
		nextButton.attr("href", nextUrl);
		nextButton.attr("variant", "normal");
		nextButton.text("Next Task");

		container.appendChild(nextButton);
		return container;
	}

	private static Element createCrowdSubmitButtonContainer() {
		Element container = createBlankContainerWithMargin();
		Element crowdSubmitButton = createCrowdSubmitButton(false);
		container.appendChild(crowdSubmitButton);
		return container;
	}

	/**
	 * 
	 * @param hidden
	 *            : If true, then adding this element to <crowd-form> will disable its "submit" form-action
	 * @return Crowd "Submit" button
	 */
	private static Element createCrowdSubmitButton(boolean hidden) {
		Element submitButton = new Element("crowd-button");
		submitButton.attr("form-action", "submit");
		submitButton.attr("variant", "primary");
		submitButton.text("Submit");

		if (hidden) {
			submitButton.attr("style", "display:none");
		}
		return submitButton;
	}

	private static Element createBlankContainerWithMargin() {
		Element container = new Element("div");
		container.addClass(W3_CONTAINER);
		container.addClass(W3_MARGIN);
		return container;
	}

	public static Element createCrowdQuestionContainer(String question) {
		Element container = createBlankContainerWithMargin();
		Element questionHeader = new Element("h5");
		questionHeader.text(question);
		container.appendChild(questionHeader);
		return container;
	}

	public static Element createCrowdRadioGroup(String[] optionNames, String[] optionLabels) {
		Element crowdRadioGroup = new Element("crowd-radio-group");
		crowdRadioGroup.attr("allow-empty-selection", false);
		crowdRadioGroup.attr("disabled", false);

		for (int i = 0; i < optionNames.length; i++) {
			String name = optionNames[i];
			String label = optionLabels[i];
			Element crowdRadioButton = new Element("crowd-radio-button");
			crowdRadioButton.attr("id", name);
			crowdRadioButton.attr("name", name);
			crowdRadioButton.text(label);
			crowdRadioGroup.appendChild(crowdRadioButton);
		}
		return crowdRadioGroup;
	}
}
