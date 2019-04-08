package solver.prismconnector.explicitmodel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import language.domain.metrics.IQFunction;
import language.domain.metrics.ITransitionStructure;
import language.domain.models.IAction;
import language.exceptions.QFunctionNotFoundException;
import language.objectives.AttributeCostFunction;
import language.objectives.CostCriterion;
import language.objectives.IAdditiveCostFunction;
import solver.common.CostType;
import solver.common.ExplicitMDP;
import solver.prismconnector.PrismRewardType;
import solver.prismconnector.QFunctionEncodingScheme;
import solver.prismconnector.exceptions.ExplicitModelParsingException;
import solver.prismconnector.exceptions.GoalStatesParsingException;
import solver.prismconnector.exceptions.InitialStateParsingException;

public class ExplicitMDPReader {

	private static final String INIT_LAB_HEADER_PATTERN = "([0-9]+)=\"init\"";
	private static final String END_LAB_HEADER_PATTERN = "([0-9]+)=\"end\"";

	private PrismExplicitModelPointer mPrismModelPointer;
	private QFunctionEncodingScheme mQFunctionEncoding;
	private CostCriterion mCostCriterion;

	public ExplicitMDPReader(PrismExplicitModelReader prismExplicitModelReader, CostCriterion costCriterion) {
		mPrismModelPointer = prismExplicitModelReader.getPrismExplicitModelPointer();
		mQFunctionEncoding = prismExplicitModelReader.getValueEncodingScheme().getQFunctionEncodingScheme();
		mCostCriterion = costCriterion;
	}

	/**
	 * Read an {@link ExplicitMDP} from PRISM explicit model files.
	 * 
	 * @return ExplicitMDP without objective costs
	 * @throws IOException
	 * @throws ExplicitModelParsingException
	 */
	public ExplicitMDP readExplicitMDP() throws IOException, ExplicitModelParsingException {
		File traFile = mPrismModelPointer.getTransitionsFile();
		File labFile = mPrismModelPointer.getLabelsFile();
		List<String> traAllLines = readLinesFromFile(traFile);
		List<String> labAllLines = readLinesFromFile(labFile);
		String traHeader = readFirstLineFromFile(traFile);

		int numStates = readNumStates(traHeader);
		Set<String> actionNames = readActionNames(traAllLines);

		// Assume a single initial state
		int iniState = readInitialState(labAllLines);

		// SSP must have at least one goal state
		// Average-cost MDP does not have a goal state (there is no goal-reachability guarantee)
		Set<Integer> goalStates = mCostCriterion == CostCriterion.TOTAL_COST ? readGoalStates(labAllLines)
				: new HashSet<>();

		// Create an additional slot for cost function to:
		// (1) Align the indices of the cost functions (starts at 0) to the PRISM reward indices (starts at 1), and
		// (2) Reserve the first slot for the optimization objective function (which is NOT necessarily the XMDP's cost
		// function)
		int numCostFunctions = mQFunctionEncoding.getNumRewardStructures() + 1;

		CostType costType = mPrismModelPointer.getPrismRewardType() == PrismRewardType.STATE_REWARD
				? CostType.STATE_COST
				: CostType.TRANSITION_COST;

		ExplicitMDP explicitMDP = new ExplicitMDP(numStates, actionNames, costType, numCostFunctions, iniState,
				goalStates);
		readTransitionProbabilities(traAllLines, explicitMDP);

		if (costType == CostType.TRANSITION_COST) {
			readAllTransitionCosts(traAllLines, explicitMDP);
		} else if (costType == CostType.STATE_COST) {
			readAllStateCosts(explicitMDP);
		}
		return explicitMDP;
	}

