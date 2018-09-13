package solver.prismconnector;

public class PrismConfiguration {

	public enum PrismEngine {
		MTBDD("MTBDD"), SPARSE("Sparse"), HYBRID("Hybrid"), EXPLICIT("Explicit");

		private String mName;

		PrismEngine(String name) {
			mName = name;
		}

		@Override
		public String toString() {
			return mName;
		}
	}

	public enum PrismMDPSolutionMethod {
		VALUE_ITERATION("Value iteration"), GAUSS_SEIDEL("Gauss-Seidel"), POLICY_ITERATION(
				"Policy iteration"), MODIFIED_POLICY_ITERATION(
						"Modified policy iteration"), LINEAR_PROGRAMMING("Linear programming");

		private String mName;

		PrismMDPSolutionMethod(String name) {
			mName = name;
		}

		@Override
		public String toString() {
			return mName;
		}
	}

	public enum PrismMDPMultiSolutionMethod {
		VALUE_ITERATION("Value iteration"), GAUSS_SEIDEL("Gauss-Seidel"), LINEAR_PROGRAMMING("Linear programming");

		private String mName;

		PrismMDPMultiSolutionMethod(String name) {
			mName = name;
		}

		@Override
		public String toString() {
			return mName;
		}
	}

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private PrismEngine mEngine;
	private PrismMDPSolutionMethod mMDPSolutionMethod;
	private PrismMDPMultiSolutionMethod mMDPMultiSolutionMethod;

	/**
	 * Default PRISM configuration: Engine: Explicit, MDP Solution Method: Value iteration, Multi-Objective MDP Solution
	 * Method: Linear programming.
	 */
	public PrismConfiguration() {
		this(PrismEngine.EXPLICIT, PrismMDPSolutionMethod.VALUE_ITERATION,
				PrismMDPMultiSolutionMethod.LINEAR_PROGRAMMING);
	}

	public PrismConfiguration(PrismEngine engine, PrismMDPSolutionMethod mdpSolutionMethod,
			PrismMDPMultiSolutionMethod mdpMultiSolutionMethod) {
		mEngine = engine;
		mMDPSolutionMethod = mdpSolutionMethod;
		mMDPMultiSolutionMethod = mdpMultiSolutionMethod;
	}

	public PrismEngine getEngine() {
		return mEngine;
	}

	public PrismMDPSolutionMethod getMDPSolutionMethod() {
		return mMDPSolutionMethod;
	}

	public PrismMDPMultiSolutionMethod getMDPMultiSolutionMethod() {
		return mMDPMultiSolutionMethod;
	}

	public void setEngine(PrismEngine engine) {
		mEngine = engine;
	}

	public void setMDPSolutionMethod(PrismMDPSolutionMethod mdpSolutionMethod) {
		mMDPSolutionMethod = mdpSolutionMethod;
	}

	public void setMDPMultiSolutionMethod(PrismMDPMultiSolutionMethod mdpMultiSolutionMethod) {
		mMDPMultiSolutionMethod = mdpMultiSolutionMethod;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof PrismConfiguration)) {
			return false;
		}
		PrismConfiguration config = (PrismConfiguration) obj;
		return config.mEngine.equals(mEngine) && config.mMDPSolutionMethod.equals(mMDPSolutionMethod)
				&& config.mMDPMultiSolutionMethod.equals(mMDPMultiSolutionMethod);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mEngine.hashCode();
			result = 31 * result + mMDPSolutionMethod.hashCode();
			result = 31 * result + mMDPMultiSolutionMethod.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
