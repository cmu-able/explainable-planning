package mobilerobot.utilities;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.net.URL;

import org.json.simple.JSONObject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

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

	public static void prettyPrintJSONObjectToFile(JSONObject jsonObj, File outputFile) throws FileNotFoundException {
		// Pretty-Print JSON
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonElement element = new JsonParser().parse(jsonObj.toJSONString());
		String jsonStr = gson.toJson(element);

		try (PrintWriter out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(outputFile)))) {
			out.print(jsonStr);
		}
	}

}
