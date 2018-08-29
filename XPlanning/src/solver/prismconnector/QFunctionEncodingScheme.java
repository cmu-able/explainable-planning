package solver.prismconnector;

import java.util.ArrayList;
import java.util.List;

import language.exceptions.QFunctionNotFoundException;
import language.metrics.IQFunction;
import language.objectives.IAdditiveCostFunction;

public class QFunctionEncodingScheme {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	// Index of .srew/.trew file starts at 1
	private static final int START_REW_STRUCT_INDEX = 1;

	private List<Object> mIndexedRewardStructs = new ArrayList<>();

	public void appendCostFunction(IAdditiveCostFunction objectiveFunction) {
		mIndexedRewardStructs.add(objectiveFunction);
	}

	public void appendQFunction(IQFunction<?, ?> qFunction) {
		mIndexedRewardStructs.add(qFunction);
	}

	public int getNumRewardStructures() {
		return mIndexedRewardStructs.size();
	}

	/**
	 * 
	 * @param qFunction
	 *            : QA function
	 * @return Index of the reward structure representing the given QA function
	 * @throws QFunctionNotFoundException
	 */
	public int getRewardStructureIndex(IQFunction<?, ?> qFunction) throws QFunctionNotFoundException {
		if (!mIndexedRewardStructs.contains(qFunction)) {
			throw new QFunctionNotFoundException(qFunction);
		}
		return mIndexedRewardStructs.indexOf(qFunction) + START_REW_STRUCT_INDEX;
	}

	/**
	 * 
	 * @param objectiveFunction
	 *            : Objective function
	 * @return Index of the reward structure representing the given objective function
	 */
	public int getRewardStructureIndex(IAdditiveCostFunction objectiveFunction) {
		if (!mIndexedRewardStructs.contains(objectiveFunction)) {
			throw new IllegalArgumentException("Objective function: " + objectiveFunction.getName() + " is not found.");
		}
		return mIndexedRewardStructs.indexOf(objectiveFunction) + START_REW_STRUCT_INDEX;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof QFunctionEncodingScheme)) {
			return false;
		}
		QFunctionEncodingScheme scheme = (QFunctionEncodingScheme) obj;
		return scheme.mIndexedRewardStructs.equals(mIndexedRewardStructs);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mIndexedRewardStructs.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
