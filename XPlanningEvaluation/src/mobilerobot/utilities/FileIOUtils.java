package mobilerobot.utilities;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;

import org.apache.commons.io.FilenameUtils;
import org.json.simple.JSONObject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class FileIOUtils {

	public static final String MAPS_RESOURCE_PATH = "maps";
	public static final String MISSIONS_RESOURCE_PATH = "missions";
	public static final String POLICIES_RESOURCE_PATH = "policies";
	public static final String OUTPUT_PATH = "output";

	private FileIOUtils() {
		throw new IllegalStateException("Utility class");
	}

	public static File getMapFile(Class<?> callerClass, String mapFilename)
			throws FileNotFoundException, URISyntaxException {
		return getFile(callerClass, MAPS_RESOURCE_PATH, mapFilename);
	}

	public static File getPolicyFile(Class<?> callerClass, String policyFilename)
			throws FileNotFoundException, URISyntaxException {
		return getFile(callerClass, POLICIES_RESOURCE_PATH, policyFilename);
	}

	public static File getFile(Class<?> callerClass, String resourcePath, String filename)
			throws URISyntaxException, FileNotFoundException {
		File resourceFolder = getResourceDir(callerClass, resourcePath);
		File file = new File(resourceFolder, filename);
		if (!file.exists()) {
			throw new FileNotFoundException("File not found: " + file);
		}
		return file;
	}

	public static File getMapsResourceDir(Class<?> callerClass) throws URISyntaxException {
		return getResourceDir(callerClass, MAPS_RESOURCE_PATH);
	}

	public static File getMissionsResourceDir(Class<?> callerClass) throws URISyntaxException {
		return getResourceDir(callerClass, MISSIONS_RESOURCE_PATH);
	}

	public static File getPoliciesResourceDir(Class<?> callerClass) throws URISyntaxException {
		return getResourceDir(callerClass, POLICIES_RESOURCE_PATH);
	}

	private static File getResourceDir(Class<?> callerClass, String resourcePath) throws URISyntaxException {
		URL resourceFolderURL = callerClass.getResource(resourcePath);
		return new File(resourceFolderURL.toURI());
	}

	public static File createOutputFile(String outputFilename) {
		return new File(OUTPUT_PATH, outputFilename);
	}

	public static File createOutputFile(String outputSubDirname, String outputFilename) throws IOException {
		File outputSubDir = getOutputSubDir(outputSubDirname);
		return new File(outputSubDir, outputFilename);
	}

	public static File getOutputSubDir(String outputSubDirname) throws IOException {
		File outputSubDir = new File(OUTPUT_PATH, outputSubDirname);
		Files.createDirectories(outputSubDir.toPath());
		return outputSubDir;
	}

	public static void prettyPrintJSONObjectToFile(JSONObject jsonObj, File outputFile) throws FileNotFoundException {
		// Pretty-Print JSON
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonElement element = new JsonParser().parse(jsonObj.toJSONString());
		String jsonStr = gson.toJson(element);

		try (PrintWriter out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(outputFile)))) {
			out.print(jsonStr);
		}
	}

	public static String insertIndexToFilename(String originalFilename, int index) {
		String name = FilenameUtils.removeExtension(originalFilename);
		String extension = FilenameUtils.getExtension(originalFilename);
		return name + index + "." + extension;
	}

}
