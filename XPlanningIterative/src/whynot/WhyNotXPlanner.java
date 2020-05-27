package whynot;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

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
import language.domain.models.IAction;
import language.exceptions.XMDPException;
import language.mdp.StateVarTuple;
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
import solver.prismconnector.exceptions.PrismConnectorException;
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
			String whyNotQueryStr) throws DSMException, XMDPException, PrismConnectorException, IOException,
			ParseException, PrismException, GRBException {
		XMDP xmdp = mXMDPLoader.loadXMDP(problemFile);

		WhyNotQueryReader queryReader = new WhyNotQueryReader(xmdp);
		WhyNotQuery<?, ?> whyNotQuery = queryReader.readWhyNotQuery(whyNotQueryStr);

		// HModelGenerator and PolicyAnalyzer
		PrismConnectorSettings prismConnSettings = XPlanner.createPrismConnectorSettings(problemFile, mOutputDirs);
		PolicyAnalyzer policyAnalyzer = new PolicyAnalyzer(xmdp, costCriterion, prismConnSettings);
		HModelGenerator hModelGenerator = new HModelGenerator(policyAnalyzer);

		// Query policy
		PolicyReader policyReader = new PolicyReader(xmdp);
		Policy queryPolicy = policyReader.readPolicy(queryPolicyJsonFile);
		PolicyInfo queryPolicyInfo = policyAnalyzer.computePartialPolicyInfo(queryPolicy, xmdp.getInitialState());

		StateVarTuple queryState = whyNotQuery.getQueryState();
		// Query actions: pre-configuration actions (possibly none) and main query action
		List<IAction> queryActions = whyNotQuery.getQueryActions();
		IAction queryAction = queryActions.get(queryActions.size() - 1);
		List<IAction> preConfigActions = queryActions.subList(0, queryActions.size() - 1);

		// HModel and HPlanner
		HModel<?> hModel = hModelGenerator.generateHModel(queryPolicy, queryState, queryAction, preConfigActions);
		HPlanner hPlanner = new HPlanner(costCriterion, prismConnSettings);

		// HPolicy and HPolicyExplainer
		HPolicy hPolicy = hPlanner.computeHPolicy(hModel, queryPolicy, whyNotQuery.getQueryQFunction());

		// Check if a solution for HPolicy exists
		if (!hPolicy.solutionExists()) {
			// This query forces goal states to be unreachable in HModel

			// Skip the HPolicy explanation steps below
			return null;
		}

		HPolicyExplainer hPolicyExplainer = new HPolicyExplainer(policyAnalyzer);
		HPolicyExplanation hPolicyExplanation = hPolicyExplainer.explainHPolicy(queryPolicyInfo, whyNotQuery, hPolicy);

		// Output HPolicyExplanation to file
		outputHPolicyExplanation(problemFile, costCriterion, whyNotQuery, hPolicyExplanation);

		return hPolicyExplanation;
	}

	private void outputHPolicyExplanation(File problemFile, CostCriterion costCriterion, WhyNotQuery<?, ?> whyNotQuery,
			HPolicyExplanation hPolicyExplanation) throws IOException {
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
		PolicyInfo queryPolicyInfo = hPolicyExplanation.getQueryPolicyInfo();
		PolicyInfo hPolicyInfo = hPolicyExplanation.getHPolicyInfo();

		// Check if the query action forces the agent to revisit a state prior to the query state
		// If so, we still produce HPolicy, but it will not contain the query state and action
		// Indicate this as a flag in explanation.json
		StateVarTuple queryState = whyNotQuery.getQueryState();
		HPolicy hPolicy = hPolicyExplanation.getHPolicy();
		boolean forcesRevisitPriorState = hPolicy.forcesRevisitPriorState(queryState);

		explanationWriter.addDefaultExplanationInfo(problemFile.getName(), queryPolicyInfo.getPolicy());
		explanationWriter.addPolicyEntry("HPolicy", hPolicyInfo.getPolicy());
		explanationWriter.addPolicyQAValues(queryPolicyInfo.getQuantitativePolicy());
		explanationWriter.addPolicyQAValues(hPolicyInfo.getQuantitativePolicy());
		explanationWriter.addCustomizedExplanationInfo("Why-Not Explanation", whyNotVerbalization);
		explanationWriter.addCustomizedExplanationInfo("HPolicy Tag", hPolicyExplanation.getHPolicyTag().toString());
		explanationWriter.addCustomizedExplanationInfo("Forces Revisit Prior State", forcesRevisitPriorState);
		explanationWriter.exportExplanationToFile(explanationJsonFilename);
	}

	public static XPlannerOutDirectories getDefaultXPlannerOutDirectories() throws IOException {
		Path policiesOutputPath = Paths.get(WhyNotXPlanner.POLICIES_OUTPUT_PATH);
		Path explanationOutputPath = Paths.get(WhyNotXPlanner.EXPLANATIONS_OUTPUT_PATH);
		Path prismOutputPath = Paths.get(WhyNotXPlanner.PRISM_OUTPUT_PATH);
		return new XPlannerOutDirectories(policiesOutputPath, explanationOutputPath, prismOutputPath);
	}

}
