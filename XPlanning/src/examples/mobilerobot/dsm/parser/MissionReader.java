package examples.mobilerobot.dsm.parser;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import examples.mobilerobot.dsm.Mission;

public class MissionReader {

	private JSONParser mParser = new JSONParser();

	public Mission readMission(File missionJsonFile) throws IOException, ParseException {
		FileReader reader = new FileReader(missionJsonFile);
		Object object = mParser.parse(reader);
		JSONObject jsonObject = (JSONObject) object;

		String startNodeID = (String) jsonObject.get("start-id");
		String goalNodeID = (String) jsonObject.get("goal-id");
		double maxTravelTime = JSONSimpleParserUtils.parseDouble(jsonObject, "max-time");
		String mapJsonFilename = (String) jsonObject.get("map-file");
		return new Mission(startNodeID, goalNodeID, maxTravelTime, mapJsonFilename);
	}
}
