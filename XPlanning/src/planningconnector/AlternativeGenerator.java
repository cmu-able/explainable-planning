import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AlternativeGenerator {
	private PlannerConnector m_plannerConnector;
	private Map<String, String> m_initialState;
	private Map<String, String> m_goalState;
	private MultiAttributeObjective m_originalMAObjective;
	private PlanInformation m_selectedPlanInfo;
	private DecimalFormat m_df = new DecimalFormat("#.#");

	public AlternativeGenerator(PlannerConnector plannerConnector, Map<String, String> initialState, Map<String, String> goalState, 
			MultiAttributeObjective originalMAObjective, PlanInformation selectedPlanInfo) {
		m_plannerConnector = plannerConnector;
		m_initialState = initialState;
		m_goalState = goalState;
		m_originalMAObjective = originalMAObjective;
		m_selectedPlanInfo = selectedPlanInfo;
	}
	
	public List<PlanInformation> getAlternatives() throws Exception {
		List<Map<String, Double>> otherScalingConsts = generateAlternativeScalingConstants();
		List<PlanInformation> alternatives = new ArrayList<>();
		Set<List<String>> uniquePlans = new HashSet<>();
		uniquePlans.add(m_selectedPlanInfo.getPlan());
		
		for (Map<String, Double> altScalingConsts : otherScalingConsts) {
			MultiAttributeObjective altMAObjective = new MultiAttributeObjective(
					m_originalMAObjective.getAttributes(), 
					m_originalMAObjective.getQualityFunctions(), 
					m_originalMAObjective.getCostFunctions(), 
					altScalingConsts, 
					m_originalMAObjective.getPropsFilename(), 
					m_originalMAObjective.getPropertyIndices());
			PlanInformation altInfo = m_plannerConnector.generatePlan(m_initialState, m_goalState, altMAObjective);
			
			if (!uniquePlans.contains(altInfo.getPlan())) {
				alternatives.add(altInfo);
				uniquePlans.add(altInfo.getPlan());
			}
		}
		return alternatives;
	}
	
	private List<Map<String, Double>> generateAlternativeScalingConstants() {
		List<Map<String, Double>> lst = new ArrayList<>();
		Map<String, Double> originalScalingConsts = m_originalMAObjective.getScalingConstants();
		List<List<Double>> allScalingConsts = getScalingConstsList(m_originalMAObjective.getAttributes().size(), 1.0);
		List<String> attributes = new ArrayList<>(m_originalMAObjective.getAttributes());
		Collections.sort(attributes);
		for (List<Double> scalingConsts : allScalingConsts) {
			Map<String, Double> altScalingConsts = new HashMap<>();
			for (int i = 0; i < scalingConsts.size(); i++) {
				String attributeName = attributes.get(i);
				Double scalingConst = scalingConsts.get(i);
				altScalingConsts.put(attributeName, scalingConst);
			}
			
			if (!altScalingConsts.equals(originalScalingConsts)) {
				lst.add(altScalingConsts);
			}
		}
		return lst;
	}
	
	private List<List<Double>> getScalingConstsList(int n, double sum) {		
		if (n == 1) {
			List<List<Double>> res = new ArrayList<>();
			List<Double> lst = new ArrayList<>();
			double roundedValue = Double.valueOf(m_df.format(sum));
			lst.add(roundedValue);
			res.add(lst);
			return res;
		} else {
			List<List<Double>> res = new ArrayList<>();
			double i = 0.1;
			while (i <= sum - 0.05) {
				List<List<Double>> lsts = getScalingConstsList(n - 1, sum - i);
				for (List<Double> lst : lsts) {
					i = Double.valueOf(m_df.format(i));
					lst.add(0, i);
				}
				res.addAll(lsts);
				i += 0.1;
			}
			return res;
		}
	}
}
