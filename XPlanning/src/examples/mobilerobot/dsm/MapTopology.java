package examples.mobilerobot.dsm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import examples.mobilerobot.dsm.exceptions.ConnectionNotFoundException;
import examples.mobilerobot.dsm.exceptions.LocationNodeNotFoundException;
import examples.mobilerobot.dsm.exceptions.NodeIDNotFoundException;

public class MapTopology implements Iterable<LocationNode> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Set<LocationNode> mNodes = new HashSet<>();
	private Set<Connection> mEdges = new HashSet<>();

	// For fast look-up
	private Map<String, LocationNode> mNodeIDs = new HashMap<>();
	private Map<LocationNode, Set<Connection>> mConnections = new HashMap<>();

	public MapTopology() {
		// MapTopology is initially empty
	}

	public void addLocationNode(LocationNode node) {
		mNodes.add(node);
		mNodeIDs.put(node.getNodeID(), node);
		mConnections.put(node, new HashSet<>());
	}

	public void connect(LocationNode nodeA, LocationNode nodeB, double distance) {
		connect(nodeA, nodeB, distance, new HashMap<>());
	}

	public void connect(LocationNode nodeA, LocationNode nodeB, double distance,
			Map<String, IEdgeAttribute> defaultEdgeAttributes) {
		Connection connection = new Connection(nodeA, nodeB, distance, defaultEdgeAttributes);
		mEdges.add(connection);
		mConnections.get(nodeA).add(connection);
		mConnections.get(nodeB).add(connection);
	}

	public LocationNode lookUpLocationNode(String nodeID) throws NodeIDNotFoundException {
		if (!mNodeIDs.containsKey(nodeID)) {
			throw new NodeIDNotFoundException(nodeID);
		}
		return mNodeIDs.get(nodeID);
	}

	public Set<Connection> getConnections(LocationNode node) throws LocationNodeNotFoundException {
		if (!mConnections.containsKey(node)) {
			throw new LocationNodeNotFoundException(node);
		}
		return mConnections.get(node);
	}

	public Connection getConnection(LocationNode nodeA, LocationNode nodeB)
			throws LocationNodeNotFoundException, ConnectionNotFoundException {
		Set<Connection> connections = mConnections.get(nodeA);
		for (Connection connection : connections) {
			if (connection.getOtherNode(nodeA).equals(nodeB)) {
				return connection;
			}
		}
		throw new ConnectionNotFoundException(nodeA, nodeB);
	}

	public Iterator<LocationNode> nodeIterator() {
		return mNodes.iterator();
	}

	public Iterator<Connection> connectionIterator() {
		return mEdges.iterator();
	}

	public int getNumNodes() {
		return mNodes.size();
	}

	public int getNumConnections() {
		return mEdges.size();
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
