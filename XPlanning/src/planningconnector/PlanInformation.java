import java.util.List;
import java.util.Map;

public class PlanInformation {
	private List<String> m_plan;
	private Map<String, Double> m_qualityValues;
	private Map<String, Double> m_costValues;

	public PlanInformation(List<String> plan, Map<String, Double> qualityValues, Map<String, Double> costValues) {
		m_plan = plan;
		m_qualityValues = qualityValues;
		m_costValues = costValues;
	}
	
	public List<String> getPlan() {
		return m_plan;
	}
	
	public Map<String, Double> getQualityValues() {
		return m_qualityValues;
	}
	
	public Map<String, Double> getCostValues() {
		return m_costValues;
	}
}
