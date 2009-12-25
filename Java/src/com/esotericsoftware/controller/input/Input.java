
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
	 * Returns the device to which this input belongs.
	 */
	public InputDevice getInputDevice ();

	/**
	 * Returns the value for this input if it has changed since the last call to this method, or null if the value has not changed.
	 */
	public Float getState (InputTrigger trigger);
}
