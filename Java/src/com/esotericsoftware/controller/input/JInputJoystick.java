
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
		boolean isWindows = System.getProperty("os.name").toLowerCase().contains("windows");
		for (Controller controller : ControllerEnvironment.getDefaultEnvironment().getControllers()) {
			if (controller.getType() == Type.MOUSE) continue;
			if (controller.getType() == Type.KEYBOARD) continue;
			if (isWindows && controller.getName().equals("Controller (Xbox 360 Wireless Receiver for Windows)")) continue;
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
		private transient Component otherComponent;
		private transient JInputJoystick device;

		public JoystickInput () {
		}

		public JoystickInput (Controller controller, Component component) {
			id = component.getIdentifier().toString();
			type = component.getIdentifier().getClass().getSimpleName().toLowerCase();
			controllerName = controller.getName();
		}

		public float getState () {
			Component component = getComponent();
			if (component == null) return 0;
			return component.getPollData();
		}

		public float getOtherState () {
			Component otherComponent = getOtherComponent();
			return otherComponent.getPollData();
		}

		public Component getComponent () {
			if (component == null) {
				JInputJoystick device = getInputDevice();
				if (device == null) return null;
				for (Component component : device.controller.getComponents())
					if (component.getIdentifier().toString().equals(id)) this.component = component;
			}
			return component;
		}

		public Component getOtherComponent () {
			if (otherComponent == null) {
				JInputJoystick device = getInputDevice();
				if (device == null) return null;

				Identifier otherID;
				Identifier id = getComponent().getIdentifier();
				if (id == Identifier.Axis.RX)
					otherID = Identifier.Axis.RY;
				else if (id == Identifier.Axis.RY)
					otherID = Identifier.Axis.RX;
				else if (id == Identifier.Axis.X)
					otherID = Identifier.Axis.Y;
				else if (id == Identifier.Axis.Y)
					otherID = Identifier.Axis.X;
				else if (id == Identifier.Axis.X)
					otherID = Identifier.Axis.Y;
				else
					return null;

				for (Component component : device.controller.getComponents())
					if (component.getIdentifier().toString().equals(otherID)) this.otherComponent = component;
				if (otherComponent == null) return null;
			}
			return otherComponent;
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

		public boolean isAxis () {
			Component component = getComponent();
			if (component == null) return false;
			return component.getIdentifier() instanceof Identifier.Axis;
		}

		public boolean isAxisX () {
			Component component = getComponent();
			if (component == null) return false;
			Identifier id = component.getIdentifier();
			return id == Identifier.Axis.RX || id == Identifier.Axis.X;
		}

		public String toString () {
			Component component = getComponent();
			if (component == null) return "<none>";
			return component.getName();
		}
	}
}
