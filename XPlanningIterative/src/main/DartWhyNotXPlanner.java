package main;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.apache.commons.io.FilenameUtils;
import org.json.simple.parser.ParseException;

import analysis.HPlanner;
import analysis.HPolicyExplainer;
import analysis.PolicyAnalyzer;
import examples.common.DSMException;
import examples.common.IXMDPLoader;
import examples.common.XPlanner;
import examples.common.XPlannerOutDirectories;
import examples.dart.demo.DartXMDPLoader;
import examples.dart.demo.DartXPlanner;
import explanation.analysis.PolicyInfo;
import explanation.verbalization.Verbalizer;
import explanation.verbalization.VerbalizerSettings;
import gurobi.GRBException;
import language.exceptions.XMDPException;
import language.mdp.XMDP;
import language.objectives.CostCriterion;
import language.policy.Policy;
import models.explanation.HPolicyExplanation;
import models.explanation.WhyNotQuery;
import models.hmodel.HModel;
import models.hmodel.HModelGenerator;
import models.hmodel.HPolicy;
import prism.PrismException;
import solver.prismconnector.PrismConnectorSettings;
import solver.prismconnector.exceptions.ExplicitModelParsingException;
import solver.prismconnector.exceptions.ResultParsingException;
import uiconnector.PolicyReader;
import verbalization.HPolicyVerbalizer;

public class DartWhyNotXPlanner {

	private IXMDPLoader mXMDPLoader;
	private XPlannerOutDirectories mOutputDirs;
	private VerbalizerSettings mVerbalizerSettings;

	public DartWhyNotXPlanner(XPlannerOutDirectories outputDirs, VerbalizerSettings verbalizerSettings) {
		mXMDPLoader = new DartXMDPLoader();
		mOutputDirs = outputDirs;
		mVerbalizerSettings = verbalizerSettings;
	}

	public void answerWhyNotQuery(File problemFile, File queryPolicyJsonFile, WhyNotQuery<?, ?> whyNotQuery)
			throws DSMException, XMDPException, IOException, ParseException, ResultParsingException, PrismException,
			ExplicitModelParsingException, GRBException {
		XMDP xmdp = mXMDPLoader.loadXMDP(problemFile);
		PrismConnectorSettings prismConnSettings = XPlanner.createPrismConnectorSettings(problemFile, mOutputDirs);

		PolicyAnalyzer policyAnalyzer = new PolicyAnalyzer(xmdp, CostCriterion.TOTAL_COST, prismConnSettings);
		HModelGenerator hModelGenerator = new HModelGenerator(policyAnalyzer);

		// Query policy
		PolicyReader policyReader = new PolicyReader(xmdp);
		Policy queryPolicy = policyReader.readPolicy(queryPolicyJsonFile);
		PolicyInfo queryPolicyInfo = policyAnalyzer.computePartialPolicyInfo(queryPolicy, xmdp.getInitialState());

		// HModel and HPlanner
		HModel<?> hModel = hModelGenerator.generateHModel(queryPolicy, whyNotQuery.getQueryState(),
				whyNotQuery.getQueryAction());
		HPlanner hPlanner = new HPlanner(CostCriterion.TOTAL_COST, prismConnSettings);

		// HPolicy and HPolicyExplainer
		HPolicy hPolicy = hPlanner.computeHPolicy(hModel, queryPolicy, whyNotQuery.getQueryQFunction());
		HPolicyExplainer hPolicyExplainer = new HPolicyExplainer(policyAnalyzer);
		HPolicyExplanation hPolicyExplanation = hPolicyExplainer.explainHPolicy(queryPolicyInfo, whyNotQuery, hPolicy);

		// HPolicyVerbalizer
		String problemName = FilenameUtils.removeExtension(problemFile.getName());
		Path policyJsonPath = mOutputDirs.getPoliciesOutputPath().resolve(problemName);
		Verbalizer verbalizer = new Verbalizer(DartXPlanner.getVocabulary(), CostCriterion.TOTAL_COST,
				policyJsonPath.toFile(), mVerbalizerSettings);
		HPolicyVerbalizer hPolicyVerbalizer = new HPolicyVerbalizer(verbalizer);

		String whyNotExplanationStr = hPolicyVerbalizer.verbalize(whyNotQuery, hPolicyExplanation);
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
