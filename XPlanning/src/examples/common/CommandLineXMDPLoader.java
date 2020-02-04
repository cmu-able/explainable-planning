package examples.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Stream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class CommandLineXMDPLoader {

	private Options mOptions;
	private CommandLine mLine;

	public CommandLineXMDPLoader(Options options) {
		mOptions = options;
		mLine = null;
	}

	public void loadCommandLineFromFile(File problemFile) throws DSMException {
		String argsLine = null;

		try (FileReader fileReader = new FileReader(problemFile);
				BufferedReader buffReader = new BufferedReader(fileReader);) {
			argsLine = buffReader.readLine();
		} catch (IOException e) {
			throw new DSMException(e.getMessage());
		}

		String[] argsArray = argsLine.split(" ");
		CommandLineParser parser = new DefaultParser();

		try {
			mLine = parser.parse(mOptions, argsArray);
		} catch (ParseException e) {
			throw new DSMException(e.getMessage());
		}
	}

	public boolean getBooleanArgument(String argName) {
		return Boolean.parseBoolean(mLine.getOptionValue(argName));
	}

	public int getIntArgument(String argName) {
		return Integer.parseInt(mLine.getOptionValue(argName));
	}

	public double getDoubleArgument(String argName) {
		return Double.parseDouble(mLine.getOptionValue(argName));
	}

	public String getStringArgument(String argName) {
		return mLine.getOptionValue(argName);
	}

	public double[] getDoubleArrayArgument(String argName) {
		String[] numStrs = mLine.getOptionValue(argName).split(",");
		Stream<Double> streamDoubles = Arrays.stream(numStrs).map(Double::parseDouble);
		return streamDoubles.mapToDouble(Double::doubleValue).toArray();
	}
}
