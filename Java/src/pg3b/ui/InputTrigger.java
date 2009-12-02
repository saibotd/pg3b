
package pg3b.ui;

import net.java.games.input.Component;
import net.java.games.input.Controller;

public class InputTrigger {
	private String id;
	private String type;
	private String controllerName;

	public InputTrigger () {
	}

	public InputTrigger (Controller controller, Component component) {
		id = component.getIdentifier().toString();
		type = component.getIdentifier().getClass().getSimpleName().toLowerCase();
		controllerName = controller.getName();
		// controller.get
	}

	public String toString () {
		String id = this.id;
		if (id.equals(" ")) id = "Spacebar";
		return id + " " + type + ": " + controllerName;
	}
}
