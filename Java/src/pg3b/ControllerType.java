
package pg3b;

/**
 * The type of controller the PG3B is wired to. This is required for the PG3B to output the correct signals to properly control
 * various types of controllers.
 */
public enum ControllerType {
	wired(0, "Wired"), wireless(1, "Wireless");

	byte code;
	private final String friendlyName;

	private ControllerType (int code, String friendlyName) {
		this.friendlyName = friendlyName;
		this.code = (byte)code;
	}

	public String toString () {
		return friendlyName;
	}
}
