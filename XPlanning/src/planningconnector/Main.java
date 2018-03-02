import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.sa.rainbow.brass.PropertiesConnector;
import org.sa.rainbow.brass.metrics.ICostFunction;
import org.sa.rainbow.brass.metrics.IQualityFunction;
import org.sa.rainbow.brass.model.map.EnvMap;
import org.sa.rainbow.brass.model.translator.IPrismTranslator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Main {
	private static final int SPEED = 5;
	private static final double MAX_TIME = 4.0;

	public static void main(String[] args) throws Exception {
		Properties props = new Properties();
		props.setProperty(PropertiesConnector.PRISM_OUTPUT_DIR, "/Users/rsukkerd/Projects/self-explaining/turtlebot/prismtmp");
		props.setProperty(PropertiesConnector.PRISM_PROPERTIES_PROPKEY, "/Users/rsukkerd/Projects/self-explaining/turtlebot/planning_input/prism_models/turtlebot.props");
		props.setProperty(PropertiesConnector.MAP_PROPKEY, "/Users/rsukkerd/Projects/self-explaining/turtlebot/planning_input/map.json");
		
		EnvMap map = new EnvMap(null, props);
		PlanningModel planningModel = new PlanningModel(map);
		Map<String, String> initialState = new HashMap<>();
		Map<String, String> goalState = new HashMap<>();
		initialState.put("l", "l1");
		goalState.put("l", "l8");
		MultiAttributeObjective maObjective = createMultiAttributeObjective(props.getProperty(PropertiesConnector.PRISM_PROPERTIES_PROPKEY));
		
		IPrismTranslator prismTranslator = new SimplePrismTranslator(map, maObjective.getQualityFunctions(), maObjective.getCostFunctions());
		PlannerConnector plannerConnector = new PlannerConnector(props, prismTranslator);
		PlanInformation planInfo = plannerConnector.generatePlan(initialState, goalState, maObjective);
		
		AlternativeGenerator alternativeGenerator = new AlternativeGenerator(plannerConnector, initialState, goalState, maObjective, planInfo);
		List<PlanInformation> alternatives = alternativeGenerator.getAlternatives();
		
		writeJSONExplanationInput(planInfo, alternatives);
	}
	
	public static MultiAttributeObjective createMultiAttributeObjective(String propsFilename) {
		Set<String> attributes = new HashSet<>();
		attributes.add("time");
		attributes.add("collision");
		IQualityFunction timeQualityFun = (input) -> input.getDoubleValue("distance") / SPEED;
		IQualityFunction collisionQualityFun = (input) -> input.getDoubleValue("collision");
		ICostFunction timeCostFun = new ICostFunction() {
			
			@Override
			public double getCost(boolean qualityValue) {
				throw new UnsupportedOperationException("Invalid Operation");
			}
			
			@Override
			public double getCost(int qualityValue) {
				throw new UnsupportedOperationException("Invalid Operation");
			}
			
			@Override
			public double getCost(double time) {
				return time / MAX_TIME;
			}
		};
		ICostFunction collisionCostFun = new ICostFunction() {
			
			@Override
			public double getCost(boolean qualityValue) {
				throw new UnsupportedOperationException("Invalid Operation");
			}
			
			@Override
			public double getCost(int qualityValue) {
				throw new UnsupportedOperationException("Invalid Operation");
			}
			
			@Override
			public double getCost(double collision) {
				return collision;
			}
		};
		
		Map<String, IQualityFunction> qualityFunctions = new HashMap<>();
		qualityFunctions.put("time", timeQualityFun);
		qualityFunctions.put("collision", collisionQualityFun);
		Map<String, ICostFunction> costFunctions = new HashMap<>();
		costFunctions.put("time", timeCostFun);
		costFunctions.put("collision", collisionCostFun);
		Map<String, Double> scalingConsts = new HashMap<>();
		scalingConsts.put("time", 0.9);
		scalingConsts.put("collision", 0.1);
		Map<String, Integer> propertyIndices = new HashMap<>();
		propertyIndices.put("c_aggregate", 0);
		propertyIndices.put("time", 1);
		propertyIndices.put("collision", 2);
		propertyIndices.put("c_time", 3);
		propertyIndices.put("c_collision", 4);
		MultiAttributeObjective maObjective = new MultiAttributeObjective(attributes, qualityFunctions, costFunctions, scalingConsts, 
				propsFilename, propertyIndices);
		return maObjective;
	}
	
	public static void writeJSONExplanationInput(PlanInformation selectedPlanInfo, List<PlanInformation> alternatives) throws IOException {
		List<String> plan = selectedPlanInfo.getPlan();
		Map<String, Double> planQualityValues = selectedPlanInfo.getQualityValues();
		Map<String, Double> planCostValues = selectedPlanInfo.getCostValues();
		
		JSONObject jsonObj = new JSONObject();
		JSONArray planJsonArray = new JSONArray();
		JSONObject planQualityJsonObj = new JSONObject();
		JSONObject planCostJsonObj = new JSONObject();
		JSONArray alternativesJsonArray = new JSONArray();
		
		for (String action : plan) {
			planJsonArray.add(action);
		}
		
		for (String attributeName : planQualityValues.keySet()) {
			Double qualityValue = planQualityValues.get(attributeName);
			planQualityJsonObj.put(attributeName, qualityValue);
		}
		
		for (String attributeName : planCostValues.keySet()) {
			Double costValue = planCostValues.get(attributeName);
			planCostJsonObj.put(attributeName, costValue);
		}
		
		for (PlanInformation altInfo : alternatives) {
			List<String> altPlan = altInfo.getPlan();
			Map<String, Double> altQualityValues = altInfo.getQualityValues();
			Map<String, Double> altCostValues = altInfo.getCostValues();
			
			JSONObject altJsonObj = new JSONObject();
			JSONArray altPlanJsonArray = new JSONArray();
			JSONObject altQualityJsonObj = new JSONObject();
			JSONObject altCostJsonObj = new JSONObject();
			
			for (String action : altPlan) {
				altPlanJsonArray.add(action);
			}
			
			for (String attributeName : altQualityValues.keySet()) {
				Double qualityValue = altQualityValues.get(attributeName);
				altQualityJsonObj.put(attributeName, qualityValue);
			}
			
			for (String attributeName : altCostValues.keySet()) {
				Double costValue = altCostValues.get(attributeName);
				altCostJsonObj.put(attributeName, costValue);
			}
			
			altJsonObj.put("plan", altPlanJsonArray);
			altJsonObj.put("quality_values", altQualityJsonObj);
			altJsonObj.put("cost_values", altCostJsonObj);
			alternativesJsonArray.add(altJsonObj);
		}
		
		jsonObj.put("purpose", "get to the target location");
		jsonObj.put("plan", planJsonArray);
		jsonObj.put("quality_values", planQualityJsonObj);
		jsonObj.put("cost_values", planCostJsonObj);
		jsonObj.put("alternatives", alternativesJsonArray);
		
		FileWriter output = new FileWriter("/Users/rsukkerd/Projects/self-explaining/turtlebot/prismtmp/explanation-input.json");
		output.write(jsonObj.toJSONString());
		output.flush();
		output.close();
	}
}
