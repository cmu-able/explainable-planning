package prismconnector;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
import mdp.State;
import mdp.XMDP;
import policy.Policy;
import prism.PrismException;

public class PrismConnector {

	private PrismAPIWrapper mPrismAPI;
	private Map<Policy, Double> mCachedTotalCosts = new HashMap<>();

	public PrismConnector() throws PrismException {
		mPrismAPI = new PrismAPIWrapper();
	}

	public Policy generateOptimalPolicy(XMDP xmdp)
			throws VarNotFoundException, EffectClassNotFoundException, AttributeNameNotFoundException,
			IncompatibleVarException, DiscriminantNotFoundException, ActionNotFoundException,
			IncompatibleActionException, IncompatibleEffectClassException, IncompatibleDiscriminantClassException,
			ActionDefinitionNotFoundException, PrismException, ResultParsingException, IOException {
		PrismMDPTranslator mdpTranslator = new PrismMDPTranslator(xmdp, true);
		String mdpWithQAs = mdpTranslator.getMDPTranslationWithQAs();
		String goalProperty = mdpTranslator.getGoalPropertyTranslation();

		String outputPath = "/Users/rsukkerd/Projects/explainable-planning/models/test0/output";
		String staOutputFilename = "adv.sta";
		String traOutputFilename = "adv.tra";
		String labOutputFilename = "adv.lab";
		String srewOutputFilename = "adv.srew";
		// Reward structures include 1 for the cost function and 1 for each of the QA functions
		// Because mdpTranslator.getMDPTranslationWithQAs() is used
		int numRewardStructs = xmdp.getQFunctions().size() + 1;
		PrismExplicitModelPointer outputExplicitModelPointer = new PrismExplicitModelPointer(outputPath,
				staOutputFilename, traOutputFilename, labOutputFilename, srewOutputFilename, numRewardStructs);

		double totalCost = mPrismAPI.generateMDPAdversary(mdpWithQAs, goalProperty, outputExplicitModelPointer);

		PrismExplicitModelReader explicitModelReader = new PrismExplicitModelReader(
				mdpTranslator.getValueEncodingScheme(), outputPath);
		Map<Integer, State> stateIndices = explicitModelReader.readStatesFromFile(staOutputFilename);
		Policy policy = explicitModelReader.readPolicyFromFile(traOutputFilename, stateIndices);
		mCachedTotalCosts.put(policy, totalCost);
		return policy;
	}

	public double getExpectedTotalCost(Policy policy) {
		return mCachedTotalCosts.get(policy);
	}
}
