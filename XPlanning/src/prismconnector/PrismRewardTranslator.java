package prismconnector;

import java.util.Set;

import exceptions.XMDPException;
import factors.IAction;
import mdp.TransitionFunction;
import metrics.IQFunction;
import metrics.IQFunctionDomain;
import objectives.CostFunction;
import objectives.IAdditiveCostFunction;

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
	public String getQAFunctionsTranslation(Set<IQFunction<IAction, IQFunctionDomain<IAction>>> qFunctions)
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
	public <E extends IAction, T extends IQFunctionDomain<E>> String getQAFunctionTranslation(
			IQFunction<E, T> qFunction) throws XMDPException {
		return mRewardUtilities.buildRewardStructure(mTransFunction, qFunction);
	}
}
