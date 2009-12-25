
package com.esotericsoftware.controller.ui;

/**
 * Decides when an {@link Action} should be executed.
 */
abstract public class Trigger {
	private String description;
	private Action action;

	public String getDescription () {
		return description;
	}

	public void setDescription (String description) {
		this.description = description;
	}

	public Action getAction () {
		return action;
	}

	public void setAction (Action action) {
		this.action = action;
	}

	/**
	 * Returns the object that should be polled prior to this trigger being checked.
	 */
	abstract public Poller getPoller ();

	/**
	 * Checks this trigger and returns a non-null object if the trigger was activated.
	 */
	abstract public Object check ();

	/**
	 * Returns true if this trigger is able to be checked.
	 */
	abstract public boolean isValid ();
}
