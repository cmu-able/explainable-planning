import org.sa.rainbow.brass.metrics.IQualityFunctionInput;
import org.sa.rainbow.brass.model.map.EnvMapArc;

public class TimeQualityFunctionInput implements IQualityFunctionInput {
	private EnvMapArc m_arc;

	public TimeQualityFunctionInput(EnvMapArc arc) {
		m_arc = arc;
	}
	
	@Override
	public double getDoubleValue(String variableName) {
		if (variableName.equals("distance")) {
			return m_arc.getDistance();
		}
		
		throw new UnsupportedOperationException("Invalid Argument: " + variableName);
	}

	@Override
	public int getIntegerValue(String variableName) {
		throw new UnsupportedOperationException("Invalid Operation");
	}

	@Override
	public boolean getBooleanValue(String variableName) {
		throw new UnsupportedOperationException("Invalid Operation");
	}

}
