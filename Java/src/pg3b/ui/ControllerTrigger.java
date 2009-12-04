
package pg3b.ui;

import net.java.games.input.Component;
import net.java.games.input.Controller;

public class ControllerTrigger extends Trigger {
	private String id;
	private String type;
	private String controllerName;
	private boolean shift, ctrl, alt;

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

	public boolean getShift () {
		return shift;
	}

	public void setShift (boolean shift) {
		this.shift = shift;
	}

	public boolean getCtrl () {
		return ctrl;
	}

	public void setCtrl (boolean ctrl) {
		this.ctrl = ctrl;
	}

	public boolean getAlt () {
		return alt;
	}

	public void setAlt (boolean alt) {
		this.alt = alt;
	}

	public String toString () {
		String id = this.id;
		if (id.equals(" ")) id = "Spacebar";
		if (id.length() == 1) id = id.toUpperCase();
		StringBuilder buffer = new StringBuilder();
		if (getCtrl()) buffer.append("ctrl+");
		if (getAlt()) buffer.append("alt+");
		if (getShift()) buffer.append("shift+");
		buffer.append(id);
		buffer.append(' ');
		buffer.append(type);
		buffer.append(": ");
		buffer.append(controllerName);
		return buffer.toString();
	}
}
