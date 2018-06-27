package factors;

import mdp.Discriminant;
import mdp.ProbabilisticEffect;

public interface IProbabilisticTransitionFormula<E extends IAction> {

	public ProbabilisticEffect formula(Discriminant discriminant, E action);
}
