
package pg3b;

public enum Button implements Target {
	// Ordinals defined by firmware.
	a("A"), //
	b("B"), //
	x("X"), //
	y("Y"), //
	up("Up"), //
	down("Down"), //
	left("Left"), //
	right("Right"), //
	leftShoulder("Left Shoulder"), //
	rightShoulder("Right Shoulder"), //
	leftStick("Left Stick"), //
	rightStick("Right Stick"), //
	start("Start"), //
	guide("Guide"), //
	back("Back");

	private final String friendlyName;

	private Button (String friendlyName) {
		this.friendlyName = friendlyName + " button";
	}

	public String toString () {
		return friendlyName;
	}
}
