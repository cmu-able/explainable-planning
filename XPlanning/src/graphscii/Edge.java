package graphscii;

import java.util.HashMap;
import java.util.Map;

public class Edge {
	Node n0;
	public Node getSource() {
		return n0;
	}

	public Node getTarget() {
		return n1;
	}

	Node n1;
	String label;
	Map<String, Object> atts;
	boolean showLabel;
	boolean showAtts;
	
	public Edge(Node n0, Node n1, String label, Map<String, Object> atts, boolean showLabel, boolean showAtts) {
		this.n0 = n0;
		this.n1 = n1;
		this.label = label;
		this.atts = atts;
		this.showLabel = showLabel;
		this.showAtts = showAtts;
		
	}
	
	public Edge(Node n0, Node n1, String label) {
		this(n0, n1, label, new HashMap<>(), true, false);
	}
	
	public void setAttribute(String key, Object val) {
		if (atts == null) 
			atts = new HashMap<>();
		atts.put(key, val);
	}
	
	public Object getAttribute(String key) {
		if (atts == null) return null;
		return atts.get(key);
	}
}

