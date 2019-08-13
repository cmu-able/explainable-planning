package mobilerobot.study.prefalign;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.parser.ParseException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import mobilerobot.study.mturk.MTurkHTMLQuestionUtils;
import mobilerobot.study.utilities.ExplanationHTMLGenerator;
import mobilerobot.study.utilities.HTMLGeneratorUtils;
import mobilerobot.study.utilities.HTMLTableSettings;
import mobilerobot.study.utilities.JSTimingUtils;
import mobilerobot.utilities.FileIOUtils;

public class PrefAlignQuestionHTMLLinker {

	private static final String[] FILLABLE_DATA_TYPES = new String[] { "answer", "justification", "confidence" };
	private static final Map<String, String[]> FILLABLE_DATA_TYPE_OPTIONS = new HashMap<>();
	static {
		FILLABLE_DATA_TYPE_OPTIONS.put("answer", new String[] { "yes", "no" });
		FILLABLE_DATA_TYPE_OPTIONS.put("confidence", new String[] { "high", "medium", "low" });
	}

	private PrefAlignQuestionHTMLGenerator mQuestionHTMLGenerator;

	public PrefAlignQuestionHTMLLinker(PrefAlignQuestionHTMLGenerator questionHTMLGenerator) {
		mQuestionHTMLGenerator = questionHTMLGenerator;
	}

	public File[][] createAllLinkedQuestionFiles(LinkedPrefAlignQuestions[] allLinkedPrefAlignQuestions,
			boolean withExplanation, File rootStorageDir) throws IOException, ParseException, URISyntaxException {
		int numQuestions = allLinkedPrefAlignQuestions[0].getNumQuestions();
		File[][] allLinkedQuestionFiles = new File[allLinkedPrefAlignQuestions.length][numQuestions];

		for (int i = 0; i < allLinkedPrefAlignQuestions.length; i++) {
			LinkedPrefAlignQuestions linkedPrefAlignQuestions = allLinkedPrefAlignQuestions[i];

			// Linked HTML questions (of the same cost function but different maps) will be stored in the same directory
			//
			// But the files will first be created at /output/linked-questions/linked-questions-set[i]/
			// or /output/linked-questions-explanation/linked-questions-set[i]/
			File subStorageDir = new File(rootStorageDir, "linked-questions-set" + i);
			File[] linkedQuestionFiles = createLinkedQuestionFiles(linkedPrefAlignQuestions, withExplanation,
					subStorageDir);

			allLinkedQuestionFiles[i] = linkedQuestionFiles;
		}

		return allLinkedQuestionFiles;
	}

	private File[] createLinkedQuestionFiles(LinkedPrefAlignQuestions linkedPrefAlignQuestions, boolean withExplanation,
			File storageDir) throws IOException, ParseException, URISyntaxException {
		// Linked question HTML files will first be created at /output/linked-questions/linked-questions-set[i]/
		// or /output/linked-questions-explanation/linked-questions-set[i]/
		//
		// Later the files will be moved to storageDir
		File rootOutDir = FileIOUtils.createOutSubDir(FileIOUtils.getOutputDir(), storageDir.getParentFile().getName());
		File subOutDir = FileIOUtils.createOutSubDir(rootOutDir, storageDir.getName());

		int numQuestions = linkedPrefAlignQuestions.getNumQuestions();
		File[] linkedQuestionFiles = new File[numQuestions];

		PrefAlignQuestionFormGenerator formGenerator = new PrefAlignQuestionFormGenerator(numQuestions,
				FILLABLE_DATA_TYPES, FILLABLE_DATA_TYPE_OPTIONS);

		for (int j = 0; j < numQuestions; j++) {
			File questionDir = linkedPrefAlignQuestions.getQuestionDir(j);
			int agentIndex = linkedPrefAlignQuestions.getQuestionAgentIndex(j);

			// questionDir can be null if, for a particular cost structure, there are fewer associated questions than numQuestions
			if (questionDir == null) {
				// No more questionDir
				break;
			}

			String questionDocName = linkedPrefAlignQuestions.getQuestionDocumentName(j, withExplanation);
			Document questionDoc = mQuestionHTMLGenerator.createPrefAlignQuestionDocument(questionDir, agentIndex,
					withExplanation, storageDir);

			// MTurk Crowd HTML and externalHIT script
			Element externalHITScript = MTurkHTMLQuestionUtils.getExternalHITScript();
			// Element crowdScript = MTurkHTMLQuestionUtils.getCrowdHTMLScript();
			Element jqueryScript = JSTimingUtils.getJQueryScript();
			Element[] formElements;

			if (linkedPrefAlignQuestions.hasNextQuestion(j)) {
				// Intermediate crowd-form
				String nextQuestionDocName = linkedPrefAlignQuestions.getQuestionDocumentName(j + 1, withExplanation);
				String nextUrl = nextQuestionDocName + ".html";

				formElements = formGenerator.createIntermediateFormElements(questionDocName, j, nextUrl);
			} else {
				// Last, submittable crowd-form
				formElements = formGenerator.createSubmittableFormElements(questionDocName, j);
			}

			questionDoc.body().appendChild(externalHITScript);
			// questionDoc.body().appendChild(crowdScript);
			questionDoc.body().appendChild(jqueryScript);

			// Form UI and form-action script
			for (Element formElement : formElements) {
				questionDoc.body().appendChild(formElement);
			}

			// Write question HTML document to file
			File questionFile = HTMLGeneratorUtils.writeHTMLDocumentToFile(questionDoc, questionDocName, subOutDir);

			linkedQuestionFiles[j] = questionFile;
		}

		return linkedQuestionFiles;
	}

	public static void main(String[] args)
			throws URISyntaxException, IOException, ParseException, ClassNotFoundException {
		String rootStorageDirPath = args[0]; // e.g., "linked-questions", "vlinked-questions"
		String serLinkedQuestionsDirname = args[1]; // e.g., "serialized-linked-questions", "serialized-vlinked-questions"
		boolean withExplanation = args.length > 2 && args[2].equals("-e");

		File rootStorageDir = new File(rootStorageDirPath);
		File serLinkedQuestionsDir = FileIOUtils.getResourceDir(PrefAlignQuestionHTMLLinker.class,
				serLinkedQuestionsDirname);

		LinkedPrefAlignQuestions[] allLinkedPrefAlignQuestions = PrefAlignQuestionLinker
				.readAllLinkedPrefAlignQuestions(serLinkedQuestionsDir);
		linkAllPrefAlignHTMLQuestions(allLinkedPrefAlignQuestions, withExplanation, rootStorageDir);
	}

	private static void linkAllPrefAlignHTMLQuestions(LinkedPrefAlignQuestions[] allLinkedPrefAlignQuestions,
			boolean withExplanation, File rootStorageDir) throws IOException, ParseException, URISyntaxException {
		HTMLTableSettings tableSettings = ExplanationHTMLGenerator.getMobileRobotHTMLTableSettings();
		PrefAlignQuestionHTMLGenerator questionHTMLGenerator = new PrefAlignQuestionHTMLGenerator(tableSettings);

		PrefAlignQuestionHTMLLinker questionHTMLLinker = new PrefAlignQuestionHTMLLinker(questionHTMLGenerator);

		// Both explanation and no-explanation groups will have the exact same questions in the exact same order
		questionHTMLLinker.createAllLinkedQuestionFiles(allLinkedPrefAlignQuestions, withExplanation, rootStorageDir);
	}
}
