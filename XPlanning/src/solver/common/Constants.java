package solver.common;

public class Constants {

	/**
	 * This is to ensure that there is no zero-reward cycle in the MDP. This is because the current version of PRISM 4.4
	 * does not support "constructing a strategy for Rmin in the presence of zero-reward ECs".
	 * 
	 * The cost offset is used in SSPs to ensure that all objective costs are positive, except in the goal states.
	 */
	public static final double SSP_COST_OFFSET = 1e-4; // GRB's default value of OptimalityTol is 1e-6

	private Constants() {
		throw new IllegalStateException("Utility class");
	}
}
