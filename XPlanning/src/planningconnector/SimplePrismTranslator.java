import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.sa.rainbow.brass.metrics.ICostFunction;
import org.sa.rainbow.brass.metrics.IQualityFunction;
import org.sa.rainbow.brass.metrics.IQualityFunctionInput;
import org.sa.rainbow.brass.model.map.EnvMap;
import org.sa.rainbow.brass.model.map.EnvMapArc;
import org.sa.rainbow.brass.model.map.EnvMapNode;
import org.sa.rainbow.brass.model.translator.IPrismTranslator;

public class SimplePrismTranslator implements IPrismTranslator {
	public static final String INITIAL_ROBOT_LOCATION_CONST = "INITIAL_LOCATION";
	public static final String TARGET_ROBOT_LOCATION_CONST = "TARGET_LOCATION";
	public static final String ROBOT_LOCATION_VAR = "l";
	public static final String TIME_PROP = "time";
	public static final String COLLISION_PROP = "collision";
	
	private EnvMap m_map;
	private Map<String, IQualityFunction> m_qualityFunctions;
	private Map<String, ICostFunction> m_costFunctions;
	
	public SimplePrismTranslator(EnvMap map, 
			Map<String, IQualityFunction> qualityFunctions, Map<String, ICostFunction> costFunctions) {
		m_map = map;
		m_qualityFunctions = qualityFunctions;
		m_costFunctions = costFunctions;
	}
	
	@Override
	public void updateEnvMap(EnvMap map) {
		m_map = map;
	}
	
	@Override
	public EnvMap getEnvMap() {
		return m_map;
	}

	@Override
	public String getPrismModelTranslation(boolean inhibitTactics) {
		String translation = "mdp\n\n";
		translation += generateMapLocationConstants();
		translation += generateGoalFormula();
		translation += generateRobotModule();
		for (String propertyName : m_qualityFunctions.keySet()) {
			translation += generateQualityRewardStructure(propertyName);
			translation += generateCostRewardStructure(propertyName);
		}
		translation += generateAggregateRewardStructure();
		return translation;
	}
	
	@Override
	public String getPrismModelTranslationWithPathConstraint(List<EnvMapNode> path, boolean inhibitTactics) {
		List<String> moveActionSequence = new Stack<>();
		for (int i = 0; i < path.size() - 1; i++) {
			EnvMapNode node = path.get(i);
			EnvMapNode nextNode = path.get(i + 1);
			String moveAction = node.getLabel() + "_to_" + nextNode.getLabel();
			moveActionSequence.add(moveAction);
		}
		return getPrismModelTranslationWithPlanConstraint(moveActionSequence);
	}

	@Override
	public String getPrismModelTranslationWithPlanConstraint(List<String> actionSequence) {
		String translation = getPrismModelTranslation(false);
		translation += generatePlanConstraint(actionSequence);
		return translation;
	}
	
	private String generateMapLocationConstants() {
		String s = "// Map locations\n";
		for (String nodeLabel : m_map.getNodes().keySet()){
			int nodeId = m_map.getNodes().get(nodeLabel).getId();
            s += "const " + nodeLabel + "=" + nodeId + ";\n";
        }
		s += "\n";
		s += "const " + INITIAL_ROBOT_LOCATION_CONST + ";\n";
		s += "const " + TARGET_ROBOT_LOCATION_CONST + ";\n\n";
		return s;
	}
		
	private String generateGoalFormula() {
		String s = "// Goal conditions\n";
		s += "formula goal = " + ROBOT_LOCATION_VAR + "=" + TARGET_ROBOT_LOCATION_CONST + ";\n\n";
		return s;
	}
	
	private String generateRobotModule() {
		String s = "// Robot module\n\n";
		s += "module robot_module\n";
		int maxNodeId = m_map.getNodeCount() - 1;
		s += "\t" + ROBOT_LOCATION_VAR + " : [0.." + maxNodeId + "] init " + INITIAL_ROBOT_LOCATION_CONST + ";\n\n";
		s += generateMoveCommands();
		s += "endmodule\n\n";
		return s;
	}
	
	private String generateMoveCommands() {
		String s = "";
		for (EnvMapArc arc : m_map.getArcs()) {
			if (arc.isEnabled()) {
				s += "\t[" + arc.getSource() + "_to_" + arc.getTarget() + "] " + ROBOT_LOCATION_VAR + "=" + arc.getSource() + " -> ";
				s += "(" + ROBOT_LOCATION_VAR + "'=" + arc.getTarget() + ");\n";
			}
		}
		return s;
	}
	
