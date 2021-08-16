package examples.common;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

public class PlannerArguments {

	public static final String PROBLEM_FILES_PATH_PROP = "Problems.Path";
	public static final String PROBLEM_FILE_PROP = "Problem";

	public static Properties parsePlanningCommandLineArguments(String prog, String[] args) {
		Properties props = new Properties();

		ArgumentParser parser = createArgumentParser(prog);
		try {
			Namespace arguments = parser.parseArgs(args);
			List<Object> propFile = arguments.getList("props");
			if (propFile != null) {
				for (Object f : propFile) {
					try {
						props.load(new FileInputStream((String) f));
					} catch (FileNotFoundException e) {
						ArgumentParserException pe = new ArgumentParserException("File '" + propFile + "' not found", e,
								parser);
						throw pe;

					} catch (IOException e) {
						ArgumentParserException pe = new ArgumentParserException("Error loading '" + propFile, e,
								parser);
						throw pe;
					}
				}
			}
			updatePropertyWithArgument(PROBLEM_FILES_PATH_PROP, arguments, parser, props);
			updatePropertyWithArgument(XPlannerOutDirectories.EXPLANATIONS_OUTPUT_PATH_PROP, arguments, parser, props);
			updatePropertyWithArgument(XPlannerOutDirectories.POLICIES_OUTPUT_PATH_PROP, arguments, parser, props);
			updatePropertyWithArgument(XPlannerOutDirectories.PRISM_ADVS_OUTPUT_PATH_PROP, arguments, parser, props);
			updatePropertyWithArgument(XPlannerOutDirectories.PRISM_MODELS_OUTPUT_PATH_PROP, arguments, parser, props);
			String problemFile = arguments.getString("problem");
			props.setProperty(PROBLEM_FILE_PROP, problemFile);
			Path problemPath = Paths.get(props.getProperty(PROBLEM_FILES_PATH_PROP), problemFile);
			if (!Files.isRegularFile(problemPath)) {
				ArgumentParserException pe = new ArgumentParserException("Could not find file '" + problemPath.toString() + "'", parser);
				throw pe;
				
			}
			Integer ll = arguments.getInt("linelength");
			if (ll != null) {
				props.put("consoleWidth", ll);
			}
			return props;
		} catch (ArgumentParserException e) {
			parser.handleError(e);
			return null;
		}
	}

	private static void updatePropertyWithArgument(String propArg, Namespace arguments, ArgumentParser parser,
			Properties props) throws ArgumentParserException {
		String property = arguments.getString(propArg);
		if (property != null) {
			props.setProperty(propArg, property);
		} else if (!props.containsKey(propArg)) {
			ArgumentParserException pe = new ArgumentParserException(
					"'" + propArg + "' not found in properties file or on command line.", parser);
			throw pe;
		}
		String value = props.getProperty(propArg);
		if (!Files.isDirectory(Paths.get(value))) {
			ArgumentParserException pe = new ArgumentParserException("'" + value + "' is not a valid directory.",
					parser);
			throw pe;
		}
	}

	private static ArgumentParser createArgumentParser(String prog) {
		ArgumentParser parser = ArgumentParsers.newFor(prog).build();
		parser.addArgument("--props").type(String.class).action(Arguments.append()).required(false)
				.help("The list of names of the properties file that specifies directory locations");
		parser.addArgument("--Problems.Path").type(String.class).required(false)
				.help("The directory to find problems -- overrides props");
		parser.addArgument("--" + XPlannerOutDirectories.EXPLANATIONS_OUTPUT_PATH_PROP).type(String.class).required(false)
				.help("The directory to place explanations - overrides props");
		parser.addArgument("--" + XPlannerOutDirectories.POLICIES_OUTPUT_PATH_PROP).type(String.class).required(false)
				.help("The directory to output PRISM polices -- overrides props");
		parser.addArgument("--" + XPlannerOutDirectories.PRISM_ADVS_OUTPUT_PATH_PROP).type(String.class).required(false)
				.help("The directory to output PRISM adv files -- overrides props");
		parser.addArgument("--" + XPlannerOutDirectories.PRISM_MODELS_OUTPUT_PATH_PROP).type(String.class).required(false)
				.help("The directory to output PRISM models -- overrides props");
		parser.addArgument("--linelength").type(Integer.class).required(false).help("The line length to use for explanation");
		parser.addArgument("problem").type(String.class).required(true).help("The problem to be explained");
		return parser;
	}

}
