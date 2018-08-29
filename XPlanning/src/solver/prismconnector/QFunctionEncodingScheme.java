package solver.prismconnector;

import java.util.ArrayList;
import java.util.List;

import language.exceptions.QFunctionNotFoundException;
import language.metrics.IQFunction;

public class QFunctionEncodingScheme {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	// Reward structure "cost" always has index 1
	private static final int REW_STRUCT_COST_INDEX = 1;

	// Starting index of reward structures representing QA functions (if any) of an unconstrained MDP
	private static final int START_REW_STRUCT_QA_INDEX = 2;

	private List<IQFunction<?, ?>> mIndexedQFunctions = new ArrayList<>();

	public void appendQFunction(IQFunction<?, ?> qFunction) {
		mIndexedQFunctions.add(qFunction);
	}

	/**
	 * 
	 * @param qFunction
	 *            : QA function
	 * @return Index of the reward structure representing the given QA function
	 * @throws QFunctionNotFoundException
	 */
	public int getRewardStructureIndex(IQFunction<?, ?> qFunction) throws QFunctionNotFoundException {
		if (!mIndexedQFunctions.contains(qFunction)) {
			throw new QFunctionNotFoundException(qFunction);
		}
		return mIndexedQFunctions.indexOf(qFunction) + START_REW_STRUCT_QA_INDEX;
	}

	/**
	 * 
	 * @return Index of the reward structure "cost"
	 */
	public int getCostStructureIndex() {
		return REW_STRUCT_COST_INDEX;
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
		return scheme.mIndexedQFunctions.equals(mIndexedQFunctions);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mIndexedQFunctions.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
