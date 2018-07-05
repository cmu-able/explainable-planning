package examples.mobilerobot.dsm;

public class LocationNodeNotFoundException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8559501860194977927L;

	public LocationNodeNotFoundException(LocationNode node) {
		super("Location node '" + node + "' is not found.");
	}
}
