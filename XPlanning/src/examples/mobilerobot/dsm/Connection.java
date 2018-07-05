package examples.mobilerobot.dsm;

import java.util.HashSet;
import java.util.Set;

public class Connection {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private LocationNode mNodeA;
	private LocationNode mNodeB;
	private Set<LocationNode> mNodes = new HashSet<>();
	private double mDistance;

	public Connection(LocationNode nodeA, LocationNode nodeB, double distance) {
		mNodeA = nodeA;
		mNodeB = nodeB;
		mNodes.add(nodeA);
		mNodes.add(nodeB);
		mDistance = distance;
	}

	public LocationNode getNodeA() {
		return mNodeA;
	}

	public LocationNode getNodeB() {
		return mNodeB;
	}

	public double getDistance() {
		return mDistance;
	}

	public LocationNode getOtherNode(LocationNode node) throws LocationNodeNotFoundException {
		if (!getNodeA().equals(node) && !getNodeB().equals(node)) {
			throw new LocationNodeNotFoundException(node);
		}
		return getNodeA().equals(node) ? getNodeB() : getNodeA();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof Connection)) {
			return false;
		}
		Connection connection = (Connection) obj;
		return connection.mNodes.equals(mNodes) && Double.compare(connection.mDistance, mDistance) == 0;
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mNodes.hashCode();
			result = 31 * result + Double.hashCode(mDistance);
			hashCode = result;
		}
		return hashCode;
	}

	@Override
	public String toString() {
		return "connected(" + mNodeA + ", " + mNodeB + ")";
	}
}
