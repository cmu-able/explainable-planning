package examples.clinicscheduling.dsm;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import examples.clinicscheduling.metrics.ClientPredictionUtils;
import examples.clinicscheduling.metrics.IdleTimeQFunction;
import examples.clinicscheduling.metrics.LeadTimeDomain;
import examples.clinicscheduling.metrics.LeadTimeQFunction;
import examples.clinicscheduling.metrics.OvertimeQFunction;
import examples.clinicscheduling.metrics.RevenueQFunction;
import examples.clinicscheduling.metrics.ScheduleDomain;
import examples.clinicscheduling.metrics.SwitchABPDomain;
import examples.clinicscheduling.metrics.SwitchABPQFunction;
import examples.clinicscheduling.models.ABP;
import examples.clinicscheduling.models.ABPActionDescription;
import examples.clinicscheduling.models.BookedClientCountActionDescription;
import examples.clinicscheduling.models.ClientCount;
import examples.clinicscheduling.models.NewClientCountActionDescription;
import examples.clinicscheduling.models.ScheduleAction;
import language.domain.metrics.IQFunction;
import language.domain.metrics.ITransitionStructure;
import language.domain.models.ActionDefinition;
import language.domain.models.IAction;
import language.domain.models.StateVarDefinition;
import language.exceptions.IncompatibleActionException;
import language.mdp.ActionSpace;
import language.mdp.FactoredPSO;
import language.mdp.Precondition;
import language.mdp.QSpace;
import language.mdp.StateSpace;
import language.mdp.StateVarClass;
import language.mdp.StateVarTuple;
import language.mdp.TransitionFunction;
import language.mdp.XMDP;
import language.objectives.AttributeCostFunction;
import language.objectives.CostFunction;

public class ClinicSchedulingXMDPBuilder {

	private int mBranchFactor;

	// State variables
	private StateVarDefinition<ABP> rABPDef;
	private StateVarDefinition<ClientCount> rABClientCountDef;
	private StateVarDefinition<ClientCount> rNewClientCountDef;

	// Action definition
	private ActionDefinition<ScheduleAction> scheduleDef;

	public ClinicSchedulingXMDPBuilder(int branchFactor) {
		mBranchFactor = branchFactor;
	}

	public XMDP buildXMDP(SchedulingContext schedulingContext, int iniABP, int iniABCount, int iniNewClientCount,
			double clientArrivalRate) throws IncompatibleActionException {
		int maxABP = schedulingContext.getMaxABP();
		int maxQueueSize = schedulingContext.getMaxQueueSize();
		int capacity = schedulingContext.getCapacity();
		ClinicCostProfile clinicCostProfile = schedulingContext.getClinicCostProfile();

		StateSpace stateSpace = buildStateSpace(maxABP, maxQueueSize, clientArrivalRate);
		ActionSpace actionSpace = buildActionSpace();
		StateVarTuple initialState = buildInitialState(iniABP, iniABCount, iniNewClientCount);
		StateVarTuple goal = null; // This average-cost MDP does not have a goal
		TransitionFunction transFunction = buildTransitionFunction(maxQueueSize, clientArrivalRate);
		QSpace qSpace = buildQFunctions(capacity, clinicCostProfile);
		CostFunction costFunction = buildCostFunction(qSpace, maxABP, clientArrivalRate);
		return new XMDP(stateSpace, actionSpace, initialState, goal, transFunction, qSpace, costFunction);
	}

