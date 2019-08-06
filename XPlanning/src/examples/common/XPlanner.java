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
import solver.prismconnector.PrismConnector;
import solver.prismconnector.PrismConnectorSettings;
import solver.prismconnector.exceptions.PrismConnectorException;
import solver.prismconnector.exceptions.ResultParsingException;
import uiconnector.ExplanationWriter;

public class XPlanner {

	private IXMDPLoader mXMDPLoader;
	private XPlannerOutDirectories mOutputDirs;
	private Vocabulary mVocabulary;

	public XPlanner(IXMDPLoader xmdpLoader, XPlannerOutDirectories outputDirs, Vocabulary vocabulary) {
		mXMDPLoader = xmdpLoader;
		mOutputDirs = outputDirs;
		mVocabulary = vocabulary;
	}

	public XMDP loadXMDPFromMissionFile(File missionJsonFile) throws DSMException, XMDPException {
		return mXMDPLoader.loadXMDP(missionJsonFile);
	}

	public PolicyInfo runXPlanning(File missionJsonFile, VerbalizerSettings verbalizerSettings)
			throws PrismException, IOException, XMDPException, PrismConnectorException, GRBException, DSMException {
		return runXPlanning(missionJsonFile, verbalizerSettings, null);
	}

	public PolicyInfo runXPlanning(File missionJsonFile, VerbalizerSettings verbalizerSettings,
			DifferenceScaler diffScaler)
			throws PrismException, IOException, XMDPException, PrismConnectorException, GRBException, DSMException {
		String missionName = FilenameUtils.removeExtension(missionJsonFile.getName());
		Path modelOutputPath = mOutputDirs.getPrismModelsOutputPath().resolve(missionName);
		Path advOutputPath = mOutputDirs.getPrismAdvsOutputPath().resolve(missionName);

		XMDP xmdp = loadXMDPFromMissionFile(missionJsonFile);

		PrismConnectorSettings prismConnSettings = new PrismConnectorSettings(modelOutputPath.toString(),
				advOutputPath.toString());
		PrismConnector prismConnector = new PrismConnector(xmdp, CostCriterion.TOTAL_COST, prismConnSettings);
		PolicyInfo policyInfo = prismConnector.generateOptimalPolicy();

		// Close down PRISM -- before explainer creates a new PrismConnector
		prismConnector.terminate();

		// ExplainerSettings define what DifferenceScaler to use, if any
		ExplainerSettings explainerSettings = new ExplainerSettings(prismConnSettings);
		explainerSettings.setDifferenceScaler(diffScaler);
		Explainer explainer = new Explainer(explainerSettings);
		Explanation explanation = explainer.explain(xmdp, CostCriterion.TOTAL_COST, policyInfo);

		Path policyJsonPath = mOutputDirs.getPoliciesOutputPath().resolve(missionName);
		Verbalizer verbalizer = new Verbalizer(mVocabulary, CostCriterion.TOTAL_COST, policyJsonPath.toFile(),
				verbalizerSettings);

		String explanationJsonFilename = String.format("%s_explanation.json", missionName);
		Path explanationOutputPath = mOutputDirs.getExplanationsOutputPath();
		ExplanationWriter explanationWriter = new ExplanationWriter(explanationOutputPath.toFile(), verbalizer);
		explanationWriter.writeExplanation(missionJsonFile.getName(), explanation, explanationJsonFilename);

		return policyInfo;
	}

	public PolicyInfo runPlanning(File missionJsonFile)
			throws DSMException, XMDPException, PrismException, IOException, ResultParsingException {
		String missionName = FilenameUtils.removeExtension(missionJsonFile.getName());
		Path modelOutputPath = mOutputDirs.getPrismModelsOutputPath().resolve(missionName);
		Path advOutputPath = mOutputDirs.getPrismAdvsOutputPath().resolve(missionName);

		XMDP xmdp = loadXMDPFromMissionFile(missionJsonFile);

		PrismConnectorSettings prismConnSetttings = new PrismConnectorSettings(modelOutputPath.toString(),
				advOutputPath.toString());
		PrismConnector prismConnector = new PrismConnector(xmdp, CostCriterion.TOTAL_COST, prismConnSetttings);
		PolicyInfo policyInfo = prismConnector.generateOptimalPolicy();

		// Close down PRISM
		prismConnector.terminate();

		return policyInfo;
	}

}
