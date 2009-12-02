
package pg3b;

public enum Axis implements Target {
	// Ordinals defined by firmware.
	leftStickX("Left Stick X"), //
	leftStickY("Left Stick Y"), //
	rightStickX("Right Stick X"), //
	rightStickY("Right Stick Y"), //
	leftTrigger("Left Trigger"), //
	rightTrigger("Right Trigger");

	private final String friendlyName;

	private Axis (String friendlyName) {
		this.friendlyName = friendlyName;
	}

	public String toString () {
		return friendlyName;
	}
}