	private StateSpace buildStateSpace(int maxABP, int maxQueueSize, double clientArrivalRate) {
		Set<ABP> possibleABPs = new HashSet<>();
		// The minimum value of ABP must be 1 to be able to compute appointment lead time: LT = floor(x / w)
		for (int i = 1; i <= maxABP; i++) {
			ABP abp = new ABP(i);
			possibleABPs.add(abp);
		}

		// Variable: w = current ABP (# clients who can be booked in advance in each future day)
		rABPDef = new StateVarDefinition<>("w", possibleABPs);

		Set<ClientCount> possibleABClientCounts = new HashSet<>();
		for (int i = 0; i <= maxQueueSize; i++) {
			ClientCount clientCount = new ClientCount(i);
			possibleABClientCounts.add(clientCount);
		}

		// Variable: x = # clients who have been previously booked for appointments
		rABClientCountDef = new StateVarDefinition<>("x", possibleABClientCounts);

		// Variable: y = # new client-requests arriving today
		rNewClientCountDef = new StateVarDefinition<>("y",
				ClientPredictionUtils.getPossibleNewClientCounts(clientArrivalRate, mBranchFactor));

		StateSpace stateSpace = new StateSpace();
		stateSpace.addStateVarDefinition(rABPDef);
		stateSpace.addStateVarDefinition(rABClientCountDef);
		stateSpace.addStateVarDefinition(rNewClientCountDef);
		return stateSpace;
	}

	private ClientCount getMaxNewClientCount() {
		Set<ClientCount> possibleNewClientCounts = rNewClientCountDef.getPossibleValues();
		return Collections.max(possibleNewClientCounts);
	}

	private ActionSpace buildActionSpace() {
		Set<ScheduleAction> scheduleActions = new HashSet<>();

		// Schedule action: action = <a, b>
		// a = new ABP
		// b = # new clients arriving today to service today

		// Parameter a can be any value in the set of possible values of variable w (current ABP)
		// Parameter b can be any value up to maximum number of new clients arriving in each day

		ClientCount maxNewClientCount = getMaxNewClientCount();

		for (ABP newABP : rABPDef.getPossibleValues()) {
			for (int b = 0; b <= maxNewClientCount.getValue(); b++) {
				ClientCount numNewClientsToService = new ClientCount(b);
				ScheduleAction schedule = new ScheduleAction(newABP, numNewClientsToService);
				scheduleActions.add(schedule);
			}
		}

		// Schedule action definition
		scheduleDef = new ActionDefinition<>("schedule", scheduleActions);

		ActionSpace actionSpace = new ActionSpace();
		actionSpace.addActionDefinition(scheduleDef);
		return actionSpace;
	}

	private StateVarTuple buildInitialState(int iniABP, int iniABCount, int iniNewClientCount) {
		ABP initialABP = new ABP(iniABP);
		ClientCount initialABCount = new ClientCount(iniABCount);
		ClientCount initialNewClientCount = new ClientCount(iniNewClientCount);

		StateVarTuple initialState = new StateVarTuple();
		initialState.addStateVar(rABPDef.getStateVar(initialABP));
		initialState.addStateVar(rABClientCountDef.getStateVar(initialABCount));
		initialState.addStateVar(rNewClientCountDef.getStateVar(initialNewClientCount));
		return initialState;
	}

	private TransitionFunction buildTransitionFunction(int maxQueueSize, double clientArrivalRate)
			throws IncompatibleActionException {
		// Schedule: action = <a, b>

		// Precondition
		Precondition<ScheduleAction> preSchedule = new Precondition<>(scheduleDef);

		// Add preconditions
		for (ScheduleAction schedule : scheduleDef.getActions()) {
			ABP newABP = schedule.getNewABP(); // a
			ClientCount numNewClientsToService = schedule.getNumNewClientsToService(); // b

			addSchedulePreconditions(preSchedule, maxQueueSize, schedule, newABP, numNewClientsToService);
		}

		// Action description for w
		ABPActionDescription rABPActionDesc = new ABPActionDescription(scheduleDef, preSchedule, rABPDef);

		// Action description for x
		BookedClientCountActionDescription rABClientCountActionDesc = new BookedClientCountActionDescription(
				scheduleDef, preSchedule, rABClientCountDef, rABPDef, rNewClientCountDef);

		// Action description for y
		NewClientCountActionDescription rNewClientCountActionDesc = new NewClientCountActionDescription(scheduleDef,
				preSchedule, rNewClientCountDef, clientArrivalRate, mBranchFactor);

		// PSO
		FactoredPSO<ScheduleAction> schedulePSO = new FactoredPSO<>(scheduleDef, preSchedule);
		schedulePSO.addActionDescription(rABPActionDesc);
		schedulePSO.addActionDescription(rABClientCountActionDesc);
		schedulePSO.addActionDescription(rNewClientCountActionDesc);

		TransitionFunction transFunction = new TransitionFunction();
		transFunction.add(schedulePSO);
		return transFunction;
	}

