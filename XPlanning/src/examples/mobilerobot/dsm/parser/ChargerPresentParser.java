package examples.mobilerobot.dsm.parser;

import org.json.simple.JSONObject;

import examples.mobilerobot.models.ChargerPresence;

public class ChargerPresentParser implements INodeAttributeParser<ChargerPresence> {

	@Override
	public String getAttributeName() {
		return "hasCharger";
	}

	@Override
	public String getJSONObjectKey() {
		return "charger";
	}

	@Override
	public ChargerPresence parseAttribute(JSONObject nodeObject) {
		boolean charger = (Boolean )nodeObject.get(getJSONObjectKey());
		return new ChargerPresence(charger);
	}

}