	/**
	 * Read an {@link ExplicitMDP} from PRISM explicit model files, and set its objective costs according to the given
	 * objective function.
	 * 
	 * @param objectiveFunction
	 *            : Optimization objective function
	 * @return ExplicitMDP with objective costs
	 * @throws IOException
	 * @throws ExplicitModelParsingException
	 * @throws QFunctionNotFoundException
	 */
	public ExplicitMDP readExplicitMDP(IAdditiveCostFunction objectiveFunction)
			throws IOException, ExplicitModelParsingException, QFunctionNotFoundException {
		ExplicitMDP explicitMDP = readExplicitMDP();

		if (mQFunctionEncoding.contains(objectiveFunction)) {
			// The given objective function has a corresponding cost function (already in explicitMDP).
			// Set that cost function to be the objective function of explicitMDP.
			int objectiveIndex = mQFunctionEncoding.getRewardStructureIndex(objectiveFunction);
			explicitMDP.setObjectiveCosts(objectiveIndex);
		} else {
			// The given objective function doesn't have a corresponding cost function in explicitMDP.
			// Compute the objective costs according to the function and add them to explicitMDP.
			setObjectiveFunctionOfExplicitMDP(explicitMDP, objectiveFunction);
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
	 * Read the initial state from .lab file.
	 * 
	 * @param labAllLines
	 *            : All lines from .lab file
	 * @return Initial state
	 * @throws InitialStateParsingException
	 */
	private int readInitialState(List<String> labAllLines) throws InitialStateParsingException {
		// Header format: 0="init" 1="deadlock" ... {n}="end" ...
		String labHeader = labAllLines.get(0);
		Pattern pattern = Pattern.compile(INIT_LAB_HEADER_PATTERN);
		Matcher matcher = pattern.matcher(labHeader);
		if (!matcher.find()) {
			throw new InitialStateParsingException(labHeader);
		}
		String initLabel = matcher.group(1);
		List<String> labBody = labAllLines.subList(1, labAllLines.size());
		for (String line : labBody) {
			// Line format: "{state}: {label} {label} ..."
			String[] pair = line.split(":");
			String[] labels = pair[1].trim().split(" ");
			if (labels[0].equals(initLabel)) {
				return Integer.parseInt(pair[0]);
			}
		}
		throw new InitialStateParsingException(labHeader, labBody);
	}

	/**
	 * Read the goal states (labeled "end") from .lab file.
	 * 
	 * @param labAllLines
	 *            : All lines from .lab file
	 * @return Goal states
	 * @throws GoalStatesParsingException
	 */
	private Set<Integer> readGoalStates(List<String> labAllLines) throws GoalStatesParsingException {
		Set<Integer> goalStates = new HashSet<>();
		// Header format: 0="init" 1="deadlock" ... {n}="end" ...
		String labHeader = labAllLines.get(0);
		Pattern pattern = Pattern.compile(END_LAB_HEADER_PATTERN);
		Matcher matcher = pattern.matcher(labHeader);
		if (!matcher.find()) {
			throw new GoalStatesParsingException(labHeader);
		}
		String endLabel = matcher.group(1);
		List<String> labBody = labAllLines.subList(1, labAllLines.size());
		for (String line : labBody) {
			// Line format: "{state}: {label} {label} ..."
			String[] pair = line.split(":");
			String[] labels = pair[1].trim().split(" ");
			for (String label : labels) {
				if (label.equals(endLabel)) {
					Integer goalState = Integer.parseInt(pair[0]);
					goalStates.add(goalState);
					break;
				}
			}
		}
		if (goalStates.isEmpty()) {
			throw new GoalStatesParsingException(labHeader, labBody);
		}
		return goalStates;
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
	 * Read transition costs from all .trew files into all cost function indices -- except the 0th index, which is
	 * reserved for the optimization objective function.
	 * 
	 * @param traAllLines
	 *            : All lines from .tra file
	 * @param explicitMDP
	 * @throws IOException
	 */
	private void readAllTransitionCosts(List<String> traAllLines, ExplicitMDP explicitMDP) throws IOException {
		int numStates = explicitMDP.getNumStates();
		int numActions = explicitMDP.getNumActions();
		String[][] choicesToActions = readChoicesToActions(traAllLines, numStates, numActions);
		int numRewardStructs = mQFunctionEncoding.getNumRewardStructures();

		// Reserve 0-slot for the optimization objective function
		for (int k = 1; k <= numRewardStructs; k++) {
			File trewFile = mPrismModelPointer.getIndexedTransitionRewardsFile(k);
			List<String> trewAllLines = readLinesFromFile(trewFile);
			readTransitionCosts(k, trewAllLines, choicesToActions, explicitMDP);
		}
	}

	/**
	 * Read transition costs from .trew file into a specific cost function index.
	 * 
	 * @param costFuncIndex
	 *            : Cost function index
	 * @param trewAllLines
	 *            : All lines from .trew file.
	 * @param choicesToActions
	 * @param explicitMDP
	 */
	private void readTransitionCosts(int costFuncIndex, List<String> trewAllLines, String[][] choicesToActions,
			ExplicitMDP explicitMDP) {
		List<String> body = trewAllLines.subList(1, trewAllLines.size());
		for (String line : body) {
			// Line format: "{src} {choice} {dest} {cost}"
			String[] tokens = line.split(" ");
			int srcState = Integer.parseInt(tokens[0]);
			int choiceIndex = Integer.parseInt(tokens[1]);
			double cost = Double.parseDouble(tokens[3]);
			String actionName = choicesToActions[srcState][choiceIndex];
			explicitMDP.addTransitionCost(costFuncIndex, srcState, actionName, cost);
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
	 * Read state costs from all .srew files into all cost function indices -- except the 0th index, which is reserved
	 * for the optimization objective function.
	 * 
	 * @param explicitMDP
	 * @throws IOException
	 */
	private void readAllStateCosts(ExplicitMDP explicitMDP) throws IOException {
		int numRewardStructs = mQFunctionEncoding.getNumRewardStructures();

		// Reserve 0-slot for the optimization objective function
		for (int k = 1; k < numRewardStructs; k++) {
			File srewFile = mPrismModelPointer.getIndexedStateRewardsFile(k);
			List<String> srewAllLines = readLinesFromFile(srewFile);
			readStateCosts(k, srewAllLines, explicitMDP);
		}
	}

	/**
	 * Read state costs from .srew file into a specific cost function index.
	 * 
	 * @param costFuncIndex
	 *            : Cost function index
	 * @param srewAllLines
	 *            : All lines from .srew file
	 * @param explicitMDP
	 */
	private void readStateCosts(int costFuncIndex, List<String> srewAllLines, ExplicitMDP explicitMDP) {
		List<String> body = srewAllLines.subList(1, srewAllLines.size());
		for (String line : body) {
			// Line format: "{src} {cost}"
			String[] tokens = line.split(" ");
			int state = Integer.parseInt(tokens[0]);
			double cost = Double.parseDouble(tokens[1]);
			explicitMDP.addStateCost(costFuncIndex, state, cost);
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

	/**
	 * Set the objective function of the ExplicitMDP.
	 * 
	 * If the cost type is transition, objective cost of a transition is: c_0[i][a] = sum_k(scaling const * attribute
	 * cost of c_k[i][a]).
	 * 
	 * If the cost type is state, objective cost of a state is: c_0[i] = sum_k(scaling const * attribute cost of
	 * c_k[i]).
	 * 
	 * @param objectiveFunction
	 * @param qFunctionEncoding
	 * @throws QFunctionNotFoundException
	 */
	private void setObjectiveFunctionOfExplicitMDP(ExplicitMDP explicitMDP, IAdditiveCostFunction objectiveFunction)
			throws QFunctionNotFoundException {
		int numStates = explicitMDP.getNumStates();
		int numActions = explicitMDP.getNumActions();

		// Auxiliary cost is assigned to every "compute" transition
		double offset = objectiveFunction.getOffset();

		for (int i = 0; i < numStates; i++) {
			for (int a = 0; a < numActions; a++) {
				// Compute "pure" objective cost from all QA functions in the objective function
				double objectiveCost = computePureObjectiveCost(i, a, explicitMDP, objectiveFunction);

				// Add offset to objective cost of (any state, "compute" action)
				String actionName = explicitMDP.getActionNameAtIndex(a);
				if (actionName.equals("compute")) {
					objectiveCost += offset;
				}

				// Set objective cost at c_0[i][a]
				// OR
				// Set objective cost at c_0[i]
				if (explicitMDP.getCostType() == CostType.TRANSITION_COST) {
					explicitMDP.addObjectiveTransitionCost(i, a, objectiveCost);
				} else {
					explicitMDP.addObjectiveStateCost(i, objectiveCost);
				}
			}
		}
	}

	private double computePureObjectiveCost(int i, int a, ExplicitMDP explicitMDP,
			IAdditiveCostFunction objectiveFunction) throws QFunctionNotFoundException {
		Set<IQFunction<IAction, ITransitionStructure<IAction>>> qFunctions = objectiveFunction.getQFunctions();

		// Objective cost of a transition: c_0[i][a] = sum_k(scaling const * attribute cost of c_k[i][a])
		// OR
		// Objective cost of a state: c_0[i] = sum_k(scaling const * attribute cost of c_k[i])
		double objectiveCost = 0;

		for (IQFunction<?, ?> qFunction : qFunctions) {
			AttributeCostFunction<?> attrCostFunc = objectiveFunction.getAttributeCostFunction(qFunction);
			double scalingConst = objectiveFunction.getScalingConstant(attrCostFunc);

			// When constructing ExplicitMDP, we ensure that the indices of the cost functions of ExplicitMDP
			// are aligned with the indices of the PRISM reward structures
			int costFuncIndex = mQFunctionEncoding.getRewardStructureIndex(qFunction);

			// QA value of a single transition
			// OR
			// QA value of a single state
			double stepQValue = explicitMDP.getCostType() == CostType.TRANSITION_COST
					? explicitMDP.getTransitionCost(costFuncIndex, i, a)
					: explicitMDP.getStateCost(costFuncIndex, i);

			// Attribute cost of the transition value
			// OR
			// Attribute cost of the state value
			double stepAttrCost = attrCostFunc.getCost(stepQValue);

			objectiveCost += scalingConst * stepAttrCost;
		}

		return objectiveCost;
	}
}
