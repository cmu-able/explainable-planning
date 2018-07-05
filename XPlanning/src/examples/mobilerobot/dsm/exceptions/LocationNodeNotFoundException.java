package examples.mobilerobot.dsm.exceptions;

import examples.mobilerobot.dsm.LocationNode;

public class LocationNodeNotFoundException extends MapTopologyException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8559501860194977927L;

	public LocationNodeNotFoundException(LocationNode node) {
		super("Location node '" + node + "' is not found.");
	}
}
