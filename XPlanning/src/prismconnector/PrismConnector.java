package prismconnector;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import exceptions.ResultParsingException;
import parser.ast.ModulesFile;
import parser.ast.PropertiesFile;
import parser.ast.Property;
import prism.ModelType;
import prism.Prism;
import prism.PrismException;
import prism.PrismFileLog;
import prism.PrismLog;
import prism.PrismSettings;
import prism.Result;

/**
 * Reference: https://github.com/prismmodelchecker/prism-api/blob/master/src/demos/MDPAdversaryGeneration.java
 * 
 * @author rsukkerd
 *
 */
public class PrismConnector {

	private static final String FLOATING_POINT_PATTERN = "[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?";

	private String mModelPath;
	private String mOutputPath;
	private Prism mPrism;

	public PrismConnector(String modelPath, String outputDir) throws PrismException {
		mModelPath = modelPath;
		mOutputPath = modelPath + "/" + outputDir;
		initializePrism();
	}

	private void initializePrism() throws PrismException {
		// Create a log for PRISM output (stdout)
		PrismLog mainLog = new PrismFileLog("stdout");

		// Initialize PRISM engine
		mPrism = new Prism(mainLog);
		mPrism.initialise();
	}

	private void terminatePrism() {
		// Close down PRISM
		// NoClassDefFoundError: edu/jas/kern/ComputerThreads
		mPrism.closeDown();
	}

	/**
	 * 
	 * @param mdpFilename
	 * @param propFilename
	 * @param staOutputFilename
	 * @param labOutputFilename
	 * @param traOutputFilename
	 * @return Expected cumulative cost of the generated optimal policy
	 * @throws PrismException
	 * @throws FileNotFoundException
	 * @throws ResultParsingException
	 */
	public double generateMDPAdversary(String mdpFilename, String propFilename, String staOutputFilename,
			String labOutputFilename, String traOutputFilename, String srewOutputFilename)
			throws PrismException, FileNotFoundException, ResultParsingException {
		// Parse and load a PRISM model (an MDP) from a file
		ModulesFile modulesFile = mPrism.parseModelFile(new File(mModelPath, mdpFilename));
		mPrism.loadPRISMModel(modulesFile);

		// Export the states of the model to a file
		mPrism.exportStatesToFile(Prism.EXPORT_PLAIN, new File(mOutputPath, staOutputFilename));
		mPrism.exportLabelsToFile(null, Prism.EXPORT_PLAIN, new File(mOutputPath, labOutputFilename));

		// Export the reward structure to a file
		mPrism.exportStateRewardsToFile(Prism.EXPORT_PLAIN, new File(mOutputPath, srewOutputFilename));

		// Parse and load a properties model for the model
		PropertiesFile propertiesFile = mPrism.parsePropertiesFile(modulesFile, new File(mModelPath, propFilename));

		// Configure PRISM to export an optimal adversary to a file when model checking an MDP
		mPrism.getSettings().set(PrismSettings.PRISM_EXPORT_ADV, "DTMC");
		mPrism.getSettings().set(PrismSettings.PRISM_EXPORT_ADV_FILENAME, mOutputPath + "/" + traOutputFilename);

		// Select PRISM engine
		// Engines by index: 0 -> MTBDD, 1 -> Sparse, 2 -> Hybrid, 3 -> Explicit
		// According to: https://github.com/prismmodelchecker/prism/blob/master/prism/src/prism/PrismSettings.java
		mPrism.getSettings().set(PrismSettings.PRISM_ENGINE, "Explicit");

		// Model check the first property from the file
		Property property = propertiesFile.getPropertyObject(0);
		Result result = mPrism.modelCheck(propertiesFile, property);

		String resultStr = result.getResultString();
		Pattern p = Pattern.compile(FLOATING_POINT_PATTERN);
		Matcher m = p.matcher(resultStr);

		if (m.find()) {
			return Double.parseDouble(m.group(0));
		}

		// terminatePrism();

		throw new ResultParsingException(resultStr, FLOATING_POINT_PATTERN);
	}

	public double queryPropertyFromExplicitDTMC(String propertyStr, String staOutputFilename, String labOutputFilename,
			String traOutputFilename, String srewOutputFilename) throws PrismException, ResultParsingException {
		File staFile = new File(mOutputPath, staOutputFilename);
		File traFile = new File(mOutputPath, traOutputFilename);
		File labFile = new File(mOutputPath, labOutputFilename);
		File srewFile = new File(mOutputPath, srewOutputFilename);

		ModulesFile modulesFile = mPrism.loadModelFromExplicitFiles(staFile, traFile, labFile, srewFile,
				ModelType.DTMC);

		PropertiesFile propertiesFile = mPrism.parsePropertiesString(modulesFile, propertyStr);

		Property property = propertiesFile.getPropertyObject(0);
		Result result = mPrism.modelCheck(propertiesFile, property);

		String resultStr = result.getResultString();
		Pattern p = Pattern.compile(FLOATING_POINT_PATTERN);
		Matcher m = p.matcher(resultStr);

		if (m.find()) {
			return Double.parseDouble(m.group(0));
		}

		// terminatePrism();

		throw new ResultParsingException(resultStr, FLOATING_POINT_PATTERN);
	}
}
