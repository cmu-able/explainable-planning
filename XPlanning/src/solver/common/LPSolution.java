package solver.common;

import java.util.HashMap;
import java.util.Map;

public class LPSolution {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private boolean mExists;
	private double mObjectiveValue;
	private Map<String, double[][]> mSolutions = new HashMap<>();

	public LPSolution(boolean exists, double objectiveValue) {
		mExists = exists;
		mObjectiveValue = objectiveValue;
	}

	public void addSolution(String varName, double[][] solution) {
		int n = solution.length;
		int m = solution[0].length;
		double[][] solutionCopy = new double[n][m];
		System.arraycopy(solution, 0, solutionCopy, 0, solution.length);
		mSolutions.put(varName, solutionCopy);
	}

	public boolean exists() {
		return mExists;
	}

	public double getObjectiveValue() {
		return mObjectiveValue;
	}

	public double[][] getSolution(String varName) {
		return mSolutions.get(varName);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof LPSolution)) {
			return false;
		}
		LPSolution solution = (LPSolution) obj;
		return Boolean.compare(solution.mExists, mExists) == 0
				&& Double.compare(solution.mObjectiveValue, mObjectiveValue) == 0
				&& solution.mSolutions.equals(mSolutions);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + Boolean.hashCode(mExists);
			result = 31 * result + Double.hashCode(mObjectiveValue);
			result = 31 * result + mSolutions.hashCode();
			hashCode = result;
		}
		return result;
	}
}
