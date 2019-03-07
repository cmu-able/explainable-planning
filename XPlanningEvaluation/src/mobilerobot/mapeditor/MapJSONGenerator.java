package mobilerobot.mapeditor;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class MapJSONGenerator {

	private static final String RESOURCE_PATH = "maps";
	private static final String RESOURCE_FILE_NAME = "GHC7-waypoints.csv";
	private static final String OUTPUT_PATH = "output";
	private static final String OUTPUT_FILE_NAME = "GHC7-map.json";

	private static final String MPR_KEY = "mpr";
	private static final String MAP_KEY = "map";

	public static void main(String[] args) throws URISyntaxException, IOException {
		URL resourceFolderURL = MapJSONGenerator.class.getResource(RESOURCE_PATH);
		File resourceFolder = new File(resourceFolderURL.toURI());
		File csvFile = new File(resourceFolder, RESOURCE_FILE_NAME);
		if (!csvFile.exists()) {
			throw new FileNotFoundException("File not found: " + csvFile);
		}

		List<String> csvLines = readLines(csvFile);
		JSONObject mapJSONObj = convertCSVLinesToJSON(csvLines);

		// Pretty-Print JSON
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonElement element = new JsonParser().parse(mapJSONObj.toJSONString());
		String mapJSONStr = gson.toJson(element);

		File outputFile = new File(OUTPUT_PATH, OUTPUT_FILE_NAME);
		try (PrintWriter out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(outputFile)))) {
			out.print(mapJSONStr);
		}
	}

	private static List<String> readLines(File file) throws IOException {
		List<String> lines = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			String line;
			while ((line = reader.readLine()) != null) {
				lines.add(line);
			}
		}
		return lines;
	}

	private static JSONObject convertCSVLinesToJSON(List<String> csvLines) {
		Iterator<String> it = csvLines.iterator();
		String[] columnNames = it.next().split(",");

		JSONObject mapJSONObj = new JSONObject();
		mapJSONObj.put(MPR_KEY, 20);

		JSONArray nodesJSONArray = new JSONArray();
		while (it.hasNext()) {
			String line = it.next();
			JSONObject nodeJSONObj = NodeJSONObjFactory.create(line, columnNames);
			nodesJSONArray.add(nodeJSONObj);
		}
		mapJSONObj.put(MAP_KEY, nodesJSONArray);
		return mapJSONObj;
	}

}
