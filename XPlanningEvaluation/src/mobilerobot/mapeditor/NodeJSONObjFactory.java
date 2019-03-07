package mobilerobot.mapeditor;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class NodeJSONObjFactory {

	private static final int ID_INDEX = 0;
	private static final int X_COORD_INDEX = 3;
	private static final int Y_COORD_INDEX = 4;
	private static final int CONNECTIONS_INDEX = 5;

	private static final String NODE_ID_KEY = "node-id";
	private static final String X_KEY = "x";
	private static final String Y_KEY = "y";
	private static final String COORDS_KEY = "coords";
	private static final String CONNECTIONS_KEY = "connected-to";

	private NodeJSONObjFactory() {
	}

	public static JSONObject create(String line, String[] columnNames) {
		String[] tokens = line.split(",");
		String id = tokens[ID_INDEX].trim();
		float xCoord = Float.parseFloat(tokens[X_COORD_INDEX].trim());
		float yCoord = Float.parseFloat(tokens[Y_COORD_INDEX].trim());

		JSONObject nodeJSONObj = new JSONObject();
		nodeJSONObj.put(NODE_ID_KEY, "L" + id);

		JSONObject coordsJSONObj = new JSONObject();
		coordsJSONObj.put(X_KEY, xCoord);
		coordsJSONObj.put(Y_KEY, yCoord);
		nodeJSONObj.put(COORDS_KEY, coordsJSONObj);

		String[] connections = tokens[CONNECTIONS_INDEX].split(";");

		JSONArray connJSONArray = new JSONArray();
		for (String node : connections) {
			connJSONArray.add("L" + node);
		}
		nodeJSONObj.put(CONNECTIONS_KEY, connJSONArray);
		return nodeJSONObj;
	}

}
