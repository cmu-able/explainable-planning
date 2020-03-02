package models.hmodel;

import analysis.PolicyAnalyzer;
import language.domain.metrics.IQFunction;
import language.domain.metrics.ITransitionStructure;
import language.domain.metrics.Transition;
import language.domain.models.ActionDefinition;
import language.domain.models.IAction;
import language.exceptions.XMDPException;
import language.mdp.StateVarClass;
import language.mdp.StateVarTuple;
import language.mdp.XMDP;
import language.policy.Policy;
import prism.PrismException;
import solver.prismconnector.exceptions.ResultParsingException;

public class HModelGenerator {

	private XMDP mOriginalXMDP;
	private PolicyAnalyzer mPolicyAnalyzer;

	public HModelGenerator(PolicyAnalyzer policyAnalyzer) {
		mOriginalXMDP = policyAnalyzer.getXMDP();
		mPolicyAnalyzer = policyAnalyzer;
	}

	public <E extends IAction> HModel<E> generateHModel(Policy queryPolicy, StateVarTuple queryState, E queryAction)
			throws XMDPException, ResultParsingException, PrismException {
		HModel<E> hModel = new HModel<>(mOriginalXMDP, queryState, queryAction);

		for (StateVarTuple queryDestState : hModel.getAllDestStatesOfQuery()) {

			// Compute QA values, costs, etc. of the original policy, starting from s_query onwards
			// This is for comparison to alternative policy satisfying the why-not query
			PartialPolicyInfo originalPartialPolicyInfo = mPolicyAnalyzer.computePartialPolicyInfo(queryPolicy,
					queryState);

			for (IQFunction<?, ?> qFunction : mOriginalXMDP.getQSpace()) {

				// If this QA function has non-compatible action type with a_query,
				// then QA value of (s_query, a_query, s') is 0
				double oneStepQAValue = 0;

				if (checkCompatibleActionType(queryAction, qFunction)) {
					// Compute 1-step QA value of a query transition (s_query, a_query, s')
					oneStepQAValue = computeOneStepQAValue(hModel.getQueryState(), hModel.getQueryAction(),
							queryDestState, qFunction);
				}

				// QA value constraint for alternative policy starting from s' onwards
				double queryQAValueConstraint = originalPartialPolicyInfo.getPartialQAValue(qFunction) - oneStepQAValue;

				// Add each QA value constraint for state s' to HModel
				hModel.putQueryQAValueConstraint(queryDestState, qFunction, queryQAValueConstraint);
			}
		}

		return hModel;
	}

	/**
	 * 
	 * @param queryAction
	 *            : Query action
	 * @param qFunction
	 *            : QA function
	 * @return Query action has a compatible action type with QA function if they have the same action definition, or if
	 *         the parent composite action definition of the query action is the same as the action definition of the QA
	 *         function
	 */
	private <F extends IAction, E extends IAction, T extends ITransitionStructure<E>> boolean checkCompatibleActionType(
			F queryAction, IQFunction<E, T> qFunction) {
		ActionDefinition<F> queryActionDef = mOriginalXMDP.getActionSpace().getActionDefinition(queryAction);
		// Parent composite action definition of a_query
		ActionDefinition<IAction> parentCompActionDef = queryActionDef.getParentCompositeActionDefinition();

		ActionDefinition<E> qFuncActionDef = qFunction.getTransitionStructure().getActionDef();

		return queryActionDef.equals(qFuncActionDef)
				|| (parentCompActionDef != null && parentCompActionDef.equals(qFuncActionDef));
	}

	private <E extends IAction, T extends ITransitionStructure<E>> double computeOneStepQAValue(
			StateVarTuple queryState, IAction queryAction, StateVarTuple queryDestState, IQFunction<E, T> qFunction)
			throws XMDPException {
		T transStructure = qFunction.getTransitionStructure();

		// Relevant source state variables in query state
		StateVarClass srcStateVarClass = transStructure.getSrcStateVarClass();
		StateVarTuple srcVars = new StateVarTuple();
		srcVars.addStateVarTupleWithFilter(queryState, srcStateVarClass);

		// Relevant destination state variables in resulting state of query
		StateVarClass destStateVarClass = transStructure.getSrcStateVarClass();
		StateVarTuple destVars = new StateVarTuple();
		destVars.addStateVarTupleWithFilter(queryDestState, destStateVarClass);

		// We already ensure that a_query has a compatible action type with the QA function
		// Cast up (e.g., incAlt -> durative action)
		E castedQueryAction = (E) queryAction;

		Transition<E, T> transition = new Transition<>(transStructure, castedQueryAction, srcVars, destVars);
		return qFunction.getValue(transition);
	}

}
