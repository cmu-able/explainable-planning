package examples.mobilerobot.dsm.parser;

import org.json.simple.JSONObject;

import examples.mobilerobot.dsm.INodeAttribute;

public interface INodeAttributeParser<E extends INodeAttribute> {

	/**
	 * 
	 * @return Name of this node-attribute to be parsed
	 */
	public String getAttributeName();

	/**
	 * This can be either a key of a node JSONObject or a key representing an additional node attribute.
	 * 
	 * @return Key of a node JSONObject that maps to an attribute value, or key of the JSONObject that maps to an array
	 *         of attributes of multiple nodes
	 */
	public String getJSONObjectKey();

	/**
	 * 
	 * @param nodeObject
	 *            : Either a node object or an object encapsulating an attribute of a particular node
	 * @return Attribute value of the node
	 */
	public E parseAttribute(JSONObject nodeObject);
}
