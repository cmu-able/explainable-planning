package models.hmodel;

import java.util.List;

import analysis.PolicyAnalyzer;
import explanation.analysis.PolicyInfo;
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
		// Create HModel for single-action query
		HModel<E> hModel = new HModel<>(mOriginalXMDP, queryState, queryAction);

		// Computer and put query QA value constraints for HModel
		computeQAValueConstraintsForHModel(hModel, queryPolicy, queryState);

		return hModel;
	}

	/**
	 * Generate HModel with a composite query action. A composite query action consists of a sequence of
	 * (re)configuration actions prior to the main query action (e.g., in mobilerobot, setSpeed before moveTo).
	 * 
	 * Assume that the pre-configuration actions themselves do not impact any QA directly (e.g., setSpeed action itself
	 * does not cost anything). But they can change how the subsequent main query action impact QAs.
	 * 
	 * @param queryPolicy
	 * @param queryState
	 *            : Query state
	 * @param queryAction
	 *            : Main query action
	 * @param preConfigActions
	 *            : Configuration actions to apply in the query state, prior to the main query action
	 * @return HModel with a composite query action
	 * @throws PrismException
	 * @throws XMDPException
	 * @throws ResultParsingException
	 */
	public <E extends IAction> HModel<E> generateHModel(Policy queryPolicy, StateVarTuple queryState, E queryAction,
			List<? extends IAction> preConfigActions) throws ResultParsingException, XMDPException, PrismException {
		// Create HModel for composite-action query
		HModel<E> hModel = new HModel<>(mOriginalXMDP, queryState, queryAction, preConfigActions);

		// Computer QA value constraints for HModel
		computeQAValueConstraintsForHModel(hModel, queryPolicy, queryState);

		return hModel;
	}

	private <E extends IAction> void computeQAValueConstraintsForHModel(HModel<E> hModel, Policy queryPolicy,
			StateVarTuple queryState) throws ResultParsingException, PrismException, XMDPException {
		// Compute QA values, costs, etc. of the original policy, starting from s_query onwards
		// This is for comparison to alternative policy satisfying the why-not query
		PolicyInfo originalPartialPolicyInfo = mPolicyAnalyzer.computePartialPolicyInfo(queryPolicy, queryState);

		for (StateVarTuple queryDestState : hModel.getAllDestStatesOfQuery()) {

			for (IQFunction<?, ?> qFunction : mOriginalXMDP.getQSpace()) {

				// Assume that pre-configuration actions don't cost anything

				// If this QA function has non-compatible action type with a_query,
				// then QA value of (s_query_final, a_query_final, s') is 0
				double oneStepQAValue = 0;

				StateVarTuple finalQueryState = hModel.getFinalQueryState();
				E finalQueryAction = hModel.getFinalQueryAction();

				if (checkCompatibleActionType(finalQueryAction, qFunction)) {
					// Compute 1-step QA value of a query transition (s_query_final, a_query_final, s')
					oneStepQAValue = computeOneStepQAValue(finalQueryState, finalQueryAction, queryDestState,
							qFunction);
				}

				// QA value constraint for alternative policy starting from s' onwards
				double queryQAValueConstraint = originalPartialPolicyInfo.getQAValue(qFunction) - oneStepQAValue;

				// Add each QA value constraint for state s' to HModel
				hModel.putQAValueConstraint(queryDestState, qFunction, queryQAValueConstraint);
			}
		}
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
		StateVarClass destStateVarClass = transStructure.getDestStateVarClass();
		StateVarTuple destVars = new StateVarTuple();
		destVars.addStateVarTupleWithFilter(queryDestState, destStateVarClass);

		// We already ensure that a_query has a compatible action type with the QA function
		// Cast up (e.g., incAlt -> durative action)
		E castedQueryAction = (E) queryAction;

		Transition<E, T> transition = new Transition<>(transStructure, castedQueryAction, srcVars, destVars);
		return qFunction.getValue(transition);
	}

}
