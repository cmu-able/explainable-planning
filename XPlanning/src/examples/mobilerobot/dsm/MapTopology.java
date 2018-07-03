package examples.mobilerobot.dsm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class MapTopology implements Iterable<LocationNode> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Set<LocationNode> mNodes = new HashSet<>();
	private Set<Connection> mEdges = new HashSet<>();
	// For fast look-up
	private Map<LocationNode, Set<Connection>> mConnections = new HashMap<>();

	public MapTopology() {

	}

	public void addLocationNode(LocationNode node) {
		mNodes.add(node);
		mConnections.put(node, new HashSet<>());
	}

	public void connect(LocationNode nodeA, LocationNode nodeB, double distance) {
		Connection connection = new Connection(nodeA, nodeB, distance);
		mEdges.add(connection);
		mConnections.get(nodeA).add(connection);
		mConnections.get(nodeB).add(connection);
	}

	public Set<Connection> getConnections(LocationNode node) {
		return mConnections.get(node);
	}

	@Override
	public Iterator<LocationNode> iterator() {
		return mNodes.iterator();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof MapTopology)) {
			return false;
		}
		MapTopology map = (MapTopology) obj;
		return map.mNodes.equals(mNodes) && map.mEdges.equals(mEdges);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mNodes.hashCode();
			result = 31 * result + mEdges.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}