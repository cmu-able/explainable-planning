package mobilerobot.missiongen;

import examples.mobilerobot.dsm.MapTopology;
import examples.mobilerobot.dsm.exceptions.MapTopologyException;

public class ObjectiveInfo {

	private String mName;
	private double mMinStepValue;
	private double mMaxStepValue;
	private IGetMaxStepValue mGetMaxStepValue;

	public ObjectiveInfo(String name, double minStepValue, double maxStepValue) {
		this(name, minStepValue, null);
		mMaxStepValue = maxStepValue;
	}

	public ObjectiveInfo(String name, double minStepValue, IGetMaxStepValue getMaxStepValue) {
		mName = name;
		mMinStepValue = minStepValue;
		mGetMaxStepValue = getMaxStepValue;
	}

	public String getName() {
		return mName;
	}

	public double getMinStepValue() {
		return mMinStepValue;
	}

	public double getMaxStepValue(MapTopology mapTopology) throws MapTopologyException {
		return mGetMaxStepValue == null ? mMaxStepValue : mGetMaxStepValue.getMaxStepValue(mapTopology);
	}

	interface IGetMaxStepValue {
		public double getMaxStepValue(MapTopology mapTopology) throws MapTopologyException;
	}
}
