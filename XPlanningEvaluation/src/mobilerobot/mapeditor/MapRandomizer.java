package mobilerobot.mapeditor;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import examples.mobilerobot.dsm.MapTopology;
import examples.mobilerobot.dsm.exceptions.MapTopologyException;
import examples.mobilerobot.models.Area;
import examples.mobilerobot.models.Occlusion;
import mobilerobot.utilities.FileIOUtils;
import mobilerobot.utilities.MapTopologyUtils;

public class MapRandomizer {

	private RandomNodeAttributeGenerator<Area> mNodeAttrGenerator = new RandomNodeAttributeGenerator<>("area",
			new Area[] { Area.PUBLIC, Area.SEMI_PRIVATE, Area.PRIVATE });
	private RandomEdgeAttributeGenerator<Occlusion> mEdgeAttrGenerator = new RandomEdgeAttributeGenerator<>("occlusion",
			new Occlusion[] { Occlusion.CLEAR, Occlusion.PARTIALLY_OCCLUDED, Occlusion.BLOCKED },
			new double[] { 0.7, 0.25, 0.05 });

	public MapTopology generateMapWithRandomAttributes(File mapJsonFile)
			throws MapTopologyException, IOException, ParseException {
		MapTopology mapTopology = MapTopologyUtils.parseMapTopology(mapJsonFile, false);
		Area[] areaFilter = new Area[] { Area.SEMI_PRIVATE, Area.PRIVATE };
		mNodeAttrGenerator.randomlyAssignNodeAttributeValues(mapTopology, 3, areaFilter);
		mEdgeAttrGenerator.randomlyAssignEdgeAttributeValues(mapTopology);
		return mapTopology;
	}

	public Set<MapTopology> generateMapsWithRandomAttributes(File mapJsonFile, int numMaps)
			throws MapTopologyException, IOException, ParseException {
		Set<MapTopology> maps = new HashSet<>();
		for (int i = 0; i < numMaps; i++) {
			MapTopology map = generateMapWithRandomAttributes(mapJsonFile);
			maps.add(map);
		}
		return maps;
	}

	public static void main(String[] args)
			throws IOException, URISyntaxException, MapTopologyException, ParseException {
		String bareMapJsonFilename = args[0];
		int numMapsToGenerate = Integer.parseInt(args[1]);

		File mapJsonFile = FileIOUtils.getFile(MapRandomizer.class, FileIOUtils.MAPS_RESOURCE_PATH,
				bareMapJsonFilename);
		MapRandomizer mapRandomizer = new MapRandomizer();
		Set<MapTopology> randomMaps = mapRandomizer.generateMapsWithRandomAttributes(mapJsonFile, numMapsToGenerate);

		int i = 0;
		for (MapTopology map : randomMaps) {
			JSONObject mapJsonObj = MapTopologyUtils.convertMapTopologyToJSONObject(map);
			String indexedMapJsonFilename = FileIOUtils.insertIndexToFilename(bareMapJsonFilename, i);
			File indexedMapJsonFile = FileIOUtils.createOutputFile(indexedMapJsonFilename);
			FileIOUtils.prettyPrintJSONObjectToFile(mapJsonObj, indexedMapJsonFile);
			i++;
		}
	}

}
