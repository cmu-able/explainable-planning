package examples.mobilerobot.dsm;

public class LocationNode {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private String mNodeID;

	public LocationNode(String nodeID) {
		mNodeID = nodeID;
	}

	public String getNodeID() {
		return mNodeID;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof LocationNode)) {
			return false;
		}
		LocationNode node = (LocationNode) obj;
		return node.mNodeID.equals(mNodeID);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mNodeID.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
