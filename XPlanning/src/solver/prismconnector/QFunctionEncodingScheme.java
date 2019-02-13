package solver.prismconnector;

import java.util.ArrayList;
import java.util.List;

import language.domain.metrics.IQFunction;
import language.domain.metrics.ITransitionStructure;
import language.domain.models.IAction;
import language.exceptions.QFunctionNotFoundException;
import language.mdp.QSpace;
import language.objectives.CostFunction;
import language.objectives.IAdditiveCostFunction;

/**
 * {@link QFunctionEncodingScheme} defines a mapping from QA functions ({@link IQFunction}s) to the indices of their
 * corresponding PRISM reward-structures.
 * 
 * The mapping is defined arbitrarily, but the order in which the reward structures are written to PRISM MDP model file
 * (in {@link PrismRewardTranslatorHelper} conforms to this encoding.
 * 
 * @author rsukkerd
 *
 */
public class QFunctionEncodingScheme {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	// PRISM index of .srew/.trew file starts at 1
	public static final int START_REW_STRUCT_INDEX = 1;

	// PRISM index of .srew/.trew file corresponding to QA function starts at 2
	public static final int START_QA_REW_STRUCT_INDEX = 2;

	private QSpace mQSpace;

	// Fixed-order all PRISM reward structures
	private List<Object> mOrderedRewardStructs = new ArrayList<>();

	// Fixed-order of PRISM reward structures representing QA functions
	// This is a sub-list of mOrderedRewardStructs
	private List<IQFunction<IAction, ITransitionStructure<IAction>>> mOrderedQFunctions = new ArrayList<>();

	public QFunctionEncodingScheme(CostFunction costFunction, QSpace qSpace) {
		mQSpace = qSpace;

		// The cost function is always the first reward structure in PRISM model
		appendCostFunction(costFunction);

		// The order of reward structures representing the QA functions is defined arbitrarily
		for (IQFunction<IAction, ITransitionStructure<IAction>> qFunction : qSpace) {
			appendQFunction(qFunction);
		}
	}

	private void appendCostFunction(CostFunction costFunction) {
		mOrderedRewardStructs.add(costFunction);
	}

	private void appendQFunction(IQFunction<IAction, ITransitionStructure<IAction>> qFunction) {
		mOrderedRewardStructs.add(qFunction);
		mOrderedQFunctions.add(qFunction);
	}

	public QSpace getQSpace() {
		return mQSpace;
	}

	/**
	 * Get the total number of reward structures in the PRISM model. These include: 1 reward structure for the XMDP's
	 * cost function, followed by 1 reward structure for each QA function.
	 * 
	 * @return Total number of reward structures in the PRISM model
	 */
	public int getNumRewardStructures() {
		return mOrderedRewardStructs.size();
	}

	/**
	 * Get the PRISM index of the reward structure representing a given QA function.
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
	 * Get the QFunction at a given PRISM reward-structure index.
	 * 
	 * @param rewStructIndex
	 *            : Reward-structure index (starts at 2)
	 * @return QFunction at the given index
	 */
	public IQFunction<IAction, ITransitionStructure<IAction>> getQFunctionAtRewardStructureIndex(int rewStructIndex) {
		int index = rewStructIndex - START_QA_REW_STRUCT_INDEX;
		return mOrderedQFunctions.get(index);
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
		return scheme.mQSpace.equals(mQSpace) && scheme.mOrderedRewardStructs.equals(mOrderedRewardStructs)
				&& scheme.mOrderedQFunctions.equals(mOrderedQFunctions);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mQSpace.hashCode();
			result = 31 * result + mOrderedRewardStructs.hashCode();
			result = 31 * result + mOrderedQFunctions.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
