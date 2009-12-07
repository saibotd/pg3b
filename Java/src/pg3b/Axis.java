
package pg3b;

/**
 * Represents a PG3B axis.
 */
public enum Axis implements Target {
	// Ordinals defined by firmware.
	leftStickX("Left Stick X", "LX"), //
	leftStickY("Left Stick Y", "LY"), //
	rightStickX("Right Stick X", "RY"), //
	rightStickY("Right Stick Y", "RX"), //
	leftTrigger("Left Trigger", "LT"), //
	rightTrigger("Right Trigger", "RT");

	private final String friendlyName;
	private final String alias;

	private Axis (String friendlyName, String alias) {
		this.friendlyName = friendlyName + " axis";
		this.alias = alias;
	}

	public String getAlias () {
		return alias;
	}

	public String toString () {
		return friendlyName;
	}
}
