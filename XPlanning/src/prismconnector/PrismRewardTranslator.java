package prismconnector;

import language.exceptions.XMDPException;
import language.mdp.TransitionFunction;
import language.metrics.EventBasedMetric;
import language.metrics.IQFunction;
import language.metrics.ITransitionStructure;
import language.objectives.CostFunction;
import language.objectives.IAdditiveCostFunction;
import language.qfactors.IAction;

public class PrismRewardTranslator {

	private TransitionFunction mTransFunction;
	private PrismRewardTranslatorUtilities mRewardUtilities;

	public PrismRewardTranslator(TransitionFunction transFunction, ValueEncodingScheme encodings,
			PrismRewardType prismRewardType) {
		mTransFunction = transFunction;
		mRewardUtilities = new PrismRewardTranslatorUtilities(encodings, prismRewardType);
	}

	/**
	 * 
	 * @param costFunction
	 *            : Cost function of MDP
	 * @return Reward structure representing the cost function
	 * @throws XMDPException
	 */
	public String getCostFunctionTranslation(CostFunction costFunction) throws XMDPException {
		return mRewardUtilities.buildRewardStructure(mTransFunction, costFunction);
	}

	/**
	 * 
	 * @param objectiveFunction
	 *            : Objective function -- this can be n-1-attribute cost function
	 * @return Reward structure representing the objective function
	 * @throws XMDPException
	 */
	public String getObjectiveFunctionTranslation(IAdditiveCostFunction objectiveFunction) throws XMDPException {
		return mRewardUtilities.buildRewardStructure(mTransFunction, objectiveFunction);
	}

	/**
	 * 
	 * @param qFunctions
	 *            : QA functions
	 * @return Reward structures representing the QA functions
	 * @throws XMDPException
	 */
	public String getQAFunctionsTranslation(Iterable<IQFunction<IAction, ITransitionStructure<IAction>>> qFunctions)
			throws XMDPException {
		return mRewardUtilities.buildRewardStructures(mTransFunction, qFunctions);
	}

	/**
	 * 
	 * @param qFunction
	 *            : QA function
	 * @return Reward structure representing the QA function
	 * @throws XMDPException
	 */
	public String getQAFunctionTranslation(IQFunction<?, ?> qFunction) throws XMDPException {
		return mRewardUtilities.buildRewardStructure(mTransFunction, qFunction);
	}

	/**
	 * 
	 * @param eventBasedMetric
	 *            : Event-based metric of a non-standard QA function
	 * @return Counters for different events in the eventBasedMetric
	 * @throws XMDPException
	 */
	public String getEventCounters(EventBasedMetric<?, ?, ?> eventBasedMetric) throws XMDPException {
		return mRewardUtilities.buildRewardStructuresForEventCounts(mTransFunction, eventBasedMetric.getEvents());
	}
}
