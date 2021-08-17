package examples.mobilerobot.viz;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import examples.common.DSMException;
import examples.mobilerobot.dsm.Connection;
import examples.mobilerobot.dsm.IEdgeAttribute;
import examples.mobilerobot.dsm.INodeAttribute;
import examples.mobilerobot.dsm.LocationNode;
import examples.mobilerobot.dsm.MapTopology;
import examples.mobilerobot.dsm.Mission;
import examples.mobilerobot.dsm.parser.AreaParser;
import examples.mobilerobot.dsm.parser.IEdgeAttributeParser;
import examples.mobilerobot.dsm.parser.INodeAttributeParser;
import examples.mobilerobot.dsm.parser.MapTopologyReader;
import examples.mobilerobot.dsm.parser.MissionReader;
import examples.mobilerobot.dsm.parser.OcclusionParser;
import examples.mobilerobot.models.Area;
import examples.mobilerobot.models.Occlusion;
import explanation.rendering.IPolicyRenderer;
import graphscii.Edge;
import graphscii.Graph;
import graphscii.ILabelProvider;
import graphscii.Node;
import graphscii.Node.Dimension;
import graphscii.Node.Position;

public class MapBasedPolicyRenderer implements IPolicyRenderer {
	
	/**
	 * 
	 * @return The terminal width and height
	 * @throws Throwable
	 */
	public static int[] getTerminalSize() throws Throwable {
		try {
			Object[] r = new java.io.BufferedReader(
					new java.io.InputStreamReader(Runtime.getRuntime().exec("cmd /c mode CON").getInputStream())).lines()
							.toArray();
			String lines = (String) r[3];
			lines = lines.substring(lines.indexOf(':') + 1);
			String cols = (String) r[4];
			cols = cols.substring(cols.indexOf(':') + 1);
			return new int[] { Integer.valueOf(lines.trim()), Integer.valueOf(cols.trim()) };
		} catch (Exception e) {
			try {
				Object[] r = new java.io.BufferedReader(
						new java.io.InputStreamReader(Runtime.getRuntime().exec("stty size").getInputStream())).lines()
								.toArray();
				String[] size = ((String) r[0]).split(" ");
				return new int[] { Integer.valueOf(size[0]), Integer.valueOf(size[1]) };
			} catch (IOException e1) {
				throw new Throwable("Could not find terminal size");
			}
		}
	}

	public static final String[] LEGEND = new String[] { 
			"+==========================+",
			"| LEGEND                   |",
			"+--------------------------+", 
			"| Locations:               |",
			"|  *X* : Start Node        |", 
			"|  .X. : End Node          |", 
			"| ((X)): Semi-Private      |",
			"| [[X]]: Private           |", 
			"+--------------------------+", 
			"| Edges:                   |",
			"|  ()  : Sparse occlusion  |", 
			"|  []  : Dense occlusion   |", 
			"|  " + (char )0x21D2 + "   : 0.7m/s traversal  |",
			"|  " + (char )0x2192 + "   : 0.35m/s traversal |", 
			"+--------------------------+" };
	public static final int LEGEND_WIDTH = LEGEND[0].length();
	public static final int LEGEND_HEIGHT = LEGEND.length;

	interface TransformInterface {
		double transform(double t);
	}

	private static final Area DEFAULT_AREA = Area.PUBLIC;
	private static final Occlusion DEFAULT_OCCLUSION = Occlusion.CLEAR;
	private MissionReader mMissionReader = new MissionReader();

	private File mMapsJsonDir;
	private MapTopologyReader mMapReader;
	private Map<String, INodeAttribute> mDefaultNodeAttributes = new HashMap<>();
	private Map<String, IEdgeAttribute> mDefaultEdgeAttributes = new HashMap<>();
	private Graph m_graph;

	private ILabelProvider<Node> mapNodeLabelProvider = new ILabelProvider<Node>() {

		@Override
		public String labelFor(Node t) {
			String base = t.getLabel();
			Boolean start = (Boolean) t.getAttribute("start");
			if (start != null && start)
				base = String.format("*%s*", base);
			Boolean end = (Boolean) t.getAttribute("goal");
			if (end != null && end)
				base = String.format(".%s.", base);
			Area area = (Area) t.getAttribute("area");
			switch (area) {
			case PRIVATE:
				base = String.format("[[%s]]", base);
				break;
			case SEMI_PRIVATE:
				base = String.format("((%s))", base);
				break;
			}
			return base;
		}

	};

