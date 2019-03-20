package examples.mobilerobot.dsm;

import java.util.HashMap;
import java.util.Map;

import examples.mobilerobot.dsm.exceptions.NodeAttributeNotFoundException;

public class LocationNode {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private String mNodeID;
	private double mXCoord;
	private double mYCoord;
	private Map<String, INodeAttribute> mNodeAttributes = new HashMap<>();

	public LocationNode(String nodeID, double xCoord, double yCoord) {
		this(nodeID, xCoord, yCoord, new HashMap<>());
	}

	public LocationNode(String nodeID, double xCoord, double yCoord,
			Map<String, INodeAttribute> defaultNodeAttributes) {
		mNodeID = nodeID;
		mXCoord = xCoord;
		mYCoord = yCoord;
		mNodeAttributes.putAll(defaultNodeAttributes);
	}

	public void putNodeAttribute(String name, INodeAttribute value) {
		mNodeAttributes.put(name, value);
	}

	public String getNodeID() {
		return mNodeID;
	}

	public double getNodeXCoordinate() {
		return mXCoord;
	}

	public double getNodeYCoordinate() {
		return mYCoord;
	}

	public <E extends INodeAttribute> E getNodeAttribute(Class<E> attributeType, String name)
			throws NodeAttributeNotFoundException {
		return attributeType.cast(getGenericNodeAttribute(name));
	}

	public <E extends INodeAttribute> E getNodeAttribute(Class<E> attributeType, String name, E defaultValue) {
		if (!mNodeAttributes.containsKey(name)) {
			return defaultValue;
		}
		return attributeType.cast(mNodeAttributes.get(name));
	}

	public INodeAttribute getGenericNodeAttribute(String name) throws NodeAttributeNotFoundException {
		if (!mNodeAttributes.containsKey(name)) {
			throw new NodeAttributeNotFoundException(name);
		}
		return mNodeAttributes.get(name);
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
		return node.mNodeID.equals(mNodeID) && Double.compare(node.mXCoord, mXCoord) == 0
				&& Double.compare(node.mYCoord, mYCoord) == 0 && node.mNodeAttributes.equals(mNodeAttributes);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mNodeID.hashCode();
			result = 31 * result + Double.hashCode(mXCoord);
			result = 31 * result + Double.hashCode(mYCoord);
			result = 31 * result + mNodeAttributes.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

	@Override
	public String toString() {
		return mNodeID;
	}
}
