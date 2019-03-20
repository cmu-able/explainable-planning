package mobilerobot.utilities;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.json.simple.parser.ParseException;

import examples.mobilerobot.dsm.IEdgeAttribute;
import examples.mobilerobot.dsm.INodeAttribute;
import examples.mobilerobot.dsm.MapTopology;
import examples.mobilerobot.dsm.exceptions.MapTopologyException;
import examples.mobilerobot.dsm.parser.AreaParser;
import examples.mobilerobot.dsm.parser.IEdgeAttributeParser;
import examples.mobilerobot.dsm.parser.INodeAttributeParser;
import examples.mobilerobot.dsm.parser.MapTopologyReader;
import examples.mobilerobot.dsm.parser.OcclusionParser;
import examples.mobilerobot.models.Area;
import examples.mobilerobot.models.Occlusion;

public class MapTopologyUtils {

	private MapTopologyUtils() {
		throw new IllegalStateException("Utility class");
	}

	public static MapTopology parseMapTopology(File mapJsonFile, boolean useAttrParsers)
			throws MapTopologyException, IOException, ParseException {
		// Area and Occlusion attribute parsers
		AreaParser areaParser = new AreaParser();
		OcclusionParser occlusionParser = new OcclusionParser();
		Set<INodeAttributeParser<? extends INodeAttribute>> nodeAttributeParsers = new HashSet<>();
		nodeAttributeParsers.add(areaParser);
		Set<IEdgeAttributeParser<? extends IEdgeAttribute>> edgeAttributeParsers = new HashSet<>();
		edgeAttributeParsers.add(occlusionParser);

		// Default node/edge attribute values
		Map<String, INodeAttribute> defaultNodeAttributes = new HashMap<>();
		Map<String, IEdgeAttribute> defaultEdgeAttributes = new HashMap<>();
		defaultNodeAttributes.put(areaParser.getAttributeName(), Area.PUBLIC);
		defaultEdgeAttributes.put(occlusionParser.getAttributeName(), Occlusion.CLEAR);

		MapTopologyReader mapReader;
		if (useAttrParsers) {
			mapReader = new MapTopologyReader(nodeAttributeParsers, edgeAttributeParsers);
		} else {
			mapReader = new MapTopologyReader(new HashSet<>(), new HashSet<>());
		}
		return mapReader.readMapTopology(mapJsonFile, defaultNodeAttributes, defaultEdgeAttributes);
	}
}
