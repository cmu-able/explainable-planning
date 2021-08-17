package graphscii;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import drawille.Canvas;
import graphscii.Node.Dimension;
import graphscii.Node.Position;

public class Graph {
	private int maxX;
	private int maxY;
	private Dimension defaultDimension;
	private Map<String, Node> nodes = new HashMap<>();
	private Map<String, Edge> edges = new HashMap<>();
	private ILabelProvider<Node> m_nodeLabelProvider;
	private ILabelProvider<Edge> m_edgeLabelProvider;

	public Graph() {
		this(300, 135, new Dimension(10, 10));
	}

	public Graph(int maxX, int maxY) {
		this(maxX, maxY, new Dimension(10, 10));
	}

	public Graph(int maxX, int maxY, Dimension defaultShape) {
		this.maxX = maxX;
		this.maxY = maxY;
		this.defaultDimension = defaultShape;
	}

	public void setNodeLabelProvider(ILabelProvider<Node> lp) {
		m_nodeLabelProvider = lp;
	}

	public void setEdgeLabelProvider(ILabelProvider<Edge> lp) {
		m_edgeLabelProvider = lp;
	}

	public Node addNode(String label) {
		Node value = new Node(label);
		this.nodes.put(label, value);
		return value;
	}

	public Node addNode(String label, Map<String, Object> atts, Position pos, Dimension shape, boolean showLabel,
			boolean showAttributes) {
		if (shape == null)
			shape = this.defaultDimension;
		Node node = new Node(label, atts, pos, shape, showLabel, showAttributes);
		this.nodes.put(label, node);
		return node;
	}

	public Edge addEdge(String label0, String label1) {
		if (!nodes.containsKey(label0))
			throw new IllegalArgumentException(String.format("Node '%s' does not exist", label0));
		if (!nodes.containsKey(label1))
			throw new IllegalArgumentException(String.format("Node '%s' does not exist", label1));

		Edge edge = new Edge(this.nodes.get(label0), this.nodes.get(label1), "");
		this.edges.put(label0 + label1, edge);
		return edge;
	}

	public Edge addEdge(String label0, String label1, String label, Map<String, Object> atts, boolean showLabel,
			boolean showAttributes) {
		if (!nodes.containsKey(label0))
			throw new IllegalArgumentException(String.format("Node '%s' does not exist", label0));
		if (!nodes.containsKey(label1))
			throw new IllegalArgumentException(String.format("Node '%s' does not exist", label1));

		Edge edge = new Edge(this.nodes.get(label0), this.nodes.get(label1), label, atts, showLabel, showAttributes);
		this.edges.put(label0 + label1, edge);
		return edge;
	}
	
	public void clearEdgeAttributes(String... keys) {
		for (Edge edge : this.edges.values()) {
			for (String k : keys) {
				edge.atts.remove(k);
			}
		}
	}

	public void drawNode(Canvas c, Node n) {
		double x = n.pos.x * this.maxX;
		double y = n.pos.y * this.maxY;
		double halfWidth = n.dim.width / 2;
		double halfHeight = n.dim.height / 2;

		for (int i = 1; i <= n.dim.width; i++) {
			c.set((int) Math.round(x - halfWidth) + i, (int) Math.round(y - halfHeight));
			c.set((int) Math.round(x - halfWidth) + i, (int) Math.round(y + halfHeight));
		}

		for (int i = 1; i <= n.dim.height; i++) {
			c.set((int) Math.round(x - halfWidth), (int) Math.round(y - halfHeight) + i);
			c.set((int) Math.round(x + halfWidth), (int) Math.round(y - halfHeight) + i);
		}
		String label = "";
		if (n.showLabel) {
			if (m_nodeLabelProvider != null)
				label = m_nodeLabelProvider.labelFor(n);
			else
				label += n.label;
		}
		if (n.showAttributes && m_nodeLabelProvider == null) {
			for (String key : n.attributes.keySet())
				label += String.format(", %s: %s", key, n.attributes.get(key).toString());
		}
		c.setText(new Double(x - halfWidth + 3).intValue(), new Double(y).intValue(), label);
	}
	
