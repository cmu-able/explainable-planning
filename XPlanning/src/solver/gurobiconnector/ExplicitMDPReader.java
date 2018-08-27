package solver.gurobiconnector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import solver.prismconnector.PrismExplicitModelPointer;
import solver.prismconnector.PrismRewardType;

public class ExplicitMDPReader {

	private PrismExplicitModelPointer mPrismModelPointer;

	public ExplicitMDPReader(PrismExplicitModelPointer prismExplicitModelPointer) {
		mPrismModelPointer = prismExplicitModelPointer;
	}

	public ExplicitMDP readExplicitMDP() throws IOException {
		File traFile = mPrismModelPointer.getTransitionsFile();
		List<String> traAllLines = readLinesFromFile(traFile);
		String traHeader = readFirstLineFromFile(traFile);

		int numStates = readNumStates(traHeader);
		Set<String> actionNames = readActionNames(traAllLines);
		int numActions = actionNames.size();
		CostType costType = mPrismModelPointer.getPrismRewardType() == PrismRewardType.STATE_REWARD
				? CostType.STATE_COST
				: CostType.TRANSITION_COST;
		ExplicitMDP explicitMDP = new ExplicitMDP(numStates, numActions, actionNames, costType);
		readTransitionProbabilities(traAllLines, explicitMDP);

		if (costType == CostType.TRANSITION_COST) {
			String[][] choicesToActions = readChoicesToActions(traAllLines, numStates, numActions);
			File trewFile = mPrismModelPointer.getTransitionRewardsFile();
			List<String> trewAllLines = readLinesFromFile(trewFile);
			readTransitionCosts(trewAllLines, choicesToActions, explicitMDP);
		} else if (costType == CostType.STATE_COST) {
			File srewFile = mPrismModelPointer.getStateRewardsFile();
			List<String> srewAllLines = readLinesFromFile(srewFile);
			readStateCosts(srewAllLines, explicitMDP);
		}
		return explicitMDP;
	}

	/**
	 * Read the number of states from a header of .tra file: "{#states} {#choices} {#transitions}".
	 * 
	 * @param traHeader
	 *            : First line of .tra file
	 * @return Number of states
	 */
	private int readNumStates(String traHeader) {
		String[] headerArray = traHeader.split(" ");
		return Integer.parseInt(headerArray[0]);
	}

	/**
	 * Read all of the action names from .tra file.
	 * 
	 * Each line has the format: "{src} {prob}:{dest} {prob}:{dest} ... {action name}". Assume that every command in
	 * PRISM MDP model has an action label.
	 * 
	 * @param traAllLines
	 *            : All lines from .tra file
	 * @return All action names
	 */
	private Set<String> readActionNames(List<String> traAllLines) {
		List<String> body = traAllLines.subList(1, traAllLines.size());
		Set<String> actionNames = new HashSet<>();
		for (String line : body) {
			// Line format: "{src} {prob}:{dest} {prob}:{dest} ... {action name}"
			String[] tokens = line.split(" ");
			String actionName = tokens[tokens.length - 1];
			actionNames.add(actionName);
		}
		return actionNames;
	}

	/**
	 * Read transition probabilities from .tra file.
	 * 
	 * Each line has the format: "{src} {prob}:{dest} {prob}:{dest} ... {action name}". Assume that every command in
	 * PRISM MDP model has an action label.
	 * 
	 * @param traAllLines
	 *            : All lines from .tra file
	 * @param explicitMDP
	 *            : Add probabilistic transitions to this explicit MDP
	 */
	private void readTransitionProbabilities(List<String> traAllLines, ExplicitMDP explicitMDP) {
		List<String> body = traAllLines.subList(1, traAllLines.size());
		for (String line : body) {
			// Line format: "{src} {prob}:{dest} {prob}:{dest} ... {action name}"
			String[] tokens = line.split(" ");
			int srcState = Integer.parseInt(tokens[0]);
			String actionName = tokens[tokens.length - 1];
			for (int i = 1; i < tokens.length - 1; i++) {
				String token = tokens[i];
				String[] pair = token.split(":");
				double probability = Double.parseDouble(pair[0]);
				int destState = Integer.parseInt(pair[1]);
				explicitMDP.addTransitionProbability(srcState, actionName, destState, probability);
			}
		}
	}

	/**
	 * Read transition costs from .trew file.
	 * 
	 * @param trewAllLines
	 *            : All lines from .trew file.
	 * @param choicesToActions
	 * @param explicitMDP
	 */
	private void readTransitionCosts(List<String> trewAllLines, String[][] choicesToActions, ExplicitMDP explicitMDP) {
		List<String> body = trewAllLines.subList(1, trewAllLines.size());
		for (String line : body) {
			// Line format: "{src} {choice} {dest} {cost}"
			String[] tokens = line.split(" ");
			int srcState = Integer.parseInt(tokens[0]);
			int choiceIndex = Integer.parseInt(tokens[1]);
			double cost = Double.parseDouble(tokens[3]);
			String actionName = choicesToActions[srcState][choiceIndex];
			explicitMDP.addTransitionCost(srcState, actionName, cost);
		}
	}

	/**
	 * Read the mapping from (src state, choice index) -> action name from .tra file.
	 * 
	 * Assume that choice indices of each state are ordered.
	 * 
	 * @param traAllLines
	 *            : All lines from .tra file
	 * @param numStates
	 * @param numActions
	 * @return Mapping from (src state, choice index) -> action name
	 */
	private String[][] readChoicesToActions(List<String> traAllLines, int numStates, int numActions) {
		// Maximum # of choices at each state is # of all actions
		String[][] choicesToActions = new String[numStates][numActions];
		List<String> body = traAllLines.subList(1, traAllLines.size());
		int prevSrcState = -1;
		int choiceIndex = 0;
		for (String line : body) {
			// Line format: "{src} {prob}:{dest} {prob}:{dest} ... {action name}"
			String[] tokens = line.split(" ");
			int srcState = Integer.parseInt(tokens[0]);
			String actionName = tokens[tokens.length - 1];

			if (srcState != prevSrcState) {
				// Reset choice index to 0 for the new state
				choiceIndex = 0;
			} else {
				// Increment choice index for the current state
				choiceIndex++;
			}

			// Map (src, choice index) -> action name
			choicesToActions[srcState][choiceIndex] = actionName;

			prevSrcState = srcState;
		}
		return choicesToActions;
	}

	/**
	 * Read state costs from .srew file.
	 * 
	 * @param srewAllLines
	 *            : All lines from .srew file
	 * @param explicitMDP
	 */
	private void readStateCosts(List<String> srewAllLines, ExplicitMDP explicitMDP) {
		List<String> body = srewAllLines.subList(1, srewAllLines.size());
		for (String line : body) {
			// Line format: "{src} {cost}"
			String[] tokens = line.split(" ");
			int state = Integer.parseInt(tokens[0]);
			double cost = Double.parseDouble(tokens[1]);
			explicitMDP.addStateCost(state, cost);
		}
	}

	private String readFirstLineFromFile(File file) throws IOException {
		try (FileReader fileReader = new FileReader(file);
				BufferedReader buffReader = new BufferedReader(fileReader);) {
			return buffReader.readLine();
		}
	}

	private List<String> readLinesFromFile(File file) throws IOException {
		List<String> lines = new ArrayList<>();

		try (FileReader fileReader = new FileReader(file);
				BufferedReader buffReader = new BufferedReader(fileReader);) {
			String line;
			while ((line = buffReader.readLine()) != null) {
				lines.add(line);
			}
		}
		return lines;
	}
}