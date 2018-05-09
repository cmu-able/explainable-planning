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
 * References: https://github.com/prismmodelchecker/prism-api/blob/master/src/demos/MDPAdversaryGeneration.java,
 * https://github.com/prismmodelchecker/prism/blob/master/prism/src/prism/Prism.java
 * 
 * @author rsukkerd
 *
 */
public class PrismConnector {

	private static final String FLOATING_POINT_PATTERN = "[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?";

	private Prism mPrism;

	public PrismConnector() throws PrismException {
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
	 * Generate an optimal adversary of a MDP in the form of an explicit model of DTMC. The output explicit model files
	 * include: states file (.sta), transitions file (.tra), labels file (.lab), and state rewards file (.srew).
	 * 
	 * @param mdpStr
	 * @param propertyStr
	 * @param outputPath
	 * @param staOutputFilename
	 * @param traOutputFilename
	 * @param labOutputFilename
	 * @param srewOutputFilename
	 * @return Expected cumulative cost of the generated optimal policy
	 * @throws PrismException
	 * @throws FileNotFoundException
	 * @throws ResultParsingException
	 */
	public double generateMDPAdversary(String mdpStr, String propertyStr, String outputPath, String staOutputFilename,
			String traOutputFilename, String labOutputFilename, String srewOutputFilename)
			throws PrismException, FileNotFoundException, ResultParsingException {
		// Parse and load a PRISM MDP model from a model string
		ModulesFile modulesFile = mPrism.parseModelString(mdpStr, ModelType.MDP);
		mPrism.loadPRISMModel(modulesFile);

		// Export the states of the model to a file
		mPrism.exportStatesToFile(Prism.EXPORT_PLAIN, new File(outputPath, staOutputFilename));

		// Export the labels (including "init" and "deadlock" -- these are important!) of the model to a file
		mPrism.exportLabelsToFile(null, Prism.EXPORT_PLAIN, new File(outputPath, labOutputFilename));

		// Export the reward structure to a file
		mPrism.exportStateRewardsToFile(Prism.EXPORT_PLAIN, new File(outputPath, srewOutputFilename));

		// Parse and load property from a property string
		PropertiesFile propertiesFile = mPrism.parsePropertiesString(modulesFile, propertyStr);

		// Configure PRISM to export an optimal adversary to a file when model checking an MDP
		mPrism.getSettings().set(PrismSettings.PRISM_EXPORT_ADV, "DTMC");
		mPrism.getSettings().set(PrismSettings.PRISM_EXPORT_ADV_FILENAME, outputPath + "/" + traOutputFilename);

		// Select PRISM engine
		// Engines by index: 0 -> MTBDD, 1 -> Sparse, 2 -> Hybrid, 3 -> Explicit
		// According to: https://github.com/prismmodelchecker/prism/blob/master/prism/src/prism/PrismSettings.java
		mPrism.getSettings().set(PrismSettings.PRISM_ENGINE, "Explicit");

		// Model check the first property
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

	/**
	 * Query quantitative property of a DTMC -- from the explicit model files.
	 * 
	 * @param propertyStr
	 * @param inputPath
	 * @param staInputFilename
	 * @param traInputFilename
	 * @param labInputFilename
	 * @param srewInputFilename
	 * @return Quantitative result of the given query property of the DTMC
	 * @throws PrismException
	 * @throws ResultParsingException
	 */
	public double queryPropertyFromExplicitDTMC(String propertyStr, String inputPath, String staInputFilename,
			String traInputFilename, String labInputFilename, String srewInputFilename)
			throws PrismException, ResultParsingException {
		File staFile = new File(inputPath, staInputFilename);
		File traFile = new File(inputPath, traInputFilename);
		File labFile = new File(inputPath, labInputFilename);
		File srewFile = new File(inputPath, srewInputFilename);

		// Load modules from .sta, .tra, .lab, and .srew files (.lab file contains at least "init" and "deadlock" labels
		// -- important!)
		ModulesFile modulesFile = mPrism.loadModelFromExplicitFiles(staFile, traFile, labFile, srewFile,
				ModelType.DTMC);

		// Parse and load property from a property string
		PropertiesFile propertiesFile = mPrism.parsePropertiesString(modulesFile, propertyStr);

		// Model check the first property
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

	/**
	 * Query quantitative property of a DTMC -- from the model and property strings.
	 * 
	 * @param dtmcModelStr
	 * @param propertyStr
	 * @return Quantitative result of the given query property of the DTMC
	 * @throws PrismException
	 * @throws ResultParsingException
	 */
	public double queryPropertyFromDTMC(String dtmcModelStr, String propertyStr)
			throws PrismException, ResultParsingException {
		// Parse and load a PRISM DTMC model from a model string
		ModulesFile modulesFile = mPrism.parseModelString(dtmcModelStr, ModelType.DTMC);
		mPrism.loadPRISMModel(modulesFile);

		// Parse and load property from a property string
		PropertiesFile propertiesFile = mPrism.parsePropertiesString(modulesFile, propertyStr);

		// Model check the first property
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
