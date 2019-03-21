package mobilerobot.mapeditor;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import examples.mobilerobot.dsm.LocationNode;
import examples.mobilerobot.dsm.MapTopology;
import examples.mobilerobot.dsm.exceptions.MapTopologyException;
import examples.mobilerobot.models.Area;
import examples.mobilerobot.models.Occlusion;
import mobilerobot.utilities.FileIOUtils;
import mobilerobot.utilities.MapTopologyUtils;

public class MapRandomizer {

	private static final Area[] AREA_LIST = new Area[] { Area.PUBLIC, Area.SEMI_PRIVATE, Area.PRIVATE };
	private static final long AREA_SEED = 43;

	private static final Occlusion[] OCCL_LIST = new Occlusion[] { Occlusion.CLEAR, Occlusion.PARTIALLY_OCCLUDED,
			Occlusion.OCCLUDED };
	private static final double[] OCCL_PROBS = new double[] { 0.65, 0.2, 0.15 };
	private static final long OCCL_SEED = 42;

	private File mBareMapJsonFile;
	private RandomNodeAttributeGenerator<Area> mNodeAttrGenerator;
	private RandomEdgeAttributeGenerator<Occlusion> mEdgeAttrGenerator;

	public MapRandomizer(File bareMapJsonFile, long areaSeed, long occlusionSeed)
			throws MapTopologyException, IOException, ParseException {
		mBareMapJsonFile = bareMapJsonFile;

		// Get a fixed order of location nodes, to allow deterministic sequence of random-attribute maps
		MapTopology mapTopology = MapTopologyUtils.parseMapTopology(bareMapJsonFile, false);
		LocationNode[] auxLocNodes = MapTopologyUtils.getLocationNodeArray(mapTopology);

		// Seeded node/edge-attribute generators must be created only once per bare map
		// To produce deterministic sequence of random values, for reproducibility
		mNodeAttrGenerator = new RandomNodeAttributeGenerator<>("area", AREA_LIST, auxLocNodes, areaSeed);
		mEdgeAttrGenerator = new RandomEdgeAttributeGenerator<>("occlusion", OCCL_LIST, OCCL_PROBS, occlusionSeed);
	}

	public MapTopology generateMapWithRandomAttributes() throws MapTopologyException, IOException, ParseException {
		MapTopology mapTopology = MapTopologyUtils.parseMapTopology(mBareMapJsonFile, false);
		Area[] areaFilter = new Area[] { Area.SEMI_PRIVATE, Area.PRIVATE };
		mNodeAttrGenerator.randomlyAssignNodeAttributeValues(mapTopology, 3, areaFilter);
		mEdgeAttrGenerator.randomlyAssignEdgeAttributeValues(mapTopology);
		return mapTopology;
	}

	public Set<MapTopology> generateMapsWithRandomAttributes(int numMaps)
			throws MapTopologyException, IOException, ParseException {
		Set<MapTopology> maps = new HashSet<>();
		for (int i = 0; i < numMaps; i++) {
			MapTopology map = generateMapWithRandomAttributes();
			maps.add(map);
		}
		return maps;
	}

	public static void main(String[] args)
			throws IOException, URISyntaxException, MapTopologyException, ParseException {
		String bareMapJsonFilename = args[0];
		int numMapsToGenerate = Integer.parseInt(args[1]);

		File bareMapJsonFile = FileIOUtils.getMapFile(MapRandomizer.class, bareMapJsonFilename);
		MapRandomizer mapRandomizer = new MapRandomizer(bareMapJsonFile, AREA_SEED, OCCL_SEED);
		Set<MapTopology> randomMaps = mapRandomizer.generateMapsWithRandomAttributes(numMapsToGenerate);

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
