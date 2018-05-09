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
import exceptions.ResultParsingException;
import exceptions.VarNotFoundException;
import mdp.XMDP;
import metrics.IQFunction;
import policy.Policy;
import prism.PrismException;
import prismconnector.PrismConnector;
import prismconnector.PrismDTMCTranslator;
import prismconnector.PrismPropertyTranslator;
import prismconnector.ValueEncodingScheme;

public class PolicyEvaluator {

	private XMDP mXMDP;
	private PrismConnector mConnector;

	public PolicyEvaluator(XMDP xmdp) throws PrismException {
		mXMDP = xmdp;
		mConnector = new PrismConnector();
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

	public double evaluatePolicyFromExplicitDTMC(String inputPath, String staInputFilename, String traInputFilename,
			String labInputFilename, String srewInputFilename, IQFunction qFunction, ValueEncodingScheme encodings,
			boolean threeParamRewards) throws VarNotFoundException, PrismException, ResultParsingException {
		PrismPropertyTranslator propertyTranslator = new PrismPropertyTranslator(encodings, threeParamRewards);
		String rawRewardQuery = propertyTranslator.buildDTMCRawRewardQueryProperty(mXMDP.getGoal());
		// TODO
		return mConnector.queryPropertyFromExplicitDTMC(rawRewardQuery, inputPath, staInputFilename, traInputFilename,
				labInputFilename, srewInputFilename);
	}
}
