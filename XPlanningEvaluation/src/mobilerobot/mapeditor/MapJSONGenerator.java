package mobilerobot.mapeditor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import mobilerobot.utilities.FileIOUtils;

public class MapJSONGenerator {

	private static final String RESOURCE_FILE_NAME = "GHC7-waypoints.csv";
	private static final String OUTPUT_FILE_NAME = "GHC7-map.json";

	private static final String MUR_KEY = "mur";
	private static final String MAP_KEY = "map";

	public static void main(String[] args) throws URISyntaxException, IOException {
		File csvFile = FileIOUtils.getFile(MapJSONGenerator.class, FileIOUtils.MAPS_RESOURCE_PATH, RESOURCE_FILE_NAME);
		List<String> csvLines = readLines(csvFile);
		JSONObject mapJSONObj = convertCSVLinesToJSON(csvLines);
		File outputFile = FileIOUtils.createOutputFile(OUTPUT_FILE_NAME);
		FileIOUtils.prettyPrintJSONObjectToFile(mapJSONObj, outputFile);
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
		mapJSONObj.put(MUR_KEY, 10); // use default MUR value = 10

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
