package examples.dart.models;

import language.domain.models.IProbabilisticTransitionFormula;
import language.domain.models.StateVar;
import language.domain.models.StateVarDefinition;
import language.exceptions.XMDPException;
import language.mdp.Discriminant;
import language.mdp.Effect;
import language.mdp.EffectClass;
import language.mdp.ProbabilisticEffect;

/**
 * {@link RouteSegmentFormula} is the formula of the effect on route segment (above which the team is currently) of any
 * action of type {@link IDurativeAction}.
 * 
 * @author rsukkerd
 *
 * @param <E>
 */
public class RouteSegmentFormula<E extends IDurativeAction> implements IProbabilisticTransitionFormula<E> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	// Discriminant/Effect variable
	private StateVarDefinition<RouteSegment> mSegmentDef;

	private EffectClass mEffectClass; // of route segment

	public RouteSegmentFormula(StateVarDefinition<RouteSegment> segmentDef) {
		mSegmentDef = segmentDef;

		mEffectClass = new EffectClass();
		mEffectClass.add(segmentDef);
	}

	@Override
	public ProbabilisticEffect formula(Discriminant discriminant, E action) throws XMDPException {
		RouteSegment currSegment = discriminant.getStateVarValue(RouteSegment.class, mSegmentDef);
		RouteSegment targetSegment = retrieveRouteSegment(currSegment.getSegment() + 1);

		// Target route segment variable
		StateVar<RouteSegment> segmentDest = mSegmentDef.getStateVar(targetSegment);

		ProbabilisticEffect segmentProbEffect = new ProbabilisticEffect(mEffectClass);
		Effect newSegmentEffect = new Effect(mEffectClass);
		newSegmentEffect.add(segmentDest);
		segmentProbEffect.put(newSegmentEffect, 1.0);
		return segmentProbEffect;
	}

	private RouteSegment retrieveRouteSegment(int targetSegment) {
		for (RouteSegment routeSegment : mSegmentDef.getPossibleValues()) {
			if (routeSegment.getSegment() == targetSegment) {
				return routeSegment;
			}
		}
		throw new IllegalArgumentException("Segment " + targetSegment + " does not exist");
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof RouteSegmentFormula<?>)) {
			return false;
		}
		RouteSegmentFormula<?> formula = (RouteSegmentFormula<?>) obj;
		return formula.mSegmentDef.equals(mSegmentDef);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mSegmentDef.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
