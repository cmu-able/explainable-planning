package prismconnector;

import java.util.Set;

import exceptions.ActionDefinitionNotFoundException;
import exceptions.ActionNotFoundException;
import exceptions.AttributeNameNotFoundException;
import exceptions.DiscriminantNotFoundException;
import exceptions.IncompatibleVarException;
import exceptions.VarNotFoundException;
import mdp.TransitionFunction;
import metrics.IQFunction;
import preferences.CostFunction;
import preferences.IAdditiveCostFunction;

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
	 * @throws VarNotFoundException
	 * @throws AttributeNameNotFoundException
	 * @throws IncompatibleVarException
	 * @throws DiscriminantNotFoundException
	 * @throws ActionNotFoundException
	 * @throws ActionDefinitionNotFoundException
	 */
	public String getCostFunctionTranslation(CostFunction costFunction)
			throws VarNotFoundException, AttributeNameNotFoundException, IncompatibleVarException,
			DiscriminantNotFoundException, ActionNotFoundException, ActionDefinitionNotFoundException {
		return mRewardUtilities.buildRewardStructure(mTransFunction, costFunction);
	}

	/**
	 * 
	 * @param objectiveFunction
	 *            : Objective function -- this can be n-1-attribute cost function
	 * @return Reward structure representing the objective function
	 * @throws VarNotFoundException
	 * @throws AttributeNameNotFoundException
	 * @throws IncompatibleVarException
	 * @throws DiscriminantNotFoundException
	 * @throws ActionNotFoundException
	 * @throws ActionDefinitionNotFoundException
	 */
	public String getObjectiveFunctionTranslation(IAdditiveCostFunction objectiveFunction)
			throws VarNotFoundException, AttributeNameNotFoundException, IncompatibleVarException,
			DiscriminantNotFoundException, ActionNotFoundException, ActionDefinitionNotFoundException {
		return mRewardUtilities.buildRewardStructure(mTransFunction, objectiveFunction);
	}

	/**
	 * 
	 * @param qFunctions
	 *            : QA functions
	 * @return Reward structures representing the QA functions
	 * @throws ActionDefinitionNotFoundException
	 * @throws ActionNotFoundException
	 * @throws VarNotFoundException
	 * @throws IncompatibleVarException
	 * @throws DiscriminantNotFoundException
	 * @throws AttributeNameNotFoundException
	 */
	public String getQAFunctionsTranslation(Set<IQFunction> qFunctions)
			throws ActionDefinitionNotFoundException, ActionNotFoundException, VarNotFoundException,
			IncompatibleVarException, DiscriminantNotFoundException, AttributeNameNotFoundException {
		return mRewardUtilities.buildRewardStructures(mTransFunction, qFunctions);
	}
}
