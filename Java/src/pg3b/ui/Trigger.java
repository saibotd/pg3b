
package pg3b.ui;

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
}
