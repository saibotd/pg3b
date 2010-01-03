
package com.esotericsoftware.controller.ui;

/**
 * Decides when an {@link Action} should be executed.
 */
abstract public class Trigger {
	private Action action;

	public Action getAction () {
		return action;
	}

	public void setAction (Action action) {
		this.action = action;
	}

	public Object execute (Config config) {
		return action.execute(config, this, isActive(), getPayload());
	}

	/**
	 * Returns the object that should be polled prior to this trigger being checked.
	 */
	abstract public Poller getPoller ();

	abstract public boolean isActive ();

	abstract public Object getPayload ();

	/**
	 * Returns true if this trigger is able to be checked.
	 */
	abstract public boolean isValid ();
}
