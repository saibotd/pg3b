
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
		this.description = description == null || description.length() == 0 ? null : description;
	}

	public Action getAction () {
		return action;
	}

	public void setAction (Action action) {
		this.action = action;
	}

	public Object execute (Config config) {
		return action.execute(config, this, getState());
	}

	/**
	 * Returns the object that should be polled prior to this trigger being checked.
	 */
	abstract public Poller getPoller ();

	abstract public boolean isActive ();

	abstract public Object getState ();

	/**
	 * Returns true if this trigger is able to be checked.
	 */
	abstract public boolean isValid ();
}
