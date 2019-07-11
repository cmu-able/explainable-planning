package mobilerobot.study.utilities;

import java.util.Map;

import org.jsoup.nodes.Element;

public class MTurkHTMLQuestionUtils {

	private static final String W3_CONTAINER = "w3-container";
	private static final String W3_MARGIN = "w3-margin";

	private static final String CROWD_FORM = "crowd-form";
	private static final String SCRIPT = "script";

	private static final String QUESTION = "question";

	private MTurkHTMLQuestionUtils() {
		throw new IllegalStateException("Utility class");
	}

	public static Element getCrowdHTMLScript() {
		Element crowdHTMLScript = new Element(SCRIPT);
		crowdHTMLScript.attr("src", "https://assets.crowd.aws/crowd-html-elements.js");
		return crowdHTMLScript;
	}

	public static Element createSubmittableCrowdFormContainer(int numQuestions, String[] dataTypes) {
		Element container = createCrowdFormContainerWithoutButton();
		Element crowdForm = container.selectFirst(CROWD_FORM);

		// Hidden inputs:
		// - question[i]-ref
		// - question[i]-answer
		// - question[i]-justification
		// - question[i]-confidence
		// All hidden inputs have empty values initially, but will be filled in with values from cookie once submit.
		for (int i = 0; i < numQuestions; i++) {
			String keyPrefix = QUESTION + i;

			for (String dataType : dataTypes) {
				String hiddenInputName = keyPrefix + "-" + dataType;

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

	public static Element getSubmittableCrowdFormOnSubmitScript(int questionIndex, int numQuestions, String[] dataTypes,
			Map<String, String[]> dataTypeOptions) {
		String crowdFormToLocalStorageFunction = getCrowdFormToLocalStorageFunction(questionIndex, dataTypes,
				dataTypeOptions);
		String localStorageToCrowdFormFunction = getLocalStorageToCrowdFormFunction(numQuestions, dataTypes);
		String submitDataLogic = getSubmitDataLogic();

		Element script = new Element(SCRIPT);
		script.appendText(crowdFormToLocalStorageFunction);
		script.appendText(localStorageToCrowdFormFunction);
		script.appendText(submitDataLogic);
		return script;
	}

	private static String getSubmitDataLogic() {
		String onSubmitFormat = "document.querySelector(\"crowd-form\").onsubmit = function() {\n%s\n%s\n};";
		return String.format(onSubmitFormat, "crowdFormToLocalStorage();", "localStorageToCrowdForm();");
	}

	private static String getCrowdFormToLocalStorageFunction(int questionIndex, String[] dataTypes,
			Map<String, String[]> dataTypeOptions) {
		String inputValueOptionFormat = "document.getElementById(\"%s\").checked";
		String inputValueTextFormat = "document.getElementById(\"%s\").value";

		String localStorageSetStrValueFormat = "localStorage.setItem(\"%s\", \"%s\")";
		String localStorageSetValueFormat = "localStorage.setItem(\"%s\", %s)";

		StringBuilder builder = new StringBuilder();
		builder.append("function crowdFormToLocalStorage() {\n");
		builder.append("\tif (typeof(Storage) !== \"undefined\") {\n");
		builder.append("\t\tlocalStorage.clear();\n\n");

		String keyPrefix = QUESTION + questionIndex;

		for (String dataType : dataTypes) {
			String localStorageKey = keyPrefix + "-" + dataType;

			if (dataTypeOptions.containsKey(dataType)) {
				// Multiple-choice input
				String[] options = dataTypeOptions.get(dataType);

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

	private static String getLocalStorageToCrowdFormFunction(int numQuestions, String[] dataTypes) {
		String hiddenInputValueFormat = "document.getElementById(\"%s\").value";
		String localStorageValueFormat = "localStorage.getItem(\"%s\")";

		StringBuilder builder = new StringBuilder();
		builder.append("function localStorageToCrowdForm() {\n");
		builder.append("  if (typeof(Storage) !== \"undefined\") {\n");

		for (int i = 0; i < numQuestions; i++) {
			String keyPrefix = QUESTION + i;

			for (String dataType : dataTypes) {
				String hiddenInputName = keyPrefix + "-" + dataType;

				builder.append("    ");
				builder.append(String.format(hiddenInputValueFormat, hiddenInputName));
				builder.append(" = ");
				builder.append(String.format(localStorageValueFormat, hiddenInputName));
				builder.append(";\n");
			}
		}

		builder.append("  }\n");
		builder.append("}");

		return builder.toString();
	}

	public static Element createIntermediateCrowdFormContainer(String nextUrl) {
		Element container = createCrowdFormContainerWithoutButton();
		Element crowdForm = container.selectFirst(CROWD_FORM);

		Element nextButtonContainer = createNextButtonContainer(nextUrl);
		// Disable submit action for intermediate form
		Element hiddenSubmitButton = createCrowdSubmitButton(true);

		crowdForm.appendChild(nextButtonContainer);
		crowdForm.appendChild(hiddenSubmitButton);
		return container;
	}

	public static Element getIntermediateCrowdFormNextOnClickScript() {
		String saveFormDataFunction = ""; // TODO
		Element script = new Element(SCRIPT);
		script.appendText(saveFormDataFunction);
		return script;
	}

	private static Element createCrowdFormContainerWithoutButton() {
		Element container = createBlankCrowdFormContainer();
		Element crowdForm = container.selectFirst(CROWD_FORM);
		Element questionDiv = createPrefAlignCrowdQuestionContainer();
		Element justificationDiv = createJustificationCrowdQuestionContainer();
		Element confidenceDiv = createConfidenceCrowdQuestionContainer();
		crowdForm.appendChild(questionDiv);
		crowdForm.appendChild(justificationDiv);
		crowdForm.appendChild(confidenceDiv);
		return container;
	}

	public static Element createBlankCrowdFormContainer() {
		Element container = new Element("div");
		container.addClass(W3_CONTAINER);
		container.addClass(W3_MARGIN);
		container.addClass("w3-sand");
		container.addClass("w3-card");

		Element crowdForm = new Element(CROWD_FORM);
		container.appendChild(crowdForm);
		return container;
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
