
package com.esotericsoftware.controller.input;

import com.esotericsoftware.controller.ui.InputTrigger;

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

	/**
	 * Returns the device to which this input belongs.
	 */
	public InputDevice getInputDevice ();

	/**
	 * Returns the value for this input.
	 */
	public float getState ();
}