	public Edge getEdgeBySourceTarget(String source, String target) {
		return edges.get(source+target);
	}

	public void drawEdge(Canvas c, Edge e) {
		double x0 = e.n0.pos.x * this.maxX;
		double y0 = e.n0.pos.y * this.maxY;
		double x1 = e.n1.pos.x * this.maxX;
		double y1 = e.n1.pos.y * this.maxY;

		int halfWidth0 = (int) e.n0.dim.width / 2;
		int halfWidth1 = (int) e.n1.dim.width / 2;
		int halfHeight0 = (int) e.n0.dim.height / 2;
		int halfHeight1 = (int) e.n1.dim.height / 2;
		double xDiff = x1 - x0;
		double yDiff = y1 - y0;
		double l = Math.sqrt(xDiff * xDiff + yDiff * yDiff);
		double dx = l != 0 ? xDiff / l : 0;
		double dy = l != 0 ? yDiff / l : 0;
		for (int i = 0; i < l; i++) {
			double x = x0 + i * dx;
			double y = y0 + i * dy;
			if ((Math.abs(x - x0) > halfWidth0 && Math.abs(x - x1) > halfWidth1)
					|| (Math.abs(y - y0) > halfHeight0 && Math.abs(y - y1) > halfHeight1))
				c.set(new Double(x).intValue(), new Double(y).intValue());
		}
		StringBuffer label = new StringBuffer();
		if (e.showLabel) {
			if (m_edgeLabelProvider != null)
				label.append(m_edgeLabelProvider.labelFor(e));
			else
				label.append(e.label);
		}
		if (e.showAtts && m_edgeLabelProvider == null) {
			for (Map.Entry<String, Object> entry : e.atts.entrySet()) {
				label.append(String.format(", %s: %s", entry.getKey(), entry.getValue().toString()));
			}
		}
		c.setText(new Double(x0 + ((int) l / 2) * dx).intValue(), new Double(y0 + ((int) l / 2) * dy).intValue(),
				label.toString());
	}