	private ILabelProvider<Edge> mapEdgeLabelProvider = new ILabelProvider<Edge>() {
		public static final char SOUTH = 0x2191;
		public static final char SOUTH_EAST = 0x2197;
		public static final char EAST = 0x2192;
		public static final char NORTH_EAST = 0x2198;
		public static final char NORTH = 0x2193;
		public static final char NORTH_WEST = 0x2199;
		public static final char WEST = 0x2190;
		public static final char SOUTH_WEST = 0x2196;

		@Override
		public String labelFor(Edge e) {
			Character dir = getAngleChar(e);
			String label = dir == null ? "" : ("" + dir);
			Occlusion occ = (Occlusion) e.getAttribute("occlusion");
			switch (occ) {
			case OCCLUDED:
				label = String.format("[%s]", label);
				break;
			case PARTIALLY_OCCLUDED:
				label = String.format("(%s)", label);
				break;
			}

			return label;
		}

		Character getAngleChar(Edge e) {
			Double angle = (Double) e.getAttribute("dirAngle");
			Character ret = null;
			if (angle == null)
				ret = null;
			else if (angle <= 22 || angle >= 338)
				ret = EAST;
			else if (angle >= 23 && angle <= 68)
				ret = NORTH_EAST;
			else if (angle >= 68 && angle <= 113)
				ret = NORTH;
			else if (angle >= 113 && angle <= 157)
				ret = NORTH_WEST;
			else if (angle >= 157 && angle <= 202)
				ret = WEST;
			else if (angle >= 202 && angle <= 247)
				ret = SOUTH_WEST;
			else if (angle <= 292 && angle >= 247)
				ret = SOUTH;
			else if (angle >= 292 && angle <= 338)
				ret = SOUTH_EAST;
			else
				ret = null;
			String speed = (String) e.getAttribute("speed");
			if (ret != null && "fast".equals(speed)) {
				ret = (char) (ret + 0x40);
			}
			// System.out.println(
			// String.format("%s -- %s: %.0f", e.getSource().getLabel(), e.getTarget().getLabel(), angle, ret));
			return ret;

		}
	};
	private int consoleWidth;
	private String outfile;

