package examples.mobilerobot.dsm.exceptions;

import examples.mobilerobot.dsm.LocationNode;

public class ConnectionNotFoundException extends MapTopologyException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7644949415328805287L;

	public ConnectionNotFoundException(LocationNode nodeA, LocationNode nodeB) {
		super("Location nodes '" + nodeA + "' and '" + nodeB + "' are not connected.");
	}
}
