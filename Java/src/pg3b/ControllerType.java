
package pg3b;

/**
 * The type of controller the PG3B is wired to. This is required for the PG3B to output the correct signals to properly control
 * various types of controllers.
 */
public enum ControllerType {
	wired(0), wireless(1);

	byte code;

	private ControllerType (int code) {
		this.code = (byte)code;
	}
}