	public MapBasedPolicyRenderer(File mapsJsonDir, File missionJsonFile, int consoleWidth) throws DSMException {
		mMapsJsonDir = mapsJsonDir;
		if (consoleWidth == - 1) {
			try {
				this.consoleWidth = getTerminalSize()[1];
			}
			catch (Throwable e) {
				this.consoleWidth = 80;
			}
		}
		else {
			this.consoleWidth = consoleWidth;
		}
		AreaParser areaParser = new AreaParser();
		OcclusionParser occlusionParser = new OcclusionParser();
		Set<INodeAttributeParser<? extends INodeAttribute>> nodeAttributeParsers = new HashSet<>();
		nodeAttributeParsers.add(areaParser);
		Set<IEdgeAttributeParser<? extends IEdgeAttribute>> edgeAttributeParsers = new HashSet<>();
		edgeAttributeParsers.add(occlusionParser);
		mMapReader = new MapTopologyReader(nodeAttributeParsers, edgeAttributeParsers);

		// Default node/edge attribute values
		mDefaultNodeAttributes.put(areaParser.getAttributeName(), DEFAULT_AREA);
		mDefaultEdgeAttributes.put(occlusionParser.getAttributeName(), DEFAULT_OCCLUSION);

		Mission mission;
		try {
			mission = mMissionReader.readMission(missionJsonFile);
		} catch (IOException | ParseException e) {
			throw new DSMException(e.getMessage());
		}
		String mapJsonFilename = mission.getMapJSONFilename();
		File mapJsonFile = new File(mMapsJsonDir, mapJsonFilename);
		MapTopology map;
		try {
			map = mMapReader.readMapTopology(mapJsonFile, mDefaultNodeAttributes, mDefaultEdgeAttributes);
		} catch (IOException | ParseException e) {
			throw new DSMException(e.getMessage());
		}
		LocationNode startNode = map.lookUpLocationNode(mission.getStartNodeID());
		LocationNode goalNode = map.lookUpLocationNode(mission.getGoalNodeID());

		// Get minmax x and y from nodes
		double minX = Integer.MAX_VALUE;
		double minY = Integer.MAX_VALUE;
		double maxX = Integer.MIN_VALUE;
		double maxY = Integer.MIN_VALUE;
		Iterator<LocationNode> nodeIterator = map.nodeIterator();
		while (nodeIterator.hasNext()) {
			LocationNode node = nodeIterator.next();
			double x = node.getNodeXCoordinate();
			double y = node.getNodeYCoordinate();

			minX = Math.min(minX, x);
			minY = Math.min(minY, y);
			maxX = Math.max(maxX, x);
			maxY = Math.max(maxY, y);
		}
		double factor = (double) this.consoleWidth / maxX;

		TransformInterface xTransform = getTransformer(minX, maxX, 0);
		TransformInterface yTransform = getTransformer(minY, maxY, 0);
		m_graph = new Graph((int) Math.ceil((maxX - minX) * factor * 4), 400);
		m_graph.setNodeLabelProvider(mapNodeLabelProvider);
		m_graph.setEdgeLabelProvider(mapEdgeLabelProvider);

		nodeIterator = map.nodeIterator();
		while (nodeIterator.hasNext()) {
			LocationNode n = nodeIterator.next();
			double tX = xTransform.transform(n.getNodeXCoordinate());
			double tY = yTransform.transform(n.getNodeYCoordinate());
			Node node = m_graph.addNode(n.getNodeID(), new HashMap<String, Object>(), new Position(tX, tY),
					new Dimension(0, 0), true, true);
			if (n == startNode)
				node.setAttribute("start", Boolean.TRUE);
			if (n == goalNode)
				node.setAttribute("goal", Boolean.TRUE);
			Area area = n.getNodeAttribute(Area.class, "area", null);
			node.setAttribute("area", area);
		}
		Iterator<Connection> edgeIterator = map.connectionIterator();
		while (edgeIterator.hasNext()) {
			Connection e = edgeIterator.next();
			Edge edge = m_graph.addEdge(e.getNodeA().getNodeID(), e.getNodeB().getNodeID(), null, new HashMap<>(), true,
					false);
			Occlusion occlusion = e.getConnectionAttribute(Occlusion.class, "occlusion");
			if (occlusion != null) {
				edge.setAttribute("occlusion", occlusion);
			}
		}

	}

	public TransformInterface getTransformer(final double min, final double max, final double offset) {
		return (v) -> (v - min + 1) / (max - min + 1);
	}

	@Override
	public void renderPolicy(String policyFile) throws IOException {
		renderPolicy(policyFile, "");

	}
	


