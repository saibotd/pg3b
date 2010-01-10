
package com.esotericsoftware.controller.input;


/**
 * Represents an input from an input device. Eg, a button press or axis movement.
 */
public interface Input {
	/**
	 * Returns true if the input and the input's device is available.
	 */
	public boolean isValid ();

	/**
	 * Returns true if this input can be mapped to the full range of an axis. If false, it will only be possible to map this input
	 * to an axis direction.
	 */
	public boolean isAxis ();

	public boolean isAxisX ();

	/**
	 * Returns the device to which this input belongs.
	 */
	public InputDevice getInputDevice ();

	/**
	 * Returns the value for this input.
	 */
	public float getState ();

	/**
	 * Returns the value for the axis that makes up the x and y axis pair, or zero if there is no corresponding axis.
	 */
	public float getOtherState ();
}
