package prismconnector;

import java.io.File;
import java.io.FileNotFoundException;

import parser.ast.ModulesFile;
import parser.ast.PropertiesFile;
import parser.ast.Property;
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

	private String mModelPath;

	public PrismConnector(String modelPath) {
		mModelPath = modelPath;
	}

	public String generateMDPAdversary(String mdpFilename, String propFilename, String staOutputFilename,
			String labOutputFilename, String traOutputFilename) throws PrismException, FileNotFoundException {
		// Create a log for PRISM output (stdout)
		PrismLog mainLog = new PrismFileLog("stdout");

		// Initialize PRISM engine
		Prism prism = new Prism(mainLog);
		prism.initialise();

		// Parse and load a PRISM model (an MDP) from a file
		ModulesFile modulesFile = prism.parseModelFile(new File(mModelPath, mdpFilename));
		prism.loadPRISMModel(modulesFile);

		// Export the states of the model to a file
		prism.exportStatesToFile(Prism.EXPORT_PLAIN, new File(mModelPath, staOutputFilename));
		prism.exportLabelsToFile(null, Prism.EXPORT_PLAIN, new File(mModelPath, labOutputFilename));

		// Parse and load a properties model for the model
		PropertiesFile propertiesFile = prism.parsePropertiesFile(modulesFile, new File(mModelPath, propFilename));

		// Configure PRISM to export an optimal adversary to a file when model checking an MDP
		prism.getSettings().set(PrismSettings.PRISM_EXPORT_ADV, "DTMC");
		prism.getSettings().set(PrismSettings.PRISM_EXPORT_ADV_FILENAME, mModelPath + "/" + traOutputFilename);

		// Select PRISM engine
		// Engines by index: 0 -> MTBDD, 1 -> Sparse, 2 -> Hybrid, 3 -> Explicit
		// According to: https://github.com/prismmodelchecker/prism/blob/master/prism/src/prism/PrismSettings.java
		prism.getSettings().set(PrismSettings.PRISM_ENGINE, "Explicit");

		// Model check the first property from the file
		Property property = propertiesFile.getPropertyObject(0);
		Result result = prism.modelCheck(propertiesFile, property);

		// Close down PRISM
		// NoClassDefFoundError: edu/jas/kern/ComputerThreads
		// prism.closeDown();

		return result.getResultString();
	}
}