	@Override
	public void renderPolicy(String policyFile, String prefix) throws IOException {
		m_graph.clearEdgeAttributes("dirAngle", "speed");
		JSONParser jsonParser = new JSONParser();
		JSONArray policy;
		try (FileReader fr = new FileReader(policyFile)) {
			policy = (JSONArray) ((JSONObject) jsonParser.parse(fr)).get("policy");
		} catch (ParseException e) {
			throw new IOException("Could not parse " + policyFile);
		} catch (ClassCastException e) {
			throw new IOException("Policy file is not in the right format: " + policyFile);
		}

		for (Iterator iterator = policy.iterator(); iterator.hasNext();) {
			JSONObject object = (JSONObject) iterator.next();
			JSONObject action = (JSONObject) object.get("action");
			JSONObject state = (JSONObject) object.get("state");
			String actionType = (String) action.get("type");
			if ("moveTo".equals(actionType)) {
				String sourceLabel = (String) state.get("rLoc");
				double speed = (Double) state.get("rSpeed");
				String targetLabel = (String) ((JSONArray) action.get("params")).get(0);
				Edge e = m_graph.getEdgeBySourceTarget(sourceLabel, targetLabel);
				Node source = null;
				Node target = null;
				if (e == null) {
					e = m_graph.getEdgeBySourceTarget(targetLabel, sourceLabel);
					source = e.getTarget();
					target = e.getSource();
				} else {
					source = e.getSource();
					target = e.getTarget();
				}
				double angle = Math.toDegrees(Math.atan2(target.getPosition().y - source.getPosition().y,
						target.getPosition().x - source.getPosition().x));

				if (angle < 0) {
					angle += 360;
				}
				e.setAttribute("dirAngle", angle);
				e.setAttribute("speed", speed == 0.35 ? "normal" : "fast");
			}

		}
		String renderedPolicy = m_graph.draw();
		renderedPolicy = insertLegend(renderedPolicy);
		if (this.outfile != null) {
			try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(this.outfile),
					StandardCharsets.UTF_8)) {
		
				writer.write(renderedPolicy);
			}
		}
		else {
			System.out.println(renderedPolicy);
		}
	}

	private String insertLegend(String renderedPolicy) {
		Pattern spacePattern = Pattern.compile("^(\\s*)[^\\s]");
		String[] policyByLine = renderedPolicy.split("\n");
		if (policyByLine.length < LEGEND_HEIGHT)
			return addLegendAfterEnd(renderedPolicy);
		int beforeStart = 0;
		int afterStart = 0;
		int beforeLegendCursor = 0;
		int afterLegendCursor = 0;
		// Go line by line through the policy looking for space for the legend
		// For the legend to fit, there needs to be enough contiguous lines that have the 
		// legend width clear at the beginning or at the end
		// beforeStart/afterStart store the start of enough space for the legend
		// beforeLegendCursor/afterLegendCursor store how many contiguous lines with 
		// enough space there are. We have enough space when one of these reaches LEGEND_HEIGHT.
		// If we have enough space in before, give this priority and break.
		for (int i = 0; i < policyByLine.length; i++) {
			String line = policyByLine[i];
			if (afterLegendCursor < LEGEND_HEIGHT) {
				if (line.length() + LEGEND_WIDTH < consoleWidth && afterLegendCursor < LEGEND_HEIGHT) {
					afterLegendCursor++;
				} else if (afterLegendCursor < LEGEND_HEIGHT) {
					afterLegendCursor = 0;
					afterStart = i + 1;
				}
			}
			if (beforeLegendCursor < LEGEND_HEIGHT) {
				Matcher matcher = spacePattern.matcher(line);
				if (matcher.find()) {
					int length = matcher.group(1).length();
					if (length > LEGEND_WIDTH + 2 && beforeLegendCursor < LEGEND_HEIGHT) {
						beforeLegendCursor++;
					} else if (beforeLegendCursor < LEGEND_HEIGHT) {
						beforeStart = i + 1;
						beforeLegendCursor = 0;
					}
				}
				
			}
			else {
				break; // We have enough for the before part
			}
		}
		if (beforeStart < policyByLine.length - LEGEND_HEIGHT) {
			// Enough space for the legend at the start of lines
			// beforeStart stores the line to start the legend
			return insertLegendAtBeginning(policyByLine, beforeStart);
		} else if (afterStart < policyByLine.length - LEGEND_HEIGHT) {
			// Enough space for the legend at the end of lines
			// endStart stores the line to start the legend
			return insertLegendAtEnd(policyByLine, afterStart);
		}
		return addLegendAfterEnd(renderedPolicy);

	}

	private String insertLegendAtBeginning(String[] policyByLine, int beforeStart) {
		for (int i = 0; i < LEGEND_HEIGHT; i++) {
			policyByLine[beforeStart + i] = LEGEND[i] + policyByLine[beforeStart + i].substring(LEGEND_WIDTH);
		}
		return String.join("\n", policyByLine);
	}

	private String insertLegendAtEnd(String[] policyByLine, int start) {
		// find padding length for legend
		int maxLength = 0;
		for (int i = 0; i < LEGEND_HEIGHT; i++) {
			int length = policyByLine[start + i].length();
			if (length > maxLength) maxLength = length;
		}
		// Insert
		for (int i = 0; i < LEGEND_HEIGHT; i++) {
			policyByLine[start + i ] = policyByLine[start + i] + StringUtils.repeat(' ', maxLength - policyByLine[start + i].length()) + LEGEND[i]; 
		}
		return String.join("\n", policyByLine);
	}

	private String addLegendAfterEnd(String policy) {
		String legend = String.join("\n", LEGEND);
		return policy + "\n" + legend;
	}

	public static void main(String[] args) throws DSMException, IOException {
		File mapDir = new File("data/mobilerobot/maps");
		File mission = new File("data/mobilerobot/missions/mission0.json");

		MapBasedPolicyRenderer renderer = new MapBasedPolicyRenderer(mapDir, mission, 120);
		renderer.outfile = "out.txt";
		renderer.renderPolicy("data/mobilerobot/policies/mission0/solnPolicy.json");
	}

}
