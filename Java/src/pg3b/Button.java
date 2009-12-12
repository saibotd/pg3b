
package pg3b;

/**
 * Represents a PG3B button.
 */
public enum Button implements Target {
	// Ordinals defined by firmware.
	a("A", null), //
	b("B", null), //
	x("X", null), //
	y("Y", null), //
	up("Up", "U"), //
	down("Down", "D"), //
	left("Left", "L"), //
	right("Right", "R"), //
	leftShoulder("Left Shoulder", "LB"), //
	rightShoulder("Right Shoulder", "RB"), //
	leftStick("Left Stick", "LS"), //
	rightStick("Right Stick", "RS"), //
	start("Start", "S"), //
	guide("Guide", "G"), //
	back("Back", null);

	private final String friendlyName;
	private final String alias;

	private Button (String friendlyName, String alias) {
		this.friendlyName = friendlyName + " button";
		this.alias = alias;
	}

	public String getAlias () {
		return alias;
	}

	public String toString () {
		return friendlyName;
	}
}
