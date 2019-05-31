package mobilerobot.study.prefalign;

import java.util.Map;

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

	public SimpleCostStructure(Map<IQFunction<?, ?>, Double> qaUnitAmounts, CostFunction costFunction) {
		mQAUnitAmounts = qaUnitAmounts;
		mCostFunction = costFunction;
	}

	public <E extends IAction, T extends ITransitionStructure<E>, S extends IQFunction<E, T>> double getCostOfEachUnit(
			S qFunction) {
		AttributeCostFunction<S> attrCostFunc = mCostFunction.getAttributeCostFunction(qFunction);
		double qaUnitAmount = mQAUnitAmounts.get(qFunction);
		double nonScaledQAUnitCost = attrCostFunc.getCost(qaUnitAmount);
		double scalingConst = mCostFunction.getScalingConstant(attrCostFunc);
		return scalingConst * nonScaledQAUnitCost;
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
		return costStruct.mQAUnitAmounts.equals(mQAUnitAmounts) && costStruct.mCostFunction.equals(mCostFunction);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mQAUnitAmounts.hashCode();
			result = 31 * result + mCostFunction.hashCode();
			hashCode = result;
		}
		return result;
	}
}
