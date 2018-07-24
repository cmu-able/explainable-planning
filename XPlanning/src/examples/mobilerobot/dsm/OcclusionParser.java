package examples.mobilerobot.dsm;

import org.json.simple.JSONObject;

import examples.mobilerobot.factors.Occlusion;

public class OcclusionParser implements IEdgeAttributeParser<Occlusion> {

	@Override
	public String getAttributeName() {
		return "occlusion";
	}

	@Override
	public String getJSONObjectKey() {
		return "obstacles";
	}

	@Override
	public Occlusion parseAttribute(JSONObject edgeObject) {
		String occlusionType = (String) edgeObject.get("occlusion");
		return Occlusion.valueOf(occlusionType);
	}

}
