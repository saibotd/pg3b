
package pg3b.ui;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Controller.Type;

// BOZO - Grab mouse and keyboard during capture with a hotkey to exit.
// BOZO - Implement ctrl, alt, shift modifiers.

public class ControllerTrigger extends Trigger {
	private String id;
	private String type;
	private String controllerName;
	private boolean shift, ctrl, alt;
	private transient Controller controller;
	private transient Component component;
	private transient float lastState = Float.MAX_VALUE;

	public String getID () {
		return id;
	}

	public void setID (String id) {
		this.id = id;
		component = null;
		lookupComponent();
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
		component = null;
		lookupComponent();
	}

	public void setComponent (Controller controller, Component component) {
		id = component.getIdentifier().toString();
		type = component.getIdentifier().getClass().getSimpleName().toLowerCase();
		controllerName = controller.getName();
		component = null;
		lookupComponent();
	}

	public Component getComponent () {
		return component;
	}

	private void lookupComponent () {
		if (component != null) return;
		if (controllerName == null || id == null) return;
		for (Controller controller : ControllerEnvironment.getDefaultEnvironment().getControllers()) {
			if (!controller.getName().equals(controllerName)) continue;
			for (Component component : controller.getComponents()) {
				if (component.getIdentifier().toString().equals(id)) {
					this.controller = controller;
					this.component = component;
					lastState = Float.MAX_VALUE;
					return;
				}
			}
		}
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

	public boolean isValid () {
		lookupComponent();
		return getComponent() != null;
	}

	public boolean poll () {
		if (component == null) return false;
		controller.poll();
		float state = component.getPollData();
		if (state != lastState) {
			if (controller.getType() == Type.MOUSE) {
				// BOZO - Do fancy computation to go from mouse position delta to axis deflection! Will need a configuration GUI.
				if (state != 0) state = state > 0 ? 1 : -1;
			}
			lastState = state;
			if (getAction().execute(state)) return true;
		}
		return false;
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
