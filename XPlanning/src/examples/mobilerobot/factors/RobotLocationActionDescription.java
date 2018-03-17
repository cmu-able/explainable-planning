package examples.mobilerobot.factors;

import java.util.Iterator;
import java.util.Map.Entry;

import factors.StateVar;
import factors.StateVarDefinition;
import mdp.Discriminant;
import mdp.EffectClass;
import mdp.IActionDescription;
import mdp.ProbabilisticEffect;

public class RobotLocationActionDescription implements IActionDescription {

	public RobotLocationActionDescription(MoveToAction moveTo, StateVarDefinition<Location> rLocDef) {
		// TODO Auto-generated constructor stub
	}

	public boolean isConnected(StateVar<Location> rLocSrc) {
		return false;
	}

	@Override
	public Iterator<Entry<Discriminant, ProbabilisticEffect>> iterator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public EffectClass getEffectClass() {
		// TODO Auto-generated method stub
		return null;
	}

}
