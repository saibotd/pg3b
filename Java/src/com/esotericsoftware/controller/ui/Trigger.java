
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
	 * Returns the name of the source of this trigger. Eg, "Joystick".
	 */
	abstract public String getSourceName ();

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

	public int hashCode () {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((action == null) ? 0 : action.hashCode());
		return result;
	}

	public boolean equals (Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Trigger other = (Trigger)obj;
		if (action == null) {
			if (other.action != null) return false;
		} else if (!action.equals(other.action)) return false;
		return true;
	}
}
