package examples.mobilerobot.dsm.parser;

import org.json.simple.JSONObject;

import examples.mobilerobot.models.Area;

public class AreaParser implements INodeAttributeParser<Area> {

	@Override
	public String getAttributeName() {
		return "area";
	}

	@Override
	public String getJSONObjectKey() {
		return "area";
	}

	@Override
	public Area parseAttribute(JSONObject nodeObject) {
		String areaType = (String) nodeObject.get(getJSONObjectKey());
		return Area.valueOf(areaType);
	}

}
