package mobilerobot.study.mturk;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

import org.jsoup.nodes.Element;

public class MTurkHTMLQuestionUtils {

	/**
	 * This is the format of QuestionIdentifier in QuestionFormAnswers MTurk data structure
	 */
	public static final String QUESTION_ID_FORMAT = "question%d-%s";

	/**
	 * Additional data type collected for each question
	 */
	private static final String[] AUX_DATA_TYPES = { "ref", "elapsedTime" };

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

	public static Element createSubmittableCrowdFormContainer(String questionDocName, int numQuestions,
			String[] fillableDataTypes) {
		Element container = createCrowdFormContainerWithoutButton(questionDocName);
		Element crowdForm = container.selectFirst(CROWD_FORM);

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

				Element hiddenInput = new Element("input");
				hiddenInput.attr("type", "hidden");
				hiddenInput.attr("id", hiddenInputName);
				hiddenInput.attr("name", hiddenInputName);
				hiddenInput.attr("value", "");

				crowdForm.appendChild(hiddenInput);
			}
		}

		// Submit button
		Element submitButtonContainer = createCrowdSubmitButtonContainer();
		crowdForm.appendChild(submitButtonContainer);

		return container;
	}

	public static Element getSubmittableCrowdFormOnSubmitScript(int questionIndex, int numQuestions,
			String[] fillableDataTypes, Map<String, String[]> fillableDataTypeOptions) {
		String crowdFormInputToLocalStorageFunction = getCrowdFormInputToLocalStorageFunction(questionIndex,
				fillableDataTypes, fillableDataTypeOptions);
		String localStorageToCrowdFormFunction = getLocalStorageToCrowdFormFunction(numQuestions, fillableDataTypes);
		String submitDataLogic = getSubmitDataLogic();

		Element script = new Element(SCRIPT);
		script.appendText(crowdFormInputToLocalStorageFunction);
		script.appendText(localStorageToCrowdFormFunction);
		script.appendText(submitDataLogic);
		return script;
	}

	private static String[] getAllCrowdFormInputDataTypes(String[] fillableDataTypes) {
		// All input data types in crowd-form include:
		// - fillable data types: answer, justification, and confidence
		// - hidden data type: ref
		String[] allFormDataTypes = Arrays.copyOf(fillableDataTypes, fillableDataTypes.length + 1);
		allFormDataTypes[allFormDataTypes.length - 1] = "ref";
		return allFormDataTypes;
	}

	private static String[] getAllDataTypes(String[] fillableDataTypes) {
		// All data types that will be stored in localStorage and submit to MTurk, for each question, include:
		// - fillable data types: answer, justification, and confidence
		// - additional data type: ref, elapsedTime
		return Stream.concat(Arrays.stream(fillableDataTypes), Arrays.stream(AUX_DATA_TYPES)).toArray(String[]::new);
	}

	private static String getSubmitDataLogic() {
		String onSubmitFormat = "document.querySelector(\"crowd-form\").onsubmit = function() {\n%s\n%s\n};";
		return String.format(onSubmitFormat, "crowdFormInputToLocalStorage();", "localStorageToCrowdForm();");
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

	private static String getLocalStorageToCrowdFormFunction(int numQuestions, String[] fillableDataTypes) {
		String hiddenInputValueFormat = "document.getElementById(\"%s\").value";
		String localStorageValueFormat = "localStorage.getItem(\"%s\")";

		StringBuilder builder = new StringBuilder();
		builder.append("function localStorageToCrowdForm() {\n");
		builder.append("\tif (typeof(Storage) !== \"undefined\") {\n");

		for (int i = 0; i < numQuestions; i++) {
			// Add "ref" to data types to be filled in crowd-form's hidden inputs
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

		builder.append("\t\tlocalStorage.clear();\n");
		builder.append("\t}\n");
		builder.append("}");

		return builder.toString();
	}

	public static Element createIntermediateCrowdFormContainer(String questionDocName, String nextUrl) {
		Element container = createCrowdFormContainerWithoutButton(questionDocName);
		Element crowdForm = container.selectFirst(CROWD_FORM);

		Element nextButtonContainer = createNextButtonContainer(nextUrl);
		// Disable submit action for intermediate form
		Element hiddenSubmitButton = createCrowdSubmitButton(true);

		crowdForm.appendChild(nextButtonContainer);
		crowdForm.appendChild(hiddenSubmitButton);
		return container;
	}

	public static Element getIntermediateCrowdFormNextOnClickScript(int questionIndex, String[] fillableDataTypes,
			Map<String, String[]> fillableDataTypeOptions) {
		String crowdFormInputToLocalStorageFunction = getCrowdFormInputToLocalStorageFunction(questionIndex,
				fillableDataTypes, fillableDataTypeOptions);

		String saveDataLogic = getSaveDataLogic();
		Element script = new Element(SCRIPT);
		script.appendText(crowdFormInputToLocalStorageFunction);
		script.appendText(saveDataLogic);
		return script;
	}

	private static String getSaveDataLogic() {
		String onClickFormat = "document.getElementById(\"save-next\").onclick = function() {\n%s\n};";
		return String.format(onClickFormat, "crowdFormInputToLocalStorage();");
	}

	private static Element createCrowdFormContainerWithoutButton(String questionDocName) {
		Element container = createBlankCrowdFormContainer(questionDocName);
		Element crowdForm = container.selectFirst(CROWD_FORM);
		Element questionDiv = createPrefAlignCrowdQuestionContainer();
		Element justificationDiv = createJustificationCrowdQuestionContainer();
		Element confidenceDiv = createConfidenceCrowdQuestionContainer();
		crowdForm.appendChild(questionDiv);
		crowdForm.appendChild(justificationDiv);
		crowdForm.appendChild(confidenceDiv);
		return container;
	}

	public static Element createBlankCrowdFormContainer(String questionDocName) {
		Element container = new Element("div");
		container.addClass(W3_CONTAINER);
		container.addClass(W3_MARGIN);
		container.addClass("w3-sand");
		container.addClass("w3-card");

		Element crowdForm = new Element(CROWD_FORM);
		Element questionRefHiddenInput = createQuestionRefHiddenInput(questionDocName);
		crowdForm.appendChild(questionRefHiddenInput);

		container.appendChild(crowdForm);
		return container;
	}

	private static Element createQuestionRefHiddenInput(String questionDocName) {
		String hiddenInputName = "ref";

		// This hidden input is attached to each crowd-form (either intermediate and submittable crowd-form)
		// <input type="hidden" id="ref" name="ref" value=[questionDocName]>
		Element hiddenInput = new Element("input");
		hiddenInput.attr("type", "hidden");
		hiddenInput.attr("id", hiddenInputName);
		hiddenInput.attr("name", hiddenInputName);
		hiddenInput.attr("value", questionDocName);
		return hiddenInput;
	}

	public static Element createPrefAlignCrowdQuestionContainer() {
		Element questionContainer = createBlankQuestionContainer(
				"Is the agent's proposed policy the best one, with respect to the given cost profile?");
		String[] optionNames = { "answer-yes", "answer-no" };
		String[] optionLabels = { "Yes", "No" };
		Element answerOptions = createCrowdRadioGroup(optionNames, optionLabels);
		questionContainer.appendChild(answerOptions);
		return questionContainer;
	}

	public static Element createJustificationCrowdQuestionContainer() {
		Element questionContainer = createBlankQuestionContainer("Please provide justification for your answer:");
		Element crowdTextArea = new Element("crowd-text-area");
		crowdTextArea.attr("id", "justification");
		crowdTextArea.attr("name", "justification");
		crowdTextArea.attr("label", "I gave the answer above because...");
		crowdTextArea.attr("rows", "5");
		crowdTextArea.attr("max-rows", "5");
		questionContainer.appendChild(crowdTextArea);
		return questionContainer;
	}

	public static Element createConfidenceCrowdQuestionContainer() {
		Element questionContainer = createBlankQuestionContainer("How confident are you in your answer?");
		String[] optionNames = { "confidence-high", "confidence-medium", "confidence-low" };
		String[] optionLabels = { "Highly Confident", "Somewhat Confident", "Not Confident" };
		Element options = createCrowdRadioGroup(optionNames, optionLabels);
		questionContainer.appendChild(options);
		return questionContainer;
	}

	private static Element createBlankQuestionContainer(String question) {
		Element container = new Element("div");
		container.addClass(W3_CONTAINER);
		container.addClass(W3_MARGIN);

		Element questionHeader = new Element("h5");
		questionHeader.text(question);
		container.appendChild(questionHeader);
		return container;
	}

	private static Element createCrowdRadioGroup(String[] optionNames, String[] optionLabels) {
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

	public static Element createNextButtonContainer(String nextUrl) {
		Element container = new Element("div");
		container.addClass(W3_CONTAINER);
		container.addClass(W3_MARGIN);

		Element nextButton = new Element("crowd-button");
		nextButton.attr("id", "save-next");
		nextButton.attr("href", nextUrl);
		nextButton.attr("variant", "normal");
		nextButton.text("Next Task");

		container.appendChild(nextButton);
		return container;
	}

	public static Element createCrowdSubmitButtonContainer() {
		Element container = new Element("div");
		container.addClass(W3_CONTAINER);
		container.addClass(W3_MARGIN);

		Element submitButton = createCrowdSubmitButton(false);

		container.appendChild(submitButton);
		return container;
	}

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
}
