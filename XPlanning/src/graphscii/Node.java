package graphscii;

import java.util.HashMap;
import java.util.Map;

public class Node {
	
	public static class Position {
		public final double x;
		public final double y;
		
		public Position(double x, double y) {
			this.x = x; 
			this.y = y;
		}
	}
	
	public static class Dimension {
		public final double width;
		public final double height;
		
		public Dimension(double w, double h) {
			width = w;
			height = h;
		}
	}
	
	String label;
	Position pos;
	Map<String, Object> attributes;
	Dimension dim;
	boolean showLabel;
	boolean showAttributes;
	
	public Node(String label, Map<String, Object> atts, Position pos, Dimension dim, boolean showLabel, boolean showAttributes) {
		if (pos.x < 0 || pos.x > 1)
			throw new IllegalArgumentException("x position must be between 0 and 1: " + pos.x);
		if (pos.y < 0 || pos.y > 1)
			throw new IllegalArgumentException("y position must be between 0 and 1: " + pos.y );
		this.label = label;
		this.attributes = atts;
		this.pos = pos;
		this.dim = dim;
		this.showLabel = showLabel;
		this.showAttributes = showAttributes;
		
	}
	
	public void setAttribute(String key, Object val) {
		if (attributes == null) 
			attributes = new HashMap<>();
		attributes.put(key, val);
	}
	
	public Object getAttribute(String key) {
		if (attributes == null) return null;
		return attributes.get(key);
	}
	
	public String getLabel() {
		return label;
	}
	
	public Position getPosition() {
		return pos;
	}

	public Node(String label) {
		this(label, new HashMap<>(), new Position(0,0), new Dimension(10, 10), true, false);
	}
}
