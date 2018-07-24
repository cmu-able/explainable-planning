package examples.mobilerobot.dsm;

import org.json.simple.JSONObject;

public interface INodeAttributeParser<E extends INodeAttribute> {

	public String getAttributeName();

	public String getJSONObjectKey();

	public E parseAttribute(JSONObject nodeObject);
}
