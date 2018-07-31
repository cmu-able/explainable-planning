package examples.mobilerobot.dsm.parser;

import org.json.simple.JSONObject;

import examples.mobilerobot.dsm.IEdgeAttribute;

public interface IEdgeAttributeParser<E extends IEdgeAttribute> {

	/**
	 * 
	 * @return Name of this edge-attribute to be parsed
	 */
	public String getAttributeName();

	/**
	 * 
	 * @return Key of the JSONObject that maps to an array of edge objects
	 */
	public String getJSONObjectKey();

	/**
	 * Parse the attribute value from an edge object
	 * 
	 * @param edgeObject
	 *            : Edge object must have the "from-id" and "to-id" keys
	 * @return Attribute value on the edge
	 */
	public E parseAttribute(JSONObject edgeObject);
}