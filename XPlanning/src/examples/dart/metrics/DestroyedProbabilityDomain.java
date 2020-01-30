package examples.dart.metrics;

import examples.dart.models.IDurativeAction;
import examples.dart.models.RouteSegment;
import examples.dart.models.TeamAltitude;
import examples.dart.models.TeamDestroyed;
import examples.dart.models.TeamECM;
import examples.dart.models.TeamFormation;
import examples.dart.models.ThreatDistribution;
import language.domain.metrics.ITransitionStructure;
import language.domain.metrics.Transition;
import language.domain.metrics.TransitionStructure;
import language.domain.models.ActionDefinition;
import language.domain.models.IStateVarValue;
import language.domain.models.StateVarDefinition;
import language.exceptions.AttributeNameNotFoundException;
import language.exceptions.VarNotFoundException;
import language.mdp.StateVarClass;

/**
 * {@link DestroyedProbabilityDomain} represents the domain of {@link DestroyedProbabilityQFunction}. The
 * {@link ActionDefinition} of this domain is a composite action definition, which contains all actions of types IncAlt,
 * DecAlt, and Fly.
 * 
 * @author rsukkerd
 *
 */
public class DestroyedProbabilityDomain implements ITransitionStructure<IDurativeAction> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	// DestroyedProbability value is only applicable when team is still alive
	private StateVarDefinition<TeamDestroyed> mDestroyedSrcDef;

	// Determining factors of probability of being destroyed -- same as those in TeamDestroyedFormula
	private StateVarDefinition<TeamAltitude> mAltSrcDef;
	private StateVarDefinition<TeamFormation> mFormSrcDef;
	private StateVarDefinition<TeamECM> mECMSrcDef;
	private StateVarDefinition<RouteSegment> mSegmentSrcDef;

	private TransitionStructure<IDurativeAction> mDomain = new TransitionStructure<>();

	public DestroyedProbabilityDomain(StateVarDefinition<TeamDestroyed> destroyedSrcDef,
			StateVarDefinition<TeamAltitude> altSrcDef, StateVarDefinition<TeamFormation> formSrcDef,
			StateVarDefinition<TeamECM> ecmSrcDef, StateVarDefinition<RouteSegment> segmentSrcDef,
			ActionDefinition<IDurativeAction> durActionDef) {
		mDestroyedSrcDef = destroyedSrcDef;
		mAltSrcDef = altSrcDef;
		mFormSrcDef = formSrcDef;
		mECMSrcDef = ecmSrcDef;
		mSegmentSrcDef = segmentSrcDef;

		mDomain.addSrcStateVarDef(destroyedSrcDef);
		mDomain.addSrcStateVarDef(altSrcDef);
		mDomain.addSrcStateVarDef(formSrcDef);
		mDomain.addSrcStateVarDef(ecmSrcDef);
		mDomain.addSrcStateVarDef(segmentSrcDef);
		mDomain.setActionDef(durActionDef);
	}

	public TeamDestroyed getTeamDestroyed(Transition<IDurativeAction, DestroyedProbabilityDomain> transition)
			throws VarNotFoundException {
		return transition.getSrcStateVarValue(TeamDestroyed.class, mDestroyedSrcDef);
	}

	public TeamAltitude getTeamAltitude(Transition<IDurativeAction, DestroyedProbabilityDomain> transition)
			throws VarNotFoundException {
		return transition.getSrcStateVarValue(TeamAltitude.class, mAltSrcDef);
	}

	public TeamFormation getTeamFormation(Transition<IDurativeAction, DestroyedProbabilityDomain> transition)
			throws VarNotFoundException {
		return transition.getSrcStateVarValue(TeamFormation.class, mFormSrcDef);
	}

	public TeamECM getTeamECM(Transition<IDurativeAction, DestroyedProbabilityDomain> transition)
			throws VarNotFoundException {
		return transition.getSrcStateVarValue(TeamECM.class, mECMSrcDef);
	}

	public ThreatDistribution getThreatDistribution(Transition<IDurativeAction, DestroyedProbabilityDomain> transition)
			throws VarNotFoundException, AttributeNameNotFoundException {
		RouteSegment segment = transition.getSrcStateVarValue(RouteSegment.class, mSegmentSrcDef);
		return segment.getThreatDistribution();
	}

	@Override
	public StateVarClass getSrcStateVarClass() {
		return mDomain.getSrcStateVarClass();
	}

	@Override
	public StateVarClass getDestStateVarClass() {
		return mDomain.getDestStateVarClass();
	}

	@Override
	public ActionDefinition<IDurativeAction> getActionDef() {
		return mDomain.getActionDef();
	}

	@Override
	public boolean containsSrcStateVarDef(StateVarDefinition<? extends IStateVarValue> srcVarDef) {
		return mDomain.containsSrcStateVarDef(srcVarDef);
	}

	@Override
	public boolean containsDestStateVarDef(StateVarDefinition<? extends IStateVarValue> destVarDef) {
		return mDomain.containsDestStateVarDef(destVarDef);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof DestroyedProbabilityDomain)) {
			return false;
		}
		DestroyedProbabilityDomain domain = (DestroyedProbabilityDomain) obj;
		return domain.mDomain.equals(mDomain);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mDomain.hashCode();
			hashCode = result;
		}
		return result;
	}

}
