package examples.common;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.apache.commons.io.FilenameUtils;

import explanation.analysis.DifferenceScaler;
import explanation.analysis.Explainer;
import explanation.analysis.ExplainerSettings;
import explanation.analysis.Explanation;
import explanation.analysis.PolicyInfo;
import explanation.verbalization.Verbalizer;
import explanation.verbalization.VerbalizerSettings;
import explanation.verbalization.Vocabulary;
import gurobi.GRBException;
import language.exceptions.XMDPException;
import language.mdp.XMDP;
import language.objectives.CostCriterion;
import prism.PrismException;
import solver.gurobiconnector.GRBConnector;
import solver.gurobiconnector.GRBConnectorSettings;
import solver.prismconnector.PrismConnector;
import solver.prismconnector.PrismConnectorSettings;
import solver.prismconnector.ValueEncodingScheme;
import solver.prismconnector.exceptions.ExplicitModelParsingException;
import solver.prismconnector.exceptions.PrismConnectorException;
import solver.prismconnector.exceptions.ResultParsingException;
import solver.prismconnector.explicitmodel.PrismExplicitModelPointer;
import solver.prismconnector.explicitmodel.PrismExplicitModelReader;
import uiconnector.ExplanationWriter;

public class XPlanner {

	private IXMDPLoader mXMDPLoader;
	private XPlannerOutDirectories mOutputDirs;
	private Vocabulary mVocabulary;
	private VerbalizerSettings mVerbalizerSettings;

	public XPlanner(IXMDPLoader xmdpLoader, XPlannerOutDirectories outputDirs, Vocabulary vocabulary,
			VerbalizerSettings verbalizerSettings) {
		mXMDPLoader = xmdpLoader;
		mOutputDirs = outputDirs;
		mVocabulary = vocabulary;
		mVerbalizerSettings = verbalizerSettings;
	}

	public XMDP loadXMDPFromProblemFile(File problemFile) throws DSMException, XMDPException {
		return mXMDPLoader.loadXMDP(problemFile);
	}

	public PolicyInfo runXPlanning(File problemFile, CostCriterion costCriterion)
			throws PrismException, IOException, XMDPException, PrismConnectorException, GRBException, DSMException {
		return runXPlanning(problemFile, costCriterion, null);
	}

	public PolicyInfo runXPlanning(File problemFile, CostCriterion costCriterion, DifferenceScaler diffScaler)
			throws PrismException, IOException, XMDPException, PrismConnectorException, GRBException, DSMException {
		// Run regular planning
		PolicyInfo policyInfo = runPlanning(problemFile, costCriterion);

		PrismConnectorSettings prismConnSettings = createPrismConnectorSettings(problemFile, mOutputDirs);
		// ExplainerSettings define what DifferenceScaler to use, if any
		ExplainerSettings explainerSettings = new ExplainerSettings(prismConnSettings);
		explainerSettings.setDifferenceScaler(diffScaler);

		// Generate explanation for solution policy
		Explainer explainer = new Explainer(explainerSettings);
		Explanation explanation = explainer.explain(policyInfo.getXMDP(), costCriterion, policyInfo);

		String problemName = FilenameUtils.removeExtension(problemFile.getName());
		Path policyJsonPath = mOutputDirs.getPoliciesOutputPath().resolve(problemName);
		Verbalizer verbalizer = new Verbalizer(mVocabulary, costCriterion, policyJsonPath.toFile(),
				mVerbalizerSettings);

		// Write explanation to output file
		String explanationJsonFilename = String.format("%s_explanation.json", problemName);
		Path explanationOutputPath = mOutputDirs.getExplanationsOutputPath();
		ExplanationWriter explanationWriter = new ExplanationWriter(explanationOutputPath.toFile(), verbalizer);
		explanationWriter.writeExplanation(problemFile.getName(), explanation, explanationJsonFilename);

		return policyInfo;
	}

	public PolicyInfo runPlanning(File problemFile, CostCriterion costCriterion) throws DSMException, XMDPException,
			ExplicitModelParsingException, PrismException, IOException, GRBException, ResultParsingException {
		PrismConnectorSettings prismConnSettings = createPrismConnectorSettings(problemFile, mOutputDirs);
		XMDP xmdp = mXMDPLoader.loadXMDP(problemFile);

		if (costCriterion == CostCriterion.TOTAL_COST) {
			return runPlanningTotalCost(xmdp, prismConnSettings);
		} else if (costCriterion == CostCriterion.AVERAGE_COST) {
			return runPlanningAverageCost(xmdp, prismConnSettings);
		} else {
			throw new IllegalArgumentException(costCriterion.toString() + "is not supported");
		}
	}

	private PolicyInfo runPlanningTotalCost(XMDP xmdp, PrismConnectorSettings prismConnSettings)
			throws PrismException, ResultParsingException, XMDPException, IOException {
		// Use PrismConnector directly to generate optimal policy for a total-cost XMDP
		PrismConnector prismConnector = new PrismConnector(xmdp, CostCriterion.TOTAL_COST, prismConnSettings);
		PolicyInfo policyInfo = prismConnector.generateOptimalPolicy();

		// Close down PRISM
		prismConnector.terminate();

		return policyInfo;
	}

	private PolicyInfo runPlanningAverageCost(XMDP xmdp, PrismConnectorSettings prismConnSettings)
			throws PrismException, XMDPException, IOException, ExplicitModelParsingException, GRBException {
		// Use PrismConnector to export XMDP to explicit model files
		PrismConnector prismConnector = new PrismConnector(xmdp, CostCriterion.AVERAGE_COST, prismConnSettings);
		PrismExplicitModelPointer prismExplicitModelPtr = prismConnector.exportExplicitModelFiles();
		ValueEncodingScheme encodings = prismConnector.getPrismMDPTranslator().getValueEncodingScheme();
		PrismExplicitModelReader prismExplicitModelReader = new PrismExplicitModelReader(prismExplicitModelPtr,
				encodings);

		// Close down PRISM -- before Explainer creates a new PrismConnector
		prismConnector.terminate();

		// GRBConnector reads from explicit model files, and solves for optimal policy
		GRBConnectorSettings grbConnSettings = new GRBConnectorSettings(prismExplicitModelReader);
		GRBConnector grbConnector = new GRBConnector(xmdp, CostCriterion.AVERAGE_COST, grbConnSettings);
		return grbConnector.generateOptimalPolicy();
	}

	public static PrismConnectorSettings createPrismConnectorSettings(File problemFile,
			XPlannerOutDirectories outputDirs) {
		String problemName = FilenameUtils.removeExtension(problemFile.getName());
		Path modelOutputPath = outputDirs.getPrismModelsOutputPath().resolve(problemName);
		Path advOutputPath = outputDirs.getPrismAdvsOutputPath().resolve(problemName);
		return new PrismConnectorSettings(modelOutputPath.toString(), advOutputPath.toString());
	}

}
