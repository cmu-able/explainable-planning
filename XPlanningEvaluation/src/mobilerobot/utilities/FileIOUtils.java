package mobilerobot.utilities;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.net.URL;

public class FileIOUtils {

	public static final String MAPS_RESOURCE_PATH = "maps";
	public static final String POLICIES_RESOURCE_PATH = "policies";
	public static final String OUTPUT_PATH = "output";

	private FileIOUtils() {
		throw new IllegalStateException("Utility class");
	}

	public static File getFile(Class<?> callerClass, String resourcePath, String filename)
			throws URISyntaxException, FileNotFoundException {
		URL resourceFolderURL = callerClass.getResource(resourcePath);
		File resourceFolder = new File(resourceFolderURL.toURI());
		File file = new File(resourceFolder, filename);
		if (!file.exists()) {
			throw new FileNotFoundException("File not found: " + file);
		}
		return file;
	}

	public static File createOutputFile(String outputFilename) {
		return new File(OUTPUT_PATH, outputFilename);
	}

}
