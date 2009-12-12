
package pg3b.ui;

import java.util.ArrayList;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Component.Identifier;
import net.java.games.input.Controller.Type;
import pg3b.ui.swing.PG3BUI;

/**
 * A trigger that executes its action based on the state of a JInput controller (keyboard, mouse, joystick, etc, essentially any
 * peripheral).
 */
public class ControllerTrigger extends Trigger {
	private String id;
	private String type;
	private String controllerName;
	private boolean shift, ctrl, alt;
	private transient Controller[] controllers;
	private transient Component[] components;
	private transient float[] lastStates;

	public ControllerTrigger () {
	}

	public ControllerTrigger (Controller controller, Component component, Action action) {
		setComponent(controller, component);
		setAction(action);
	}

	/**
	 * Returns the ID of the button or axis.
	 */
	public String getId () {
		return id;
	}

	/**
	 * Returns the type of controller (mouse, keyboard, gamepad, etc).
	 */
	public String getType () {
		return type;
	}

	public String getControllerName () {
		return controllerName;
	}

	/**
	 * Sets this trigger to execute its action based on the specified component.
	 */
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
		if (controllersList.isEmpty()) return;
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

	public Float poll () {
		Controller[] controllers = this.controllers;
		Component[] components = this.components;
		float[] lastStates = this.lastStates;
		if (controllers == null || components == null || lastStates == null) return null;
		PG3BUI pg3bui = PG3BUI.instance;
		for (int i = 0, n = controllers.length; i < n; i++) {
			Controller controller = controllers[i];
			if (!controller.poll()) {
				this.components = null;
				return null;
			}
			float state = components[i].getPollData();
			boolean fire = false;
			if (components[i].getIdentifier() instanceof Identifier.Button) {
				fire = state != lastStates[i];
			} else {
				fire = state != 0 && state != lastStates[i];
			}
			if (fire) {
				if (ctrl && !pg3bui.isCtrlDown()) continue;
				if (alt && !pg3bui.isAltDown()) continue;
				if (shift && !pg3bui.isShiftDown()) continue;
				if (controller.getType() == Type.MOUSE) {
					// BOZO - Do fancy computation to go from mouse position delta to axis deflection! Will need a configuration GUI.
					//if (state != 0) state = state > 0 ? 1 : -1;
				}
				lastStates[i] = state;
				return state;
			}
		}
		return null;
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
