import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.sa.rainbow.brass.adaptation.DecisionEngine;
import org.sa.rainbow.brass.adaptation.PrismPolicy;
import org.sa.rainbow.brass.model.translator.IPrismConstants;
import org.sa.rainbow.brass.model.translator.IPrismTranslator;

public class PlannerConnector {
	private DecisionEngine m_decisionEngine;
	
	public PlannerConnector(Properties props, IPrismTranslator prismTranslator) throws Exception {
		m_decisionEngine = new DecisionEngine(props, prismTranslator);
	}
	
	public PlanInformation generatePlan(Map<String, String> initialState, Map<String, String> goalState, 
			MultiAttributeObjective maObjective) throws Exception {
		// Parameters: initial and goal states, scaling constants
		String origin = initialState.get("l");
		String destination = goalState.get("l");
		Map<String, String> parameters = new HashMap<>();
		
		// Scaling constants
		for (String attributeName : maObjective.getScalingConstants().keySet()) {
			String kName = "k_" + attributeName.toUpperCase();
			double kValue = maObjective.getScalingConstants().get(attributeName);
			parameters.put(kName, Double.toString(kValue));
		}
		
		IPrismConstants prismConsts = new IPrismConstants() {
			
			@Override
			public Map<String, String> getConstants() {
				return parameters;
			}
		};
		
		// Property: minimize total aggregate cost
		int propertyIndex = maObjective.getAggregateCostPropertyIndex();
		
		// Get a best candidate plan with the minimum total aggregate cost
		m_decisionEngine.findOptimalPolicy(origin, destination, prismConsts, propertyIndex, false);
		String policyFilename = m_decisionEngine.getSelectedPolicy();
		System.out.print("Best policy generated: " + policyFilename);
				
		PrismPolicy policy = new PrismPolicy(policyFilename);
		policy.readPolicy();
		List<String> plan = policy.getPlan();
		
		// Calculate various property values of the generated plan
		Map<String, Double> qualityValues = calculateQualityValues(maObjective);
		Map<String, Double> costValues = calculateCostValues(maObjective);
		
		// Create plan information
		PlanInformation planInfo = new PlanInformation(plan, qualityValues, costValues);
		return planInfo;
	}
	
	private Map<String, Double> calculateQualityValues(MultiAttributeObjective maObjective) throws Exception {
		Map<String, Double> qualityValues = new HashMap<>();
		for (String attributeName : maObjective.getAttributes()) {
			// Property: quality attribute value
			int propertyIndex = maObjective.getPropertyIndex(attributeName, false);
			double value = m_decisionEngine.getSelectedPolicyPropertyValue(propertyIndex);
			qualityValues.put(attributeName, value);
		}
		return qualityValues;
	}
	
	private Map<String, Double> calculateCostValues(MultiAttributeObjective maObjective) throws Exception {
		Map<String, Double> costValues = new HashMap<>();
		for (String attributeName : maObjective.getAttributes()) {
			// Property: attribute cost
			int propertyIndex = maObjective.getPropertyIndex(attributeName, true);
			double value = m_decisionEngine.getSelectedPolicyPropertyValue(propertyIndex);
			costValues.put(attributeName, value);
		}
		return costValues;
	}
}
