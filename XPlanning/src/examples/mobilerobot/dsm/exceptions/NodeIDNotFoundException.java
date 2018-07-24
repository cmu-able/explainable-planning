package examples.mobilerobot.dsm.exceptions;

public class NodeIDNotFoundException extends MapTopologyException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5643066074680310048L;

	public NodeIDNotFoundException(String nodeID) {
		super("Node ID '" + nodeID + "' is not found.");
	}

}
