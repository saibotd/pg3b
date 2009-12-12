
package pg3b.ui;

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
	 * Checks if the action should be executed.
	 * @return Returns null if the action should not be executed. Otherwise, returns a payload that represents what the state that
	 *         caused the trigger to want to execute its action.
	 */
	abstract public Object poll ();

	/**
	 * Returns true if this trigger is ready to be polled.
	 */
	abstract public boolean isValid ();
}
