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
import explanation.analysis.PolicyInfo;
import explanation.verbalization.Verbalizer;
import explanation.verbalization.VerbalizerSettings;
import explanation.verbalization.Vocabulary;
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
import uiconnector.ExplanationWriter;
import uiconnector.PolicyReader;
import verbalization.HPolicyVerbalizer;

public class WhyNotXPlanner {

	private static final String XPLANNER_ITER_OUTPUT_PATH = "/Users/rsukkerd/Projects/explainable-planning/XPlanningIterative/output";

	public static final String POLICIES_OUTPUT_PATH = XPLANNER_ITER_OUTPUT_PATH + "/policies";
	public static final String EXPLANATIONS_OUTPUT_PATH = XPLANNER_ITER_OUTPUT_PATH + "/explanations";
	public static final String PRISM_OUTPUT_PATH = XPLANNER_ITER_OUTPUT_PATH + "/prism";

	private IXMDPLoader mXMDPLoader;
	private XPlannerOutDirectories mOutputDirs;
	private Vocabulary mVocabulary;
	private VerbalizerSettings mVerbalizerSettings;

	public WhyNotXPlanner(IXMDPLoader xmdpLoader, XPlannerOutDirectories outputDirs, Vocabulary vocabulary,
			VerbalizerSettings verbalizerSettings) {
		mXMDPLoader = xmdpLoader;
		mOutputDirs = outputDirs;
		mVocabulary = vocabulary;
		mVerbalizerSettings = verbalizerSettings;
	}

	public HPolicyExplanation answerWhyNotQuery(File problemFile, CostCriterion costCriterion, File queryPolicyJsonFile,
			WhyNotQuery<?, ?> whyNotQuery) throws DSMException, XMDPException, IOException, ParseException,
			ResultParsingException, PrismException, ExplicitModelParsingException, GRBException {
		XMDP xmdp = mXMDPLoader.loadXMDP(problemFile);

		// HModelGenerator and PolicyAnalyzer
		PrismConnectorSettings prismConnSettings = XPlanner.createPrismConnectorSettings(problemFile, mOutputDirs);
		PolicyAnalyzer policyAnalyzer = new PolicyAnalyzer(xmdp, costCriterion, prismConnSettings);
		HModelGenerator hModelGenerator = new HModelGenerator(policyAnalyzer);

		// Query policy
		PolicyReader policyReader = new PolicyReader(xmdp);
		Policy queryPolicy = policyReader.readPolicy(queryPolicyJsonFile);
		PolicyInfo queryPolicyInfo = policyAnalyzer.computePartialPolicyInfo(queryPolicy, xmdp.getInitialState());

		// HModel and HPlanner
		HModel<?> hModel = hModelGenerator.generateHModel(queryPolicy, whyNotQuery.getQueryState(),
				whyNotQuery.getQueryAction());
		HPlanner hPlanner = new HPlanner(costCriterion, prismConnSettings);

		// HPolicy and HPolicyExplainer
		HPolicy hPolicy = hPlanner.computeHPolicy(hModel, queryPolicy, whyNotQuery.getQueryQFunction());
		PolicyInfo hPolicyInfo = policyAnalyzer.computeHPolicyInfo(hPolicy);
		HPolicyExplainer hPolicyExplainer = new HPolicyExplainer(policyAnalyzer);
		HPolicyExplanation hPolicyExplanation = hPolicyExplainer.explainHPolicy(queryPolicyInfo, whyNotQuery, hPolicy);

		// HPolicyVerbalizer
		String problemName = FilenameUtils.removeExtension(problemFile.getName());
		Path policyJsonPath = mOutputDirs.getPoliciesOutputPath().resolve(problemName);
		Verbalizer verbalizer = new Verbalizer(mVocabulary, costCriterion, policyJsonPath.toFile(),
				mVerbalizerSettings);
		HPolicyVerbalizer hPolicyVerbalizer = new HPolicyVerbalizer(verbalizer);

		// Create verbalization of why-not explanation, including exporting policies as .json files
		String whyNotVerbalization = hPolicyVerbalizer.verbalize(whyNotQuery, hPolicyExplanation);

		// Write why-not explanation to output file
		String explanationJsonFilename = String.format("%s_explanation.json", problemName);
		Path explanationOutputPath = mOutputDirs.getExplanationsOutputPath();
		ExplanationWriter explanationWriter = new ExplanationWriter(explanationOutputPath.toFile(), verbalizer);

		// Customized why-not explanation content
		explanationWriter.addDefaultExplanationInfo(problemFile.getName(), queryPolicy);
		explanationWriter.addPolicyEntry("HPolicy", hPolicyInfo.getPolicy());
		explanationWriter.addPolicyQAValues(queryPolicyInfo.getQuantitativePolicy());
		explanationWriter.addPolicyQAValues(hPolicyInfo.getQuantitativePolicy());
		explanationWriter.addCustomizedExplanationInfo("Why-Not Explanation", whyNotVerbalization);
		explanationWriter.addCustomizedExplanationInfo("HPolicy Tag", hPolicyExplanation.getHPolicyTag());
		explanationWriter.exportExplanationToFile(explanationJsonFilename);

		return hPolicyExplanation;
	}
}