	private void addSchedulePreconditions(Precondition<ScheduleAction> preSchedule, int maxQueueSize,
			ScheduleAction schedule, ABP newABP, ClientCount numNewClientsToService)
			throws IncompatibleActionException {
		// Precondition has a multivariate predicate on variables (w, x, y)
		StateVarClass multivarPredVarClass = new StateVarClass();
		multivarPredVarClass.add(rABPDef);
		multivarPredVarClass.add(rABClientCountDef);
		multivarPredVarClass.add(rNewClientCountDef);

		for (ABP currABP : rABPDef.getPossibleValues()) { // w

			for (ClientCount currABClientCount : rABClientCountDef.getPossibleValues()) { // x

				for (ClientCount newClientCount : rNewClientCountDef.getPossibleValues()) { // y

					boolean sat1 = satScheduleConstraint1(newClientCount, numNewClientsToService);
					boolean sat2 = satScheduleConstraint2(currABClientCount, currABP, newClientCount,
							numNewClientsToService, maxQueueSize);
					boolean sat3 = satScheduleConstraint3(currABP, newABP);

					if (sat1 && sat2 && sat3) {
						StateVarTuple allowableTuple = new StateVarTuple();
						allowableTuple.addStateVar(rABPDef.getStateVar(currABP));
						allowableTuple.addStateVar(rABClientCountDef.getStateVar(currABClientCount));
						allowableTuple.addStateVar(rNewClientCountDef.getStateVar(newClientCount));

						// Add an allowable value tuple of (w, x, y)
						preSchedule.add(schedule, multivarPredVarClass, allowableTuple);
					}
				}
			}
		}
	}

	/**
	 * Scheduling constraint 1: b <= y.
	 * 
	 * Cannot have # same-day requests serviced be more than # new requests that day.
	 * 
	 * @param newClientCount
	 *            : Number of new clients arriving today, y
	 * @param numNewClientsToService
	 *            : Number of new clients arriving today to service today, b
	 * @return Whether b <= y
	 */
	private boolean satScheduleConstraint1(ClientCount newClientCount, ClientCount numNewClientsToService) {
		int y = newClientCount.getValue();
		int b = numNewClientsToService.getValue();
		return b <= y;
	}

	/**
	 * Scheduling constraint 2: x - min(x, w) + y - b <= N.
	 * 
	 * Cannot have # clients booked in advance exceed the limit N.
	 * 
	 * @param currABClientCount
	 *            : Number of clients who have been previously booked for appointments, x
	 * @param currABP
	 *            : Current ABP, w
	 * @param newClientCount
	 *            : Number of new clients arriving today, y
	 * @param numNewClientsToService
	 *            : Number of new clients arriving today to service today, b
	 * @param maxQueueSize
	 *            : Limit N
	 * @return Whether x - min(x, w) + y - b <= N
	 */
	private boolean satScheduleConstraint2(ClientCount currABClientCount, ABP currABP, ClientCount newClientCount,
			ClientCount numNewClientsToService, int maxQueueSize) {
		int x = currABClientCount.getValue();
		int w = currABP.getValue();
		int y = newClientCount.getValue();
		int b = numNewClientsToService.getValue();
		return x - Math.min(x, w) + y - b <= maxQueueSize;
	}

