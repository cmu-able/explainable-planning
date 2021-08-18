package examples.mobilerobot.models;

import examples.mobilerobot.dsm.INodeAttribute;
import language.domain.models.IStateVarAttribute;

public class ChargerPresence implements IStateVarAttribute, INodeAttribute {
	
	private final boolean mHasCharger;

	public ChargerPresence(boolean hasCharger) {
		this.mHasCharger = hasCharger;	
	}
	
	public boolean isChargerPresent() {
		return mHasCharger;
	}

}
