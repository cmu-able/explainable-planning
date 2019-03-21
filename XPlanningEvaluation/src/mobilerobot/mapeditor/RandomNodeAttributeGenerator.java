package mobilerobot.mapeditor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import examples.mobilerobot.dsm.Connection;
import examples.mobilerobot.dsm.INodeAttribute;
import examples.mobilerobot.dsm.LocationNode;
import examples.mobilerobot.dsm.MapTopology;
import examples.mobilerobot.dsm.exceptions.LocationNodeNotFoundException;
import examples.mobilerobot.dsm.exceptions.MapTopologyException;

public class RandomNodeAttributeGenerator<E extends INodeAttribute> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private String mNodeAttrName;
	private Randomizer<E> mNodeAttrRandomizer;
	private Randomizer<LocationNode> mNodeRandomizer;

	public RandomNodeAttributeGenerator(String nodeAttrName, E[] nodeAttrValues, LocationNode[] auxLocNodes,
			long seed) {
		this(nodeAttrName, nodeAttrValues, null, auxLocNodes, seed);
	}

	public RandomNodeAttributeGenerator(String nodeAttrName, E[] nodeAttrValues, double[] nodeAttrProbs,
			LocationNode[] auxLocNodes, long seed) {
		mNodeAttrName = nodeAttrName;
		mNodeAttrRandomizer = nodeAttrProbs == null ? new Randomizer<>(nodeAttrValues, seed)
				: new Randomizer<>(nodeAttrValues, nodeAttrProbs, seed);
		mNodeRandomizer = new Randomizer<>(auxLocNodes, seed * seed);
	}

	public void randomlyAssignNodeAttributeValues(MapTopology mapTopology) {
		Iterator<LocationNode> nodeIter = mapTopology.nodeIterator();
		while (nodeIter.hasNext()) {
			LocationNode locNode = nodeIter.next();
			E randomNodeAttrValue = mNodeAttrRandomizer.randomSample();
			locNode.putNodeAttribute(mNodeAttrName, randomNodeAttrValue);
		}
	}

	public void randomlyAssignNodeAttributeValues(MapTopology mapTopology, int groupSize, E[] nodeAttrFilter)
			throws MapTopologyException {
		// These auxIniNodes are NOT instances from the given mapTopology
		// They were created at the construction of this random generator
		List<LocationNode> auxIniNodes = randomlyAssignAttributeValuesToIniNodes(mapTopology, nodeAttrFilter,
				groupSize);
		List<LocationNode> mapIniNodes = retrieveMapNodesFromAuxNodes(auxIniNodes, mapTopology);
		propagateAttributeValuesFromIniNodes(mapTopology, mapIniNodes, groupSize);
	}

	private List<LocationNode> randomlyAssignAttributeValuesToIniNodes(MapTopology mapTopology, E[] nodeAttrFilter,
			int groupSize) {
		List<LocationNode> iniNodes = new ArrayList<>();

		for (E nodeAttrValue : nodeAttrFilter) {
			double prob = mNodeAttrRandomizer.getProbabilityOf(nodeAttrValue);
			int numIniNodes = (int) Math.round(prob * mapTopology.getNumNodes() / groupSize);

			for (int j = 0; j < numIniNodes; j++) {
				LocationNode randomLocNode = mNodeRandomizer.randomSample();
				randomLocNode.putNodeAttribute(mNodeAttrName, nodeAttrValue);
				iniNodes.add(randomLocNode);
			}
		}
		return iniNodes;
	}

	private List<LocationNode> retrieveMapNodesFromAuxNodes(List<LocationNode> auxNodes, MapTopology mapTopology)
			throws MapTopologyException {
		List<LocationNode> mapNodes = new ArrayList<>();
		for (LocationNode auxNode : auxNodes) {
			LocationNode mapNode = mapTopology.lookUpLocationNode(auxNode.getNodeID());
			// Copy the attribute value of auxNode to mapNode
			mapNode.putNodeAttribute(mNodeAttrName, auxNode.getGenericNodeAttribute(mNodeAttrName));
			mapNodes.add(mapNode);
		}
		return mapNodes;
	}

	private void propagateAttributeValuesFromIniNodes(MapTopology mapTopology, List<LocationNode> iniNodes,
			int groupSize) throws MapTopologyException {
		for (LocationNode iniNode : iniNodes) {
			Set<LocationNode> visitedNodes = new HashSet<>();
			INodeAttribute nodeAttrValue = iniNode.getGenericNodeAttribute(mNodeAttrName);
			propagateAttributeValueFromNodeDFS(mapTopology, iniNode, nodeAttrValue, groupSize, groupSize - 1,
					visitedNodes);
		}
	}

	private int propagateAttributeValueFromNodeDFS(MapTopology mapTopology, LocationNode node,
			INodeAttribute nodeAttrValue, int groupSize, int depth, Set<LocationNode> visitedNodes)
			throws LocationNodeNotFoundException {
		// Assign attribute value to node; mark node as visited
		node.putNodeAttribute(mNodeAttrName, nodeAttrValue);
		visitedNodes.add(node);

		if (depth == 0) {
			// Base case: Propagation finishes at the current node
			return 0;
		}

		// Need to propagate to the next {depth} nodes

		int remainingDepth = depth; // remaining number of nodes to propagate to
		Set<Connection> connections = mapTopology.getConnections(node);

		for (Connection connection : connections) {
			LocationNode otherNode = connection.getOtherNode(node);

			if (!visitedNodes.contains(otherNode)) {
				// Adjacent node has not been visited; propagate attribute value to that node

				if (remainingDepth == depth) {
					// DFS-visit first adjacent node
					remainingDepth = propagateAttributeValueFromNodeDFS(mapTopology, otherNode, nodeAttrValue,
							groupSize, depth - 1, visitedNodes);
				} else {
					// DFS-visit 2nd, 3rd, ... adjacent node
					remainingDepth = propagateAttributeValueFromNodeDFS(mapTopology, otherNode, nodeAttrValue,
							groupSize, remainingDepth, visitedNodes);
				}

				if (depth + 1 < groupSize) {
					// Current node is NOT the initial node of the propagation chain
					// Send remainingDepth to the prior recursive caller
					return remainingDepth;
				}

				// Current node is the initial node of the propagation chain

				if (remainingDepth == 0) {
					// Propagation is done
					return 0;
				}

				// More nodes to propagate to
				// Propagate to the unvisited next adjacent node of current node
			}
		}

		// No more unvisited adjacent node
		// Send remainingDepth to the prior recursive caller
		return remainingDepth;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof RandomNodeAttributeGenerator<?>)) {
			return false;
		}
		RandomNodeAttributeGenerator<?> generator = (RandomNodeAttributeGenerator<?>) obj;
		return generator.mNodeAttrName.equals(mNodeAttrName)
				&& generator.mNodeAttrRandomizer.equals(mNodeAttrRandomizer);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mNodeAttrName.hashCode();
			result = 31 * result + mNodeAttrRandomizer.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
