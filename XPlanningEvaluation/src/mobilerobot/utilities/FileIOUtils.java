package mobilerobot.utilities;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FilenameUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import examples.common.XPlanningOutDirectories;

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

	public static File getResourceDir(Class<?> callerClass, String resourcePath) throws URISyntaxException {
		URL resourceFolderURL = callerClass.getResource(resourcePath);
		return new File(resourceFolderURL.toURI());
	}

	public static File getOutputDir() {
		return new File(OUTPUT_PATH);
	}

	/**
	 * Create an output file at /output/{outputFilename}. Create /output/ if not already exist.
	 * 
	 * @param outputFilename
	 * @return Output file at /output/{outputFilename}
	 * @throws IOException
	 */
	public static File createOutputFile(String outputFilename) throws IOException {
		// Create /output/ directory if not already exist
		Files.createDirectories(Paths.get(OUTPUT_PATH));
		return new File(OUTPUT_PATH, outputFilename);
	}

	/**
	 * Create an output file at /output/{outputSubDirname}/{outputFilename}. Create outputSubDir if not already exist.
	 * 
	 * @param outputSubDirname
	 * @param outputFilename
	 * @return Output file at /output/{outputSubDirname}/{outputFilename}
	 * @throws IOException
	 */
	public static File createOutputFile(String outputSubDirname, String outputFilename) throws IOException {
		// Create /output/{outputSubDirname}/ if not already exist
		File outputSubDir = createOutSubDir(getOutputDir(), outputSubDirname);
		return new File(outputSubDir, outputFilename);
	}

	/**
	 * Create an out file at /{outDir}/{outFilename}. Create outDir if not already exist.
	 * 
	 * @param outDir
	 * @param outFilename
	 * @return Out file at /{outDir}/{outFilename}
	 * @throws IOException
	 */
	public static File createOutFile(File outDir, String outFilename) throws IOException {
		// Create /outDir/ if not already exist
		Files.createDirectories(outDir.toPath());
		return new File(outDir, outFilename);
	}

	/**
	 * Create an out file at /{outDir}/{outSubDirname}/{outFilename}. Create outSubDir if not already exist.
	 * 
	 * @param outDir
	 * @param outSubDirname
	 * @param outFilename
	 * @return Out file at /{outDir}/{outSubDirname}/{outFilename}
	 * @throws IOException
	 */
	public static File createOutFile(File outDir, String outSubDirname, String outFilename) throws IOException {
		// Create /{outDir}/{outSubDirname}/ if not already exist
		File outSubDir = createOutSubDir(outDir, outSubDirname);
		return new File(outSubDir, outFilename);
	}

	/**
	 * Create an out sub-directory at /{outDir}/{outSubDirname}/.
	 * 
	 * @param outDir
	 * @param outSubDirname
	 * @return Out sub-directory at /{outDir}/{outSubDirname}/
	 * @throws IOException
	 */
	public static File createOutSubDir(File outDir, String outSubDirname) throws IOException {
		File outSubDir = new File(outDir, outSubDirname);
		Files.createDirectories(outSubDir.toPath());
		return outSubDir;
	}

	public static XPlanningOutDirectories createXPlanningOutDirectories() throws IOException {
		Path outputPath = FileIOUtils.getOutputDir().toPath();
		Path policiesOutputPath = outputPath.resolve(XPlanningOutDirectories.POLICIES_SUBDIR_NAME);
		Path explanationsOutputPath = outputPath.resolve(XPlanningOutDirectories.EXPLANATIONS_SUBDIR_NAME);
		Path prismOutputPath = outputPath.resolve(XPlanningOutDirectories.PRISM_SUBDIR_NAME);
		return new XPlanningOutDirectories(policiesOutputPath, explanationsOutputPath, prismOutputPath);
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

	public static JSONObject readJSONObjectFromFile(File jsonFile) throws IOException, ParseException {
		FileReader reader = new FileReader(jsonFile);
		JSONParser jsonParser = new JSONParser();
		return (JSONObject) jsonParser.parse(reader);
	}

	public static String insertIndexToFilename(String originalFilename, int index) {
		String name = FilenameUtils.removeExtension(originalFilename);
		String extension = FilenameUtils.getExtension(originalFilename);
		return name + index + "." + extension;
	}

	public static File[] listFilesWithContainFilter(File dir, String nameFilter, String fileExtension) {
		String lcNameFilter = nameFilter.toLowerCase();
		String lcFileExtension = fileExtension.toLowerCase();
		FilenameFilter filter = (directory, filename) -> filename.toLowerCase().contains(lcNameFilter)
				&& filename.toLowerCase().endsWith(lcFileExtension);
		return dir.listFiles(filter);
	}

	public static File[] listFilesWithRegexFilter(File dir, String nameRegexFilter, String fileExtension) {
		String lcNameRegexFilter = nameRegexFilter.toLowerCase();
		String lcFileExtension = fileExtension.toLowerCase();
		String escapedFileExtension = lcFileExtension.replace(".", "\\.");
		String escapedFilenameRegex = lcNameRegexFilter + escapedFileExtension;
		FilenameFilter filter = (directory, filename) -> filename.toLowerCase().matches(escapedFilenameRegex);
		return dir.listFiles(filter);
	}

}
