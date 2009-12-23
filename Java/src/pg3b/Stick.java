
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

	public Axis getAxisX () {
		return this == left ? Axis.leftStickX : Axis.rightStickX;
	}

	public Axis getAxisY () {
		return this == left ? Axis.leftStickY : Axis.rightStickY;
	}

	public String toString () {
		return friendlyName;
	}
}
