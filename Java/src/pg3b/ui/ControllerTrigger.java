
package pg3b.ui;

import net.java.games.input.Component;
import net.java.games.input.Controller;

public class ControllerTrigger extends Trigger {
	private String id;
	private String type;
	private String controllerName;

	public String getID () {
		return id;
	}

	public void setID (String id) {
		this.id = id;
	}

	public String getType () {
		return type;
	}

	public void setType (String type) {
		this.type = type;
	}

	public String getControllerName () {
		return controllerName;
	}

	public void setControllerName (String controllerName) {
		this.controllerName = controllerName;
	}

	public void setComponent (Controller controller, Component component) {
		id = component.getIdentifier().toString();
		type = component.getIdentifier().getClass().getSimpleName().toLowerCase();
		controllerName = controller.getName();
		// BOZO - Need more to look up controller?
	}

	public String toString () {
		String id = this.id;
		if (id.equals(" ")) id = "Spacebar";
		if (id.length() == 1) id = id.toUpperCase();
		return id + " " + type + ": " + controllerName;
	}
}
