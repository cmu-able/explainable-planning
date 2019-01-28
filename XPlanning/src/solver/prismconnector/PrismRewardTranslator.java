package solver.prismconnector;

import language.domain.metrics.EventBasedMetric;
import language.domain.metrics.IQFunction;
import language.domain.metrics.ITransitionStructure;
import language.domain.models.IAction;
import language.exceptions.XMDPException;
import language.mdp.TransitionFunction;
import language.objectives.AttributeCostFunction;
import language.objectives.CostFunction;
import language.objectives.IAdditiveCostFunction;
import solver.prismconnector.PrismTranslatorHelper.ActionFilter;

public class PrismRewardTranslator {

	private PrismRewardTranslatorHelper mRewardHelper;

	public PrismRewardTranslator(TransitionFunction transFunction, ValueEncodingScheme encodings,
			ActionFilter actionFilter) {
		mRewardHelper = new PrismRewardTranslatorHelper(transFunction, encodings, actionFilter);
	}

	/**
	 * 
	 * @param costFunction
	 *            : Cost function of MDP
	 * @return Reward structure representing the cost function
	 * @throws XMDPException
	 */
	public String getCostFunctionTranslation(CostFunction costFunction) throws XMDPException {
		return mRewardHelper.buildRewardStructure(costFunction);
	}

	/**
	 * 
	 * @param objectiveFunction
	 *            : Objective function -- this can be n-1-attribute cost function
	 * @return Reward structure representing the objective function
	 * @throws XMDPException
	 */
	public String getObjectiveFunctionTranslation(IAdditiveCostFunction objectiveFunction) throws XMDPException {
		return mRewardHelper.buildRewardStructure(objectiveFunction);
	}

	/**
	 * The order of the reward structures conforms to the {@link QFunctionEncodingScheme}.
	 * 
	 * @param qFunctions
	 *            : QA functions
	 * @return Reward structures representing the QA functions
	 * @throws XMDPException
	 */
	public String getQAFunctionsTranslation(Iterable<IQFunction<IAction, ITransitionStructure<IAction>>> qFunctions)
			throws XMDPException {
		return mRewardHelper.buildRewardStructures(qFunctions);
	}

	/**
	 * 
	 * @param qFunction
	 *            : QA function
	 * @return Reward structure representing the QA function
	 * @throws XMDPException
	 */
	public String getQAFunctionTranslation(IQFunction<?, ?> qFunction) throws XMDPException {
		return mRewardHelper.buildRewardStructure(qFunction);
	}

	/**
	 * 
	 * @param eventBasedMetric
	 *            : Event-based metric of a non-standard QA function
	 * @return Counters for different events in the eventBasedMetric
	 * @throws XMDPException
	 */
	public String getEventCounters(EventBasedMetric<?, ?, ?> eventBasedMetric) throws XMDPException {
		return mRewardHelper.buildRewardStructuresForEventCounts(eventBasedMetric.getEvents());
	}

	/**
	 * 
	 * @param attrCostFunctions
	 *            : Single-attribute cost functions of QAs
	 * @return Reward structures representing the QA cost functions
	 * @throws XMDPException
	 */
	public String getQACostFunctionsTranslation(
			Iterable<AttributeCostFunction<IQFunction<IAction, ITransitionStructure<IAction>>>> attrCostFunctions)
			throws XMDPException {
		return mRewardHelper.buildRewardStructuresForQACostFunctions(attrCostFunctions);
	}
}
