package examples.dart.metrics;

import examples.dart.models.FlyAction;
import examples.dart.models.RouteSegment;
import examples.dart.models.TargetDistribution;
import examples.dart.models.TeamAltitude;
import examples.dart.models.TeamECM;
import examples.dart.models.TeamFormation;
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
 * {@link FlyDetectTargetDomain} represents the domain of {@link FlyMissTargetEvent}.
 * 
 * @author rsukkerd
 *
 */
public class FlyDetectTargetDomain implements ITransitionStructure<FlyAction> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private StateVarDefinition<TeamAltitude> mAltSrcDef;
	private StateVarDefinition<TeamFormation> mFormSrcDef;
	private StateVarDefinition<TeamECM> mECMSrcDef;
	private StateVarDefinition<RouteSegment> mSegmentSrcDef;

	private TransitionStructure<FlyAction> mDomain = new TransitionStructure<>();

	public FlyDetectTargetDomain(StateVarDefinition<TeamAltitude> altSrcDef,
			StateVarDefinition<TeamFormation> formSrcDef, StateVarDefinition<TeamECM> ecmSrcDef,
			StateVarDefinition<RouteSegment> segmentSrcDef, ActionDefinition<FlyAction> flyDef) {
		mAltSrcDef = altSrcDef;
		mFormSrcDef = formSrcDef;
		mECMSrcDef = ecmSrcDef;
		mSegmentSrcDef = segmentSrcDef;

		mDomain.addSrcStateVarDef(altSrcDef);
		mDomain.addSrcStateVarDef(formSrcDef);
		mDomain.addSrcStateVarDef(ecmSrcDef);
		mDomain.addSrcStateVarDef(segmentSrcDef);
		mDomain.setActionDef(flyDef);
	}

	public TeamAltitude getTeamAltitude(Transition<FlyAction, FlyDetectTargetDomain> transition)
			throws VarNotFoundException {
		return transition.getSrcStateVarValue(TeamAltitude.class, mAltSrcDef);
	}

	public TeamFormation getTeamFormation(Transition<FlyAction, FlyDetectTargetDomain> transition)
			throws VarNotFoundException {
		return transition.getSrcStateVarValue(TeamFormation.class, mFormSrcDef);
	}

	public TeamECM getTeamECM(Transition<FlyAction, FlyDetectTargetDomain> transition) throws VarNotFoundException {
		return transition.getSrcStateVarValue(TeamECM.class, mECMSrcDef);
	}

	public TargetDistribution getTargetDistribution(Transition<FlyAction, FlyDetectTargetDomain> transition)
			throws VarNotFoundException, AttributeNameNotFoundException {
		RouteSegment segment = transition.getSrcStateVarValue(RouteSegment.class, mSegmentSrcDef);
		return segment.getTargetDistribution();
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
	public ActionDefinition<FlyAction> getActionDef() {
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
		if (!(obj instanceof FlyDetectTargetDomain)) {
			return false;
		}
		FlyDetectTargetDomain domain = (FlyDetectTargetDomain) obj;
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
