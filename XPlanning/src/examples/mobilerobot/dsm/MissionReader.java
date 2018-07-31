package examples.mobilerobot.dsm;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class MissionReader {

	private JSONParser mParser = new JSONParser();

	public Mission readMission(String missionJsonDir, String missionJsonFilename) throws IOException, ParseException {
		File missionJsonFile = new File(missionJsonDir, missionJsonFilename);
		FileReader reader = new FileReader(missionJsonFile);
		Object object = mParser.parse(reader);
		JSONObject jsonObject = (JSONObject) object;

		String startNodeID = (String) jsonObject.get("start-id");
		String goalNodeID = (String) jsonObject.get("goal-id");
		double maxTravelTime = (double) jsonObject.get("max-time");
		return new Mission(startNodeID, goalNodeID, maxTravelTime);
	}
}
