package examples.mobilerobot.dsm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import examples.mobilerobot.dsm.exceptions.ConnectionAttributeNotFoundException;
import examples.mobilerobot.dsm.exceptions.LocationNodeNotFoundException;

public class Connection {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private LocationNode mNodeA;
	private LocationNode mNodeB;
	private Set<LocationNode> mNodes = new HashSet<>();
	private double mDistance;
	private Map<String, IEdgeAttribute> mEdgeAttributes = new HashMap<>();

	public Connection(LocationNode nodeA, LocationNode nodeB, double distance) {
		this(nodeA, nodeB, distance, new HashMap<>());
	}

	public Connection(LocationNode nodeA, LocationNode nodeB, double distance,
			Map<String, IEdgeAttribute> defaultEdgeAttributes) {
		mNodeA = nodeA;
		mNodeB = nodeB;
		mNodes.add(nodeA);
		mNodes.add(nodeB);
		mDistance = distance;
		mEdgeAttributes.putAll(defaultEdgeAttributes);
	}

	public void putConnectionAttribute(String name, IEdgeAttribute value) {
		mEdgeAttributes.put(name, value);
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

	public <E extends IEdgeAttribute> E getConnectionAttribute(Class<E> attributeType, String name)
			throws ConnectionAttributeNotFoundException {
		return attributeType.cast(getGenericConnectionAttribute(name));
	}

	public <E extends IEdgeAttribute> E getConnectionAttribute(Class<E> attributeType, String name, E defaultValue) {
		if (!mEdgeAttributes.containsKey(name)) {
			return defaultValue;
		}
		return attributeType.cast(mEdgeAttributes.get(name));
	}

	public IEdgeAttribute getGenericConnectionAttribute(String name) throws ConnectionAttributeNotFoundException {
		if (!mEdgeAttributes.containsKey(name)) {
			throw new ConnectionAttributeNotFoundException(name);
		}
		return mEdgeAttributes.get(name);
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
		return connection.mNodes.equals(mNodes) && Double.compare(connection.mDistance, mDistance) == 0
				&& connection.mEdgeAttributes.equals(mEdgeAttributes);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mNodes.hashCode();
			result = 31 * result + Double.hashCode(mDistance);
			result = 31 * result + mEdgeAttributes.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

	@Override
	public String toString() {
		return "connected(" + mNodeA + ", " + mNodeB + ")";
	}
}
