package examples.mobilerobot.dsm.parser;

import org.json.simple.JSONObject;

import examples.mobilerobot.models.Darkness;

public class DarknessParser implements IEdgeAttributeParser<Darkness> {

	@Override
	public String getAttributeName() {
		return "lighting";
	}

	@Override
	public String getJSONObjectKey() {
		return "lighting";
	}

	@Override
	public Darkness parseAttribute(JSONObject edgeObject) {
		Boolean dark = (Boolean )edgeObject.getOrDefault("dark", false);
		return dark ? Darkness.DARK : Darkness.LIGHT;
	}

}
