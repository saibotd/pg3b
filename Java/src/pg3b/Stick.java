
package pg3b;

/**
 * Represents a PG3B thumbstick.
 */
public enum Stick {
	left("Left"), //
	right("Right");

	private final String friendlyName;

	private Stick (String friendlyName) {
		this.friendlyName = friendlyName;
	}

	public String toString () {
		return friendlyName;
	}
}
