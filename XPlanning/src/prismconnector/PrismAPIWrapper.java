package prismconnector;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class PrismAPIWrapper {

	private static final String FLOATING_POINT_PATTERN = "[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?";
	private static final String INFINITY_PATERN = "[iI]nfinity";

	private Prism mPrism;

	public PrismAPIWrapper() throws PrismException {
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
		// mPrism.closeDown();
	}

	/**
	 * Generate an optimal adversary of a MDP in the form of an explicit model of DTMC. The output explicit model files
	 * include: states file (.sta), transitions file (.tra), labels file (.lab), and state rewards file (.srew).
	 * 
	 * @param mdpStr
	 *            : MDP translation
	 * @param propertyStr
	 *            : Property containing a function minimization (which can be the cost function or other objective
	 *            function), and optionally a constraint
	 * @param outputPath
	 *            : Output directory for the explicit model files
	 * @return Expected total objective value of the generated optimal policy. If the value is infinity, it means that
	 *         the probability of the policy reaching the goal is < 1.
	 * @throws PrismException
	 * @throws FileNotFoundException
	 * @throws ResultParsingException
	 */
	public double generateMDPAdversary(String mdpStr, String propertyStr,
			PrismExplicitModelPointer outputExplicitModelPointer)
			throws PrismException, FileNotFoundException, ResultParsingException {
		File staOutputFile = outputExplicitModelPointer.getStatesFile();
		File traOutputFile = outputExplicitModelPointer.getTransitionsFile();
		File labOutputFile = outputExplicitModelPointer.getLabelsFile();
		File srewOutputFile = outputExplicitModelPointer.getStateRewardsFile();

		// Parse and load a PRISM MDP model from a model string
		ModulesFile modulesFile = mPrism.parseModelString(mdpStr, ModelType.MDP);
		mPrism.loadPRISMModel(modulesFile);

		// Export the states of the model to a file
		mPrism.exportStatesToFile(Prism.EXPORT_PLAIN, staOutputFile);

		// Export the labels (including "init" and "deadlock" -- these are important!) of the model to a file
		mPrism.exportLabelsToFile(null, Prism.EXPORT_PLAIN, labOutputFile);

		// Export the reward structure to a file
		mPrism.exportStateRewardsToFile(Prism.EXPORT_PLAIN, srewOutputFile);

		// Configure PRISM to export an optimal adversary to a file when model checking an MDP
		mPrism.getSettings().set(PrismSettings.PRISM_EXPORT_ADV, "DTMC");
		mPrism.getSettings().set(PrismSettings.PRISM_EXPORT_ADV_FILENAME, traOutputFile.getPath());

		// Select PRISM engine
		// Engines by index: 0 -> MTBDD, 1 -> Sparse, 2 -> Hybrid, 3 -> Explicit
		// According to: https://github.com/prismmodelchecker/prism/blob/master/prism/src/prism/PrismSettings.java
		mPrism.getSettings().set(PrismSettings.PRISM_ENGINE, "Explicit");

		double result = queryPropertyHelper(modulesFile, propertyStr, 0);
		terminatePrism();
		return result;
	}

	/**
	 * Query quantitative property of a DTMC -- from the explicit model files.
	 * 
	 * @param rawRewardPropertyStr
	 *            : Raw reward query property
	 * @param explicitModelPointer
	 *            : Pointer to the explicit model
	 * @param rewardStructIndex
	 *            : Index of the reward structure representing the quantity to be queried
	 * @return Quantitative result of the given query property of the DTMC
	 * @throws PrismException
	 * @throws ResultParsingException
	 */
	public double queryPropertyFromExplicitDTMC(String rawRewardPropertyStr,
			PrismExplicitModelPointer explicitModelPointer, int rewardStructIndex)
			throws PrismException, ResultParsingException {
		File staFile = explicitModelPointer.getStatesFile();
		File traFile = explicitModelPointer.getTransitionsFile();
		File labFile = explicitModelPointer.getLabelsFile();
		File srewFile = explicitModelPointer.getIndexedStateRewardsFile(rewardStructIndex);

		// Load modules from .sta, .tra, .lab, and .srew files (.lab file contains at least "init" and "deadlock" labels
		// -- important!)
		ModulesFile modulesFile = mPrism.loadModelFromExplicitFiles(staFile, traFile, labFile, srewFile,
				ModelType.DTMC);

		double result = queryPropertyHelper(modulesFile, rawRewardPropertyStr, 0);
		terminatePrism();
		return result;
	}

	/**
	 * Query multiple quantitative properties of a DTMC -- from the explicit model files.
	 * 
	 * @param rawRewardPropertyStr
	 *            : Raw reward query property
	 * @param explicitModelPointer
	 *            : Pointer to the explicit model
	 * @return List of the results in the order of the .srew input filenames
	 * @throws PrismException
	 * @throws ResultParsingException
	 */
	public List<Double> queryPropertiesFromExplicitDTMC(String rawRewardPropertyStr,
			PrismExplicitModelPointer explicitModelPointer) throws PrismException, ResultParsingException {
		List<Double> results = new ArrayList<>();
		for (int i = 1; i <= explicitModelPointer.getNumRewardStructs(); i++) {
			double result = queryPropertyFromExplicitDTMC(rawRewardPropertyStr, explicitModelPointer, i);
			results.add(result);
		}
		return results;
	}

	/**
	 * Query quantitative property of a DTMC -- from the model and property strings.
	 * 
	 * @param dtmcModelStr
	 *            : DTMC translation
	 * @param propertyStr
	 *            : Single property to be queried
	 * @return Quantitative result of the given query property of the DTMC
	 * @throws PrismException
	 * @throws ResultParsingException
	 */
	public double queryPropertyFromDTMC(String dtmcModelStr, String propertyStr)
			throws PrismException, ResultParsingException {
		// Parse and load a PRISM DTMC model from a model string
		ModulesFile modulesFile = mPrism.parseModelString(dtmcModelStr, ModelType.DTMC);
		mPrism.loadPRISMModel(modulesFile);

		double result = queryPropertyHelper(modulesFile, propertyStr, 0);
		terminatePrism();
		return result;
	}

	/**
	 * Query multiple quantitative properties of a DTMC -- from the model and properties strings.
	 * 
	 * @param dtmcModelStr
	 *            : DTMC translation
	 * @param propertiesStr
	 *            : Multiple properties to be queried
	 * @return Mapping from each property to the result
	 * @throws PrismException
	 * @throws ResultParsingException
	 */
	public Map<String, Double> queryPropertiesFromDTMC(String dtmcModelStr, String propertiesStr)
			throws PrismException, ResultParsingException {
		// Parse and load a PRISM DTMC model from a model string
		ModulesFile modulesFile = mPrism.parseModelString(dtmcModelStr, ModelType.DTMC);
		mPrism.loadPRISMModel(modulesFile);

		Map<String, Double> results = queryPropertiesHelper(modulesFile, propertiesStr);
		terminatePrism();
		return results;
	}

	private Map<String, Double> queryPropertiesHelper(ModulesFile modulesFile, String propertiesStr)
			throws PrismException, ResultParsingException {
		// Parse and load property from a property string
		PropertiesFile propertiesFile = mPrism.parsePropertiesString(modulesFile, propertiesStr);

		// Get number of properties
		int numProperties = propertiesFile.getNumProperties();

		Map<String, Double> results = new HashMap<>();

		// Query result of each property
		for (int i = 0; i < numProperties; i++) {
			String propertyStr = propertiesFile.getPropertyObject(i).toString();
			double result = queryPropertyHelper(propertiesFile, i);
			results.put(propertyStr, result);
		}

		return results;
	}

	private double queryPropertyHelper(ModulesFile modulesFile, String propertiesStr, int propertyIndex)
			throws PrismException, ResultParsingException {
		// Parse and load property from a property string
		PropertiesFile propertiesFile = mPrism.parsePropertiesString(modulesFile, propertiesStr);
		return queryPropertyHelper(propertiesFile, propertyIndex);
	}

	private double queryPropertyHelper(PropertiesFile propertiesFile, int propertyIndex)
			throws PrismException, ResultParsingException {
		// Model check the property at the given index
		Property property = propertiesFile.getPropertyObject(propertyIndex);
		Result result = mPrism.modelCheck(propertiesFile, property);

		// Parse result double from result string
		// The result string may be a floating-point value or "Infinity"
		String resultStr = result.getResultString();
		Pattern valuePattern = Pattern.compile(FLOATING_POINT_PATTERN);
		Pattern infinityPattern = Pattern.compile(INFINITY_PATERN);
		Matcher valueMatcher = valuePattern.matcher(resultStr);
		Matcher infinityMatcher = infinityPattern.matcher(resultStr);

		if (valueMatcher.find()) {
			return Double.parseDouble(valueMatcher.group(0));
		}
		if (infinityMatcher.find()) {
			return Double.POSITIVE_INFINITY;
		}
		throw new ResultParsingException(resultStr, FLOATING_POINT_PATTERN);
	}
}
