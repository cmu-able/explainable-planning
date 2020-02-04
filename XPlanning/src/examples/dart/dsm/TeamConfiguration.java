package examples.dart.dsm;

public class TeamConfiguration {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private int mAltitude;
	private String mFormation;
	private boolean mECM;

	public TeamConfiguration(int altitude, String formation, boolean ecm) {
		mAltitude = altitude;
		mFormation = formation;
		mECM = ecm;
	}

	public int getAltitudeLevel() {
		return mAltitude;
	}

	public String getFormation() {
		return mFormation;
	}

	public boolean getECM() {
		return mECM;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof TeamConfiguration)) {
			return false;
		}
		TeamConfiguration config = (TeamConfiguration) obj;
		return config.mAltitude == mAltitude && config.mFormation.equals(mFormation) && config.mECM == mECM;
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + Integer.hashCode(mAltitude);
			result = 31 * result + mFormation.hashCode();
			result = 31 * result + Boolean.hashCode(mECM);
			hashCode = result;
		}
		return hashCode;
	}
}