	private String generatePlanConstraint(List<String> actionSequence) {
		String s = "module plan_constraint\n";
		s += "\tpc_s : [0.." + actionSequence.size() + "] init 0;\n\n";
		for (int i = 0; i < actionSequence.size(); i++) {
			int next = i + 1;
			s += "\t[" + actionSequence.get(i) + "] pc_s=" + i + " -> (pc_s'=" + next + ");\n";
		}
		s += "\n";
		s += "\t// Disallowed actions\n";
		for (EnvMapArc arc : m_map.getArcs()) {
			String moveAction = arc.getSource() + "_to_" + arc.getTarget();
            if (!actionSequence.contains(moveAction)) {
                s += "\t[" + moveAction + "] false -> true; \n";
            }
		}
		s += "endmodule\n\n";
		return s;
	}
	
	private String generateQualityRewardStructure(String propertyName) {
		IQualityFunction qualityFunction = m_qualityFunctions.get(propertyName);
		String s = "rewards \"" + propertyName + "\"\n";
		for (EnvMapArc arc : m_map.getArcs()) {
			if (arc.isEnabled()) {
				double value = -1.0;
				
				if (propertyName.equals("time")) {
					IQualityFunctionInput input = new TimeQualityFunctionInput(arc);
					value = qualityFunction.getQualityValue(input);
				} else if (propertyName.equals("collision")) {
					IQualityFunctionInput input = new CollisionQualityFunctionInput(arc);
					value = qualityFunction.getQualityValue(input);
				}

				s += "\t[" + arc.getSource() + "_to_" + arc.getTarget() + "] true : " + value + ";\n";
			}
		}
		s += "endrewards\n\n";
		return s;
	}
	
	private String generateCostRewardStructure(String propertyName) {
		IQualityFunction qualityFunction = m_qualityFunctions.get(propertyName);
		ICostFunction costFunction = m_costFunctions.get(propertyName);
		String s = "rewards \"c_" + propertyName + "\"\n";
		for (EnvMapArc arc : m_map.getArcs()) {
			if (arc.isEnabled()) {
				double cost = -1.0;
				
				if (propertyName.equals("time")) {
					IQualityFunctionInput input = new TimeQualityFunctionInput(arc);
					double value = qualityFunction.getQualityValue(input);
					cost = costFunction.getCost(value);
				} else if (propertyName.equals("collision")) {
					IQualityFunctionInput input = new CollisionQualityFunctionInput(arc);
					double value = qualityFunction.getQualityValue(input);
					cost = costFunction.getCost(value);
				}
				
				s += "\t[" + arc.getSource() + "_to_" + arc.getTarget() + "] true : " + cost + ";\n";
			}
		}
		s += "endrewards\n\n";
		return s;
	}
	
	private String generateAggregateRewardStructure() {
		String s = "// Scaling constants\n";
		for (String propertyName : m_costFunctions.keySet()) {
			s += "const double k_" + propertyName.toUpperCase() + ";\n";
		}
		s += "\n";
		s += "// Aggregate cost\n";
		s += "rewards \"c_aggregate\"\n";
		for (EnvMapArc arc : m_map.getArcs()) {
			if (arc.isEnabled()) {
				s += "\t[" + arc.getSource() + "_to_" + arc.getTarget() + "] true : ";
				boolean first = true;
				for (String propertyName : m_costFunctions.keySet()) {
					IQualityFunction qualityFunction = m_qualityFunctions.get(propertyName);
					ICostFunction costFunction = m_costFunctions.get(propertyName);
					double cost = -1.0;
					
					if (propertyName.equals("time")) {
						IQualityFunctionInput input = new TimeQualityFunctionInput(arc);
						double value = qualityFunction.getQualityValue(input);
						cost = costFunction.getCost(value);
					} else if (propertyName.equals("collision")) {
						IQualityFunctionInput input = new CollisionQualityFunctionInput(arc);
						double value = qualityFunction.getQualityValue(input);
						cost = costFunction.getCost(value);
					}
					
					if (first) {
						s += "k_" + propertyName.toUpperCase() + " * " + cost;
						first = false;
					} else {
						s += " + k_" + propertyName.toUpperCase() + " * " + cost;
					}
				}
				s += ";\n";
			}
		}
		s += "endrewards\n\n";
		return s;
	}
}
