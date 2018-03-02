import org.sa.rainbow.brass.model.map.EnvMap;

public class PlanningModel {
	private EnvMap m_map;

	public PlanningModel(EnvMap map) {
		m_map = map;
	}
	
	public EnvMap getEnvMap() {
		return m_map;
	}
}
