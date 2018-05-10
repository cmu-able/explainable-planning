package analysis;

import dtmc.XDTMC;
import exceptions.ActionDefinitionNotFoundException;
import exceptions.ActionNotFoundException;
import exceptions.AttributeNameNotFoundException;
import exceptions.DiscriminantNotFoundException;
import exceptions.EffectClassNotFoundException;
import exceptions.IncompatibleActionException;
import exceptions.IncompatibleDiscriminantClassException;
import exceptions.IncompatibleEffectClassException;
import exceptions.IncompatibleVarException;
import exceptions.QFunctionNotFoundException;
import exceptions.ResultParsingException;
import exceptions.VarNotFoundException;
import mdp.XMDP;
import metrics.IQFunction;
import policy.Policy;
import prism.PrismException;
import prismconnector.PrismAPIWrapper;
import prismconnector.PrismDTMCTranslator;
import prismconnector.PrismExplicitModelPointer;
import prismconnector.PrismPropertyTranslator;
import prismconnector.ValueEncodingScheme;

public class PolicyEvaluator {

	private XMDP mXMDP;
	private PrismAPIWrapper mConnector;

	public PolicyEvaluator(XMDP xmdp) throws PrismException {
		mXMDP = xmdp;
		mConnector = new PrismAPIWrapper();
	}

	public double evaluatePolicy(Policy policy, IQFunction qFunction)
			throws ActionDefinitionNotFoundException, EffectClassNotFoundException, VarNotFoundException,
			IncompatibleVarException, ActionNotFoundException, DiscriminantNotFoundException,
			IncompatibleActionException, IncompatibleEffectClassException, IncompatibleDiscriminantClassException,
			AttributeNameNotFoundException, PrismException, ResultParsingException {
		XDTMC xdtmc = new XDTMC(mXMDP, policy);
		PrismDTMCTranslator dtmcTranslator = new PrismDTMCTranslator(xdtmc, true);
		String dtmcWithQAs = dtmcTranslator.getDTMCTranslationWithQAs();
		String queryProperty = dtmcTranslator.getNumQueryPropertyTranslation(qFunction);
		return mConnector.queryPropertyFromDTMC(dtmcWithQAs, queryProperty);
	}

	public double evaluatePolicyFromExplicitDTMC(PrismExplicitModelPointer explicitModelPointer, IQFunction qFunction,
			ValueEncodingScheme encodings, boolean threeParamRewards)
			throws VarNotFoundException, PrismException, ResultParsingException, QFunctionNotFoundException {
		PrismPropertyTranslator propertyTranslator = new PrismPropertyTranslator(encodings, threeParamRewards);
		String rawRewardQuery = propertyTranslator.buildDTMCRawRewardQueryProperty(mXMDP.getGoal());
		Integer rewardStructIndex = encodings.getRewardStructureIndex(qFunction);
		return mConnector.queryPropertyFromExplicitDTMC(rawRewardQuery, explicitModelPointer, rewardStructIndex);
	}
}
