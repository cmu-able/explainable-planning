package prismconnector;

import mdp.Policy;
import mdp.XMDP;
import metrics.IQFunction;

public class PRISMTranslator {

	private XMDP mXMDP;

	public PRISMTranslator(XMDP xmdp) {
		mXMDP = xmdp;
	}

	public String getMDPTranslation() {
		return null;
	}

	public String getGoalPropertyTranslation() {
		return null;
	}

	public String getQFunctionTranslation(IQFunction qFunction) {
		return null;
	}

	public String getDTMC(Policy policy) {
		return null;
	}

}