	public String draw() throws IOException {
		Canvas c = new Canvas(maxX, maxY);
		for (Node node : this.nodes.values()) {
			this.drawNode(c, node);
		}
		for (Edge edge : this.edges.values()) {
			this.drawEdge(c, edge);
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		return c.render(baos).toString();
	}

	public static void main(String[] args) throws IOException {
		Graph g = new Graph(250, 250);
		// g.addNode("n0", null, new Position(0.1, 0.1), null, true, false);
		// g.addNode("n1", null, new Position(0.3, 0.1), null, true, false);
		// g.addNode("n2", null, new Position(0.2, 0.3), null, true, false);
		// g.addEdge("n0", "n1", "e0", null, true, false);
		// g.addEdge("n1", "n2", "e1", null, true, false);
		// g.addEdge("n2", "n0", "e2", null, true, false);

		// North America
		g.addNode("Alaska", null, new Position(0.05, 0.05), null, true, false);
		g.addNode("Northwest Territory", null, new Position(0.1, 0.1), null, true, false);
		g.addNode("Greenland", null, new Position(0.3, 0.1), null, true, false);
		g.addNode("Alberta", null, new Position(0.05, 0.2), null, true, false);
		g.addNode("Ontario", null, new Position(0.15, 0.3), null, true, false);
		g.addNode("Quebec", null, new Position(0.25, 0.25), null, true, false);
		g.addNode("Western US", null, new Position(0.05, 0.4), null, true, false);
		g.addNode("Eastern US", null, new Position(0.25, 0.45), null, true, false);
		g.addNode("Central America", null, new Position(0.15, 0.6), null, true, false);
		g.addEdge("Alaska", "Northwest Territory");
		g.addEdge("Alaska", "Alberta");
		g.addEdge("Northwest Territory", "Alberta");
		g.addEdge("Northwest Territory", "Ontario");
		g.addEdge("Northwest Territory", "Greenland");
		g.addEdge("Alberta", "Ontario");
		g.addEdge("Alberta", "Western US");
		g.addEdge("Ontario", "Greenland");
		g.addEdge("Ontario", "Quebec");
		g.addEdge("Ontario", "Western US");
		g.addEdge("Ontario", "Eastern US");
		g.addEdge("Greenland", "Quebec");
		g.addEdge("Western US", "Eastern US");
		g.addEdge("Western US", "Central America");
		g.addEdge("Quebec", "Eastern US");
		g.addEdge("Eastern US", "Central America");

		// South America
		g.addNode("Venezuela", null, new Position(0.2, 0.7), null, true, false);
		g.addNode("Brazil", null, new Position(0.3, 0.85), null, true, false);
		g.addNode("Peru", null, new Position(0.15, 0.8), null, true, false);
		g.addNode("Argentina", null, new Position(0.2, 1.0), null, true, false);
		g.addEdge("Venezuela", "Brazil");
		g.addEdge("Venezuela", "Peru");
		g.addEdge("Brazil", "Peru");
		g.addEdge("Brazil", "Argentina");
		g.addEdge("Peru", "Argentina");

		// Africa
		g.addNode("North Africa", null, new Position(0.45, 0.65), null, true, false);
		g.addNode("Egypt", null, new Position(0.55, 0.6), null, true, false);
		g.addNode("East Africa", null, new Position(0.65, 0.7), null, true, false);
		g.addNode("Congo", null, new Position(0.5, 0.8), null, true, false);
		g.addNode("South Africa", null, new Position(0.55, 1.0), null, true, false);
		g.addNode("Madagascar", null, new Position(0.6, 0.9), null, true, false);
		g.addEdge("North Africa", "Egypt");
		g.addEdge("North Africa", "East Africa");
		g.addEdge("North Africa", "Congo");
		g.addEdge("Egypt", "East Africa");
		g.addEdge("East Africa", "Congo");
		g.addEdge("East Africa", "Madagascar");
		g.addEdge("Congo", "South Africa");
		g.addEdge("Madagascar", "South Africa");

		// Europe
		g.addNode("Western Europe", null, new Position(0.45, 0.5), null, true, false);
		g.addNode("Southern Europe", null, new Position(0.6, 0.45), null, true, false);
		g.addNode("Northern Europe", null, new Position(0.55, 0.35), null, true, false);
		g.addNode("Great Britain", null, new Position(0.4, 0.3), null, true, false);
		g.addNode("Iceland", null, new Position(0.4, 0.2), null, true, false);
		g.addNode("Scandinavia", null, new Position(0.55, 0.1), null, true, false);
		g.addNode("Ukraine", null, new Position(0.65, 0.2), null, true, false);
		g.addEdge("Western Europe", "Southern Europe");
		g.addEdge("Western Europe", "Northern Europe");
		g.addEdge("Western Europe", "Great Britain");
		g.addEdge("Southern Europe", "Northern Europe");
		g.addEdge("Southern Europe", "Ukraine");
		g.addEdge("Northern Europe", "Great Britain");
		g.addEdge("Northern Europe", "Ukraine");
		g.addEdge("Northern Europe", "Scandinavia");
		g.addEdge("Great Britain", "Scandinavia");
		g.addEdge("Great Britain", "Iceland");
		g.addEdge("Ukraine", "Scandinavia");
		g.addEdge("Scandinavia", "Iceland");

		// Asia
		g.addNode("Middle East", null, new Position(0.7, 0.55), null, true, false);
		g.addNode("Afghanistan", null, new Position(0.75, 0.4), null, true, false);
		g.addNode("India", null, new Position(0.8, 0.6), null, true, false);
		g.addNode("Ural", null, new Position(0.75, 0.15), null, true, false);
		g.addNode("China", null, new Position(0.85, 0.5), null, true, false);
		g.addNode("Siberia", null, new Position(0.825, 0.2), null, true, false);
		g.addNode("Mongolia", null, new Position(0.95, 0.4), null, true, false);
		g.addNode("Yakutsk", null, new Position(0.9, 0.1), null, true, false);
		g.addNode("Irkutsk", null, new Position(0.9, 0.25), null, true, false);
		g.addNode("Kamchatka", null, new Position(1.0, 0.05), null, true, false);
		g.addNode("Japan", null, new Position(1.0, 0.25), null, true, false);
		g.addNode("Siam", null, new Position(0.9, 0.65), null, true, false);
		g.addEdge("Middle East", "Afghanistan");
		g.addEdge("Middle East", "India");
		g.addEdge("Afghanistan", "India");
		g.addEdge("Afghanistan", "Ural");
		g.addEdge("Afghanistan", "China");
		g.addEdge("India", "China");
		g.addEdge("India", "Siam");
		g.addEdge("Ural", "China");
		g.addEdge("Ural", "Siberia");
		g.addEdge("China", "Siam");
		g.addEdge("China", "Siberia");
		g.addEdge("China", "Mongolia");
		g.addEdge("Siberia", "Mongolia");
		g.addEdge("Siberia", "Yakutsk");
		g.addEdge("Siberia", "Irkutsk");
		g.addEdge("Mongolia", "Irkutsk");
		g.addEdge("Mongolia", "Kamchatka");
		g.addEdge("Mongolia", "Japan");
		g.addEdge("Yakutsk", "Irkutsk");
		g.addEdge("Yakutsk", "Kamchatka");
		g.addEdge("Irkutsk", "Kamchatka");
		g.addEdge("Kamchatka", "Japan");

		// Australia
		g.addNode("Indonesia", null, new Position(0.95, 0.7), null, true, false);
		g.addNode("New Guinea", null, new Position(1.0, 0.8), null, true, false);
		g.addNode("Western Australia", null, new Position(0.9, 0.9), null, true, false);
		g.addNode("Eastern Australia", null, new Position(1.0, 1.0), null, true, false);
		g.addEdge("Indonesia", "New Guinea");
		g.addEdge("Indonesia", "Western Australia");
		g.addEdge("New Guinea", "Western Australia");
		g.addEdge("New Guinea", "Eastern Australia");
		g.addEdge("Western Australia", "Eastern Australia");

		// Connections
		g.addEdge("Siam", "Indonesia", "+", null, true, false);
		g.addEdge("Southern Europe", "Middle East", "+", null, true, false);
		g.addEdge("Ukraine", "Ural", "+", null, true, false);
		g.addEdge("Ukraine", "Afghanistan", "+", null, true, false);
		g.addEdge("Ukraine", "Middle East", "+", null, true, false);
		g.addEdge("North Africa", "Western Europe", "+", null, true, false);
		g.addEdge("North Africa", "Southern Europe", "+", null, true, false);
		g.addEdge("Egypt", "Middle East", "+", null, true, false);
		g.addEdge("Egypt", "Southern Europe", "+", null, true, false);
		g.addEdge("East Africa", "Middle East", "+", null, true, false);
		g.addEdge("Brazil", "North Africa", "+", null, true, false);
		g.addEdge("Alaska", "Kamchatka", "+", null, true, false);
		g.addEdge("Greenland", "Iceland", "+", null, true, false);
		g.addEdge("Central America", "Venezuela", "+", null, true, false);

		try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream("out.txt"),
				StandardCharsets.UTF_8)) {
			writer.write(g.draw());
		}
	}

	public int getMaxX() {
		return maxX;
	}

	public void clearEdgeAttributes(String...keys) {
		for (Edge e : this.edges.values()) {
			for (String k : keys) {
				e.atts.remove(k);
			}
		}
	}

}
