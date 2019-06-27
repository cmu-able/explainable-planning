package mobilerobot.study.prefalign;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import language.domain.metrics.IQFunction;
import language.domain.metrics.ITransitionStructure;
import language.domain.models.IAction;
import language.objectives.AttributeCostFunction;
import language.objectives.CostFunction;

public class SimpleCostStructure {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Map<IQFunction<?, ?>, Double> mQAUnitAmounts;
	private CostFunction mCostFunction;
	private CostFunction mAdjustedCostFunction;

	// Derived field: simplify all unit costs such that the lowest cost has a value 1,
	// and round each unit cost to the nearest integer
	private Map<IQFunction<?, ?>, Long> mRoundedSimplestCostStruct = new HashMap<>();

	public SimpleCostStructure(Map<IQFunction<?, ?>, Double> qaUnitAmounts, CostFunction costFunction) {
		mQAUnitAmounts = qaUnitAmounts;
		mCostFunction = costFunction;
		createRoundedSimplestCostStruct();
		mAdjustedCostFunction = createAdjustedCostFunction();
	}

	private void createRoundedSimplestCostStruct() {
		Map<IQFunction<?, ?>, Double> costStruct = new HashMap<>();
		for (IQFunction<?, ?> qFunction : mCostFunction.getQFunctions()) {
			double unitCost = getCostOfEachUnit(qFunction, mCostFunction);
			costStruct.put(qFunction, unitCost);
		}
		double minUnitCost = Collections.min(costStruct.values());
		for (Entry<IQFunction<?, ?>, Double> e : costStruct.entrySet()) {
			IQFunction<?, ?> qFunction = e.getKey();
			double unitCost = e.getValue();
			double simplestUnitCost = unitCost / minUnitCost;
			long roundedSimplestUnitCost = Math.round(simplestUnitCost);
			mRoundedSimplestCostStruct.put(qFunction, roundedSimplestUnitCost);
		}
	}

	private <E extends IAction, T extends ITransitionStructure<E>, S extends IQFunction<E, T>> double getCostOfEachUnit(
			S qFunction, CostFunction costFunction) {
		AttributeCostFunction<S> attrCostFunc = costFunction.getAttributeCostFunction(qFunction);
		double qaUnitAmount = mQAUnitAmounts.get(qFunction);
		double nonScaledQAUnitCost = attrCostFunc.getCost(qaUnitAmount);
		double scalingConst = costFunction.getScalingConstant(attrCostFunc);
		return scalingConst * nonScaledQAUnitCost;
	}

	private CostFunction createAdjustedCostFunction() {
		CostFunction adjustedCostFunction = new CostFunction();
		for (AttributeCostFunction<IQFunction<IAction, ITransitionStructure<IAction>>> attrCostFunc : mCostFunction
				.getAttributeCostFunctions()) {
			IQFunction<?, ?> qFunction = attrCostFunc.getQFunction();
			// Adjusted unit cost of Q_i(s,a)
			double roundedSimplestUnitCost = getRoundedSimplestCostOfEachUnit(qFunction);
			double qaUnitAmount = mQAUnitAmounts.get(qFunction);
			// C_i(s,a) of 1 unit of Q_i(s,a)
			double nonScaledQAUnitCost = attrCostFunc.getCost(qaUnitAmount);
			// k_i * C_i(s,a) of 1 unit of Q_i(s,a) = adjusted unit cost of Q_i(s,a)
			double adjustedScalingConst = roundedSimplestUnitCost / nonScaledQAUnitCost;
			adjustedCostFunction.put(attrCostFunc, adjustedScalingConst);
		}
		return adjustedCostFunction;
	}

	public double getRoundedSimplestCostOfEachUnit(IQFunction<?, ?> qFunction) {
		return mRoundedSimplestCostStruct.get(qFunction);
	}

	public CostFunction getAdjustedCostFunction() {
		return mAdjustedCostFunction;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof SimpleCostStructure)) {
			return false;
		}
		SimpleCostStructure costStruct = (SimpleCostStructure) obj;
		return costStruct.mQAUnitAmounts.equals(mQAUnitAmounts) && costStruct.mCostFunction.equals(mCostFunction)
				&& costStruct.mAdjustedCostFunction.equals(mAdjustedCostFunction);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mQAUnitAmounts.hashCode();
			result = 31 * result + mCostFunction.hashCode();
			result = 31 * result + mAdjustedCostFunction.hashCode();
			hashCode = result;
		}
		return result;
	}
}
