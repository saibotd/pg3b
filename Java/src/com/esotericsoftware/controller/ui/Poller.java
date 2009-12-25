
package com.esotericsoftware.controller.ui;

/**
 * Represents an object that can updates its state periodically.
 */
public interface Poller {
	/**
	 * The state of the object should be updated to reflect any changes since the last update.
	 */
	public boolean poll ();
}
