
package com.esotericsoftware.controller.input;

import java.util.ArrayList;
import java.util.List;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Event;
import net.java.games.input.EventQueue;
import net.java.games.input.Component.Identifier;
import net.java.games.input.Controller.Type;

import com.esotericsoftware.controller.ui.InputTrigger;

/**
 * A JInput controller input device. This is used for all devices that are not keyboards, mice, or Xbox controllers.
 */
public class JInputJoystick implements InputDevice {
	private final Controller controller;

	public JInputJoystick (Controller controller) {
		this.controller = controller;
	}

	public boolean resetLastInput () {
		if (!controller.poll()) return false;
		EventQueue eventQueue = controller.getEventQueue();
		Event event = new Event();
		while (eventQueue.getNextEvent(event)) {
		}
		return true;
	}

	public JoystickInput getLastInput () {
		if (!controller.poll()) return null;
		EventQueue eventQueue = controller.getEventQueue();
		Event event = new Event();
		while (eventQueue.getNextEvent(event)) {
			Component component = event.getComponent();
			float value = event.getValue();
			if (value != 0) return new JoystickInput(controller, component);
		}
		return null;
	}

	public boolean poll () {
		return controller.poll();
	}

	public String toString () {
		return controller.getName();
	}

	public int hashCode () {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((controller == null) ? 0 : controller.hashCode());
		return result;
	}

	static public List<JInputJoystick> getAll () {
		ArrayList<JInputJoystick> list = new ArrayList();
		for (Controller controller : ControllerEnvironment.getDefaultEnvironment().getControllers()) {
			if (controller.getType() == Type.MOUSE) continue;
			if (controller.getType() == Type.KEYBOARD) continue;
			list.add(new JInputJoystick(controller));
		}
		return list;
	}

	public boolean equals (Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		JInputJoystick other = (JInputJoystick)obj;
		if (controller == null) {
			if (other.controller != null) return false;
		} else if (!controller.equals(other.controller)) return false;
		return true;
	}

	static public class JoystickInput implements Input {
		private String id;
		private String type;
		private String controllerName;
		private transient Component component;
		private transient JInputJoystick device;
		private transient float lastState = Float.NaN;

		public JoystickInput () {
		}

		public JoystickInput (Controller controller, Component component) {
			id = component.getIdentifier().toString();
			type = component.getIdentifier().getClass().getSimpleName().toLowerCase();
			controllerName = controller.getName();
		}

		public Float getState (InputTrigger trigger) {
			if (component == null) {
				getComponent();
				if (component == null) return null;
			}
			float state = component.getPollData();
			if (component.getIdentifier() instanceof Identifier.Axis) {
				if (!trigger.checkModifiers()) return null;
			} else {
				if (state != 0 && !trigger.checkModifiers()) return null;
			}
			if (state == lastState) return null;
			lastState = state;
			return state;
		}

		public Component getComponent () {
			if (component == null) {
				if (device == null) {
					getInputDevice();
					if (device == null) return null;
				}
				for (Component component : device.controller.getComponents())
					if (component.getIdentifier().toString().equals(id)) this.component = component;
				if (component == null) return null;
			}
			return component;
		}

		public JInputJoystick getInputDevice () {
			if (device != null) return device;
			for (Controller controller : ControllerEnvironment.getDefaultEnvironment().getControllers()) {
				if (!controller.getName().equals(controllerName)) continue;
				for (Component component : controller.getComponents()) {
					if (component.getIdentifier().toString().equals(id)) {
						device = new JInputJoystick(controller);
						return device;
					}
				}
			}
			return null;
		}

		public boolean isValid () {
			return getComponent() != null;
		}

		public String toString () {
			if (component == null) {
				getComponent();
				if (component == null) return "<none>";
			}
			return component.getName();
		}
	}
}
