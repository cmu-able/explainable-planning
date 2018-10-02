package solver.prismconnector;

import java.util.ArrayList;
import java.util.List;

import language.exceptions.QFunctionNotFoundException;
import language.mdp.QSpace;
import language.metrics.IQFunction;
import language.metrics.ITransitionStructure;
import language.objectives.IAdditiveCostFunction;
import language.qfactors.IAction;

public class QFunctionEncodingScheme {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	// Index of .srew/.trew file starts at 1
	private static final int START_REW_STRUCT_INDEX = 1;

	private QSpace mQSpace;
	private List<Object> mOrderedRewardStructs = new ArrayList<>();
	private List<IQFunction<IAction, ITransitionStructure<IAction>>> mOrderedQFunctions = new ArrayList<>();

	public QFunctionEncodingScheme(IAdditiveCostFunction objectiveFunction, QSpace qSpace) {
		mQSpace = qSpace;
		appendCostFunction(objectiveFunction);
		for (IQFunction<IAction, ITransitionStructure<IAction>> qFunction : qSpace) {
			appendQFunction(qFunction);
		}
	}

	private void appendCostFunction(IAdditiveCostFunction objectiveFunction) {
		mOrderedRewardStructs.add(objectiveFunction);
	}

	private void appendQFunction(IQFunction<IAction, ITransitionStructure<IAction>> qFunction) {
		mOrderedRewardStructs.add(qFunction);
		mOrderedQFunctions.add(qFunction);
	}

	public QSpace getQSpace() {
		return mQSpace;
	}

	public int getNumRewardStructures() {
		return mOrderedRewardStructs.size();
	}

	/**
	 * 
	 * @param qFunction
	 *            : QA function
	 * @return Index of the reward structure representing the given QA function
	 * @throws QFunctionNotFoundException
	 */
	public int getRewardStructureIndex(IQFunction<?, ?> qFunction) throws QFunctionNotFoundException {
		if (!mOrderedRewardStructs.contains(qFunction)) {
			throw new QFunctionNotFoundException(qFunction);
		}
		return mOrderedRewardStructs.indexOf(qFunction) + START_REW_STRUCT_INDEX;
	}

	/**
	 * 
	 * @param objectiveFunction
	 *            : Objective function
	 * @return Index of the reward structure representing the given objective function
	 */
	public int getRewardStructureIndex(IAdditiveCostFunction objectiveFunction) {
		if (!mOrderedRewardStructs.contains(objectiveFunction)) {
			throw new IllegalArgumentException("Objective function: " + objectiveFunction.getName() + " is not found.");
		}
		return mOrderedRewardStructs.indexOf(objectiveFunction) + START_REW_STRUCT_INDEX;
	}

	/**
	 * Use this method to ensure that: the order of which the reward structures representing the QA functions are
	 * written to the model correspond to the predefined reward-structure-index of each QA function.
	 * 
	 * @return Fixed-order QFunctions
	 */
	public List<IQFunction<IAction, ITransitionStructure<IAction>>> getOrderedQFunctions() {
		return mOrderedQFunctions;
	}

	/**
	 * 
	 * @param objectiveFunction
	 * @return Whether a given objective function has a corresponding reward structure
	 */
	public boolean contains(IAdditiveCostFunction objectiveFunction) {
		return mOrderedRewardStructs.contains(objectiveFunction);
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
		return scheme.mQSpace.equals(mQSpace) && scheme.mOrderedRewardStructs.equals(mOrderedRewardStructs);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mQSpace.hashCode();
			result = 31 * result + mOrderedRewardStructs.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
