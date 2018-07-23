package examples.mobilerobot.dsm;

import org.json.simple.JSONObject;

public interface INodeAttributeParser<E extends INodeAttribute> {

	public String getAttributeName();

	public E parseAttribute(JSONObject nodeObject);
}
