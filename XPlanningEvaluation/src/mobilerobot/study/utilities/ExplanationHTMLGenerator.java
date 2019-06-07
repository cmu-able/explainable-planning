package mobilerobot.study.utilities;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;

import org.json.simple.parser.ParseException;

public class ExplanationHTMLGenerator {

	public static void createAllExplanationHTMLFiles(File rootDir) throws IOException, ParseException {
		FilenameFilter explanationJsonFileFilter = (dir, name) -> name.toLowerCase().contains("explanation")
				&& name.toLowerCase().endsWith(".json");
		for (File explanationJsonFile : rootDir.listFiles(explanationJsonFileFilter)) {
			QuestionUtils.createExplanationHTMLFile(explanationJsonFile, rootDir);
		}

		FileFilter dirFilter = File::isDirectory;
		for (File subDir : rootDir.listFiles(dirFilter)) {
			createAllExplanationHTMLFiles(subDir);
		}
	}

	public static void main(String[] args) throws IOException, ParseException {
		String pathname = args[0];
		File rootDir = new File(pathname);
		ExplanationHTMLGenerator.createAllExplanationHTMLFiles(rootDir);
	}

}
