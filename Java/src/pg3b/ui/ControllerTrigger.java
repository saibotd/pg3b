
package pg3b.ui;

import java.util.ArrayList;

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
	private transient Controller[] controllers;
	private transient Component[] components;
	private transient float[] lastStates;

	public String getID () {
		return id;
	}

	public void setID (String id) {
		this.id = id;
		lookupComponent(true);
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
		lookupComponent(true);
	}

	public void setComponent (Controller controller, Component component) {
		id = component.getIdentifier().toString();
		type = component.getIdentifier().getClass().getSimpleName().toLowerCase();
		controllerName = controller.getName();
		lookupComponent(true);
	}

	public Controller[] getControllers () {
		lookupComponent(true);
		return controllers;
	}

	private void lookupComponent (boolean reset) {
		if (reset) {
			components = null;
			controllers = null;
			lastStates = null;
		}
		if (components != null) return;
		if (controllerName == null || id == null) return;
		// There may be more than one controller with the same name, port number, and component id, so we will monitor them all.
		ArrayList<Controller> controllersList = new ArrayList();
		ArrayList<Component> componentsList = new ArrayList();
		for (Controller controller : ControllerEnvironment.getDefaultEnvironment().getControllers()) {
			if (!controller.getName().equals(controllerName)) continue;
			for (Component component : controller.getComponents()) {
				if (component.getIdentifier().toString().equals(id)) {
					controllersList.add(controller);
					componentsList.add(component);
				}
			}
		}
		controllers = controllersList.toArray(new Controller[controllersList.size()]);
		components = componentsList.toArray(new Component[componentsList.size()]);
		lastStates = new float[components.length];
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
		lookupComponent(false);
		return components != null;
	}

	public boolean poll () {
		if (components == null) return false;
		for (int i = 0, n = controllers.length; i < n; i++) {
			Controller controller = controllers[i];
			if (!controller.poll()) {
				components = null;
				continue;
			}
			float state = components[i].getPollData();
			if (state != lastStates[i]) {
				if (controller.getType() == Type.MOUSE) {
					// BOZO - Do fancy computation to go from mouse position delta to axis deflection! Will need a configuration GUI.
					if (state != 0) state = state > 0 ? 1 : -1;
				}
				lastStates[i] = state;
				if (getAction().execute(state)) return true;
			}
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
