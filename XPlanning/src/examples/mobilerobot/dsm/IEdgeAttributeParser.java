package examples.mobilerobot.dsm;

import org.json.simple.JSONObject;

public interface IEdgeAttributeParser<E extends IEdgeAttribute> {

	public String getAttributeName();

	public E parseAttribute(JSONObject nodeObject);
}
