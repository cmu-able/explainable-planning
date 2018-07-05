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
	private Map<String, INodeAttribute> mNodeAttributes = new HashMap<>();

	public LocationNode(String nodeID) {
		mNodeID = nodeID;
	}

	public void putNodeAttribute(String name, INodeAttribute value) {
		mNodeAttributes.put(name, value);
	}

	public String getNodeID() {
		return mNodeID;
	}

	public <E extends INodeAttribute> E getNodeAttribute(Class<E> attributeType, String name)
			throws NodeAttributeNotFoundException {
		if (!mNodeAttributes.containsKey(name)) {
			throw new NodeAttributeNotFoundException(name);
		}
		return attributeType.cast(mNodeAttributes.get(name));
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
		return node.mNodeID.equals(mNodeID) && node.mNodeAttributes.equals(mNodeAttributes);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mNodeID.hashCode();
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