	/**
	 * Scheduling constraint 3: |w - a| <= 1.
	 * 
	 * Cannot have a new ABP that is more than 1 slot difference from the current ABP.
	 * 
	 * @param currABP
	 *            : Current ABP, w
	 * @param newABP
	 *            : New ABP, a
	 * @return Whether |w - a| <= 1
	 */
	private boolean satScheduleConstraint3(ABP currABP, ABP newABP) {
		int w = currABP.getValue();
		int a = newABP.getValue();
		return Math.abs(w - a) <= 1;
	}

	private QSpace buildQFunctions(int capacity, ClinicCostProfile clinicCostProfile) {
		// Revenue
		double revenuePerPatient = clinicCostProfile.getRevenuePerPatient();
		ScheduleDomain scheduleDomain = new ScheduleDomain(rABPDef, rABClientCountDef, scheduleDef);
		RevenueQFunction revenueQFunction = new RevenueQFunction(scheduleDomain, revenuePerPatient);

		// Overtime cost
		double overtimeCostPerPatient = clinicCostProfile.getOvertimeCostPerPatient();
		OvertimeQFunction overtimeQFunction = new OvertimeQFunction(scheduleDomain, overtimeCostPerPatient, capacity);

		// Idle time cost
		double idleTimeCostPerPatient = clinicCostProfile.getIdleTimeCostPerPatient();
		IdleTimeQFunction idleTimeQFunction = new IdleTimeQFunction(scheduleDomain, idleTimeCostPerPatient, capacity);

		// Lead time cost
		double leadTimeCostFactor = clinicCostProfile.getLeadTimeCostFactor();
		LeadTimeDomain leadTimeDomain = new LeadTimeDomain(rABPDef, rABClientCountDef, scheduleDef);
		LeadTimeQFunction leadTimeQFunction = new LeadTimeQFunction(leadTimeDomain, leadTimeCostFactor);

		// Switching ABP cost
		double switchABPCostFactor = clinicCostProfile.getSwitchABPCostFactor();
		SwitchABPDomain switchABPDomain = new SwitchABPDomain(rABPDef, scheduleDef);
		SwitchABPQFunction switchABPQFunction = new SwitchABPQFunction(switchABPDomain, switchABPCostFactor);

		QSpace qSpace = new QSpace();
		qSpace.addQFunction(revenueQFunction);
		qSpace.addQFunction(overtimeQFunction);
		qSpace.addQFunction(idleTimeQFunction);
		qSpace.addQFunction(leadTimeQFunction);
		qSpace.addQFunction(switchABPQFunction);
		return qSpace;
	}

	private CostFunction buildCostFunction(QSpace qSpace, int maxABP, double clientArrivalRate) {
		CostFunction costFunction = new CostFunction();
		for (IQFunction<?, ?> qFunction : qSpace) {
			addAttributeCostFunctions(qFunction, costFunction, maxABP, clientArrivalRate);
		}
		return costFunction;
	}

	private <E extends IAction, T extends ITransitionStructure<E>, S extends IQFunction<E, T>> void addAttributeCostFunctions(
			S qFunction, CostFunction costFunction, int maxABP, double clientArrivalRate) {
		double aConst;
		double bConst;
		double scalingConst = 1;

		if (qFunction instanceof RevenueQFunction) {
			RevenueQFunction revenueQFunction = (RevenueQFunction) qFunction;
			// Maximum number of new clients arriving each day is 2X the average client arrival rate
			double maxClientArrival = 2 * clientArrivalRate;
			// Maximum daily revenue = maximum revenue from all advance-booking patients and same-day patients booked
			// for today
			double maxDailyRevenue = revenueQFunction.getRevenuePerPatient() * (maxABP + maxClientArrival);
			// "Cost" of revenue is the difference between the revenue and its maximum value
			aConst = maxDailyRevenue;
			bConst = -1;
		} else {
			aConst = 0;
			bConst = 1;
		}

		AttributeCostFunction<S> attrCostFunction = new AttributeCostFunction<>(qFunction, aConst, bConst);
		costFunction.put(attrCostFunction, scalingConst);
	}
}
