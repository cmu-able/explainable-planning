import java.util.Map;
import java.util.Set;

import org.sa.rainbow.brass.metrics.ICostFunction;
import org.sa.rainbow.brass.metrics.IQualityFunction;

public class MultiAttributeObjective {	
	private Set<String> m_attributes;
	private Map<String, IQualityFunction> m_qualityFunctions;
	private Map<String, ICostFunction> m_costFunctions;
	private Map<String, Double> m_scalingConsts;
	private String m_propsFilename;
	private Map<String, Integer> m_propertyIndices;
	
	public MultiAttributeObjective(Set<String> attributes, 
			Map<String, IQualityFunction> qualityFunctions, Map<String, ICostFunction> costFunctions, 
			Map<String, Double> scalingConsts, String propsFilename, Map<String, Integer> propertyIndices) {
		m_attributes = attributes;
		m_qualityFunctions = qualityFunctions;
		m_costFunctions = costFunctions;
		m_scalingConsts = scalingConsts;
		m_propsFilename = propsFilename;
		m_propertyIndices = propertyIndices;
		//createAggregateCostReward();
	}
	
	public Set<String> getAttributes() {
		return m_attributes;
	}
	
	public Map<String, IQualityFunction> getQualityFunctions() {
		return m_qualityFunctions;
	}
	
	public Map<String, ICostFunction> getCostFunctions() {
		return m_costFunctions;
	}
	
	public Map<String, Double> getScalingConstants() {
		return m_scalingConsts;
	}
	
	public String getPropsFilename() {
		return m_propsFilename;
	}
	
	public Map<String, Integer> getPropertyIndices() {
		return m_propertyIndices;
	}
	
	public int getAggregateCostPropertyIndex() {
		return m_propertyIndices.get("c_aggregate");
	}
	
	public int getPropertyIndex(String attributeName, boolean isCostValue) {
		if (isCostValue) {
			return m_propertyIndices.get("c_" + attributeName);
		} else {
			return m_propertyIndices.get(attributeName);
		}
	}
}
