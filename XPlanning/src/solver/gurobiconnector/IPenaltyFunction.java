package solver.gurobiconnector;

public interface IPenaltyFunction {

	public boolean isNonLinear();

	public double getPenalty(double violation);

	public double getScalingConst();

	public int getNumSamples();
}
