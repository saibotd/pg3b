
package com.esotericsoftware.controller.device;

import java.io.IOException;
import java.util.HashMap;

import com.esotericsoftware.controller.util.Listeners;

abstract public class Device {
	static private final HashMap<String, Target> nameToTarget = new HashMap();
	static {
		for (Axis axis : Axis.values()) {
			nameToTarget.put(axis.name().toLowerCase(), axis);
			String friendlyName = axis.toString().toLowerCase();
			nameToTarget.put(friendlyName, axis);
			nameToTarget.put(friendlyName.substring(0, friendlyName.length() - 5), axis);
			if (axis.getAlias() != null) nameToTarget.put(axis.getAlias().toLowerCase(), axis);
		}
		for (Button button : Button.values()) {
			nameToTarget.put(button.name().toLowerCase(), button);
			String friendlyName = button.toString().toLowerCase();
			nameToTarget.put(friendlyName, button);
			nameToTarget.put(friendlyName.substring(0, friendlyName.length() - 7), button);
			if (button.getAlias() != null) nameToTarget.put(button.getAlias().toLowerCase(), button);
		}
	}

	protected final boolean[] buttonStates = new boolean[Button.values().length];
	protected final float[] axisStates = new float[Axis.values().length];

	private final Listeners<Listener> listeners = new Listeners(Listener.class);
	private final Deadzone[] deadzones = new Deadzone[Stick.values().length];

	public Device () {
		super();
	}

	/**
	 * Sets the button state.
	 * @throws IOException When communication with the device fails.
	 */
	abstract public void set (Button button, boolean pressed) throws IOException;

	/**
	 * Sets the axis state.
	 * @throws IOException When communication with the device fails.
	 */
	abstract public void set (Axis axis, float state) throws IOException;

	/**
	 * Sets the button or axis state. If the target is an axis, it will be to 0 (false) or 1 (true).
	 * @throws IOException When communication with the device fails.
	 */
	public void set (Target target, boolean pressed) throws IOException {
		if (target == null) throw new IllegalArgumentException("target cannot be null.");
		if (target instanceof Button)
			set((Button)target, pressed);
		else if (target instanceof Axis)
			set((Axis)target, pressed ? 1 : 0);
		else
			throw new IllegalArgumentException("target must be a button or axis.");
	}

	/**
	 * Sets the button or axis state. If the target is a button, it will be to not pressed (zero) or pressed (nonzero).
	 * @throws IOException When communication with the device fails.
	 */
	public void set (Target target, float state) throws IOException {
		if (target == null) throw new IllegalArgumentException("target cannot be null.");
		if (target instanceof Button)
			set((Button)target, state != 0);
		else if (target instanceof Axis)
			set((Axis)target, state);
		else
			throw new IllegalArgumentException("target must be a button or axis.");
	}

	/**
	 * Sets the button or axis state. If the target is an axis, it will be to 0 (false) or 1 (true).
	 * @throws IOException When communication with the device fails.
	 */
	public void set (String target, boolean pressed) throws IOException {
		if (target == null) throw new IllegalArgumentException("target cannot be null.");
		set(getTarget(target), pressed);
	}

	/**
	 * Sets the button or axis state. If the target is a button, it will be to not pressed (zero) or pressed (nonzero).
	 * @throws IOException When communication with the device fails.
	 */
	public void set (String target, float state) throws IOException {
		if (target == null) throw new IllegalArgumentException("target cannot be null.");
		set(getTarget(target), state);
	}

	/**
	 * Sets the x and y axes for the specified stick.
	 * @throws IOException When communication with the device fails.
	 */
	public void set (Stick stick, float stateX, float stateY) throws IOException {
		if (stick == null) throw new IllegalArgumentException("stick cannot be null.");
		Axis axisX = stick == Stick.left ? Axis.leftStickX : Axis.rightStickX;
		Axis axisY = stick == Stick.left ? Axis.leftStickY : Axis.rightStickY;
		set(axisX, stateX);
		set(axisY, stateY);
	}

	/**
	 * Sets the x and y axes for the specified stick.
	 * @throws IOException When communication with the device fails.
	 */
	public void set (String stick, float stateX, float stateY) throws IOException {
		if (stick == null) throw new IllegalArgumentException("stick cannot be null.");
		stick = stick.toLowerCase();
		if (stick.equals("leftstick") || stick.equals("left") || stick.equals("l"))
			set(Stick.left, stateX, stateY);
		else if (stick.equals("rightstick") || stick.equals("right") || stick.equals("r"))
			set(Stick.right, stateX, stateY);
		else
			throw new IllegalArgumentException("stick must be leftStick or rightStick.");
	}

	/**
	 * Returns the last state of the button as set by the device.
	 */
	public boolean get (Button button) {
		if (button == null) throw new IllegalArgumentException("button cannot be null.");
		return buttonStates[button.ordinal()];
	}

	/**
	 * Returns the last state of the axis as set by the device.
	 */
	public float get (Axis axis) {
		if (axis == null) throw new IllegalArgumentException("axis cannot be null.");
		return axisStates[axis.ordinal()];
	}

	/**
	 * Returns the last state of the button or axis as set by the device. If the taret is a button, either 0 (not pressed) or 1
	 * (pressed) is returned.
	 */
	public float get (Target target) {
		if (target == null) throw new IllegalArgumentException("target cannot be null.");
		if (target instanceof Button)
			return get((Button)target) ? 1 : 0;
		else if (target instanceof Axis)
			return get((Axis)target);
		else
			throw new IllegalArgumentException("target must be a button or axis.");
	}

	/**
	 * Returns the last state of the button or axis as set by the device. If the taret is a button, either 0 (not pressed) or 1
	 * (pressed) is returned.
	 */
	public float get (String target) {
		return get(getTarget(target));
	}

	public void setDeadzone (Stick stick, Deadzone deadzone) {
		if (stick == null) throw new IllegalArgumentException("stick cannot be null.");
		deadzones[stick.ordinal()] = deadzone;
	}

	public float getDeflection (Axis axis, float state) {
		Stick stick = axis.getStick();
		if (stick == null) return state;
		Deadzone deadzone = deadzones[stick.ordinal()];
		if (deadzone == null) return state;
		Axis axisX = stick.getAxisX();
		float x, y;
		if (axis == axisX) {
			x = state;
			y = axisStates[stick.getAxisY().ordinal()];
		} else {
			x = axisStates[axisX.ordinal()];
			y = state;
		}
		float[] deflection = deadzone.toDeflection(x, y);
		if (axis == axisX) {
			if (deflection[1] != y) {
				// BOZO - Set y.
			}
			return deflection[0];
		} else {
			if (deflection[0] != x) {
				// BOZO - Set x.
			}
			return deflection[1];
		}
	}

	/**
	 * Closes the connection with this device. No further communication will be possible with this device instance.
	 */
	abstract public void close ();

	/**
	 * Sets all buttons to released and all axes to zero.
	 */
	public void reset () throws IOException {
		for (Button button : Button.values())
			set(button, false);
		for (Axis axis : Axis.values())
			set(axis, 0);
	}

	/**
	 * Adds a listener to be notified when the device manipulates a button or axis.
	 */
	public void addListener (Listener listener) {
		listeners.addListener(listener);
	}

	public void removeListener (Listener listener) {
		listeners.removeListener(listener);
	}

	protected void notifyButtonChanged (Button button, boolean pressed) {
		Listener[] listeners = this.listeners.toArray();
		for (int i = 0, n = listeners.length; i < n; i++)
			listeners[i].buttonChanged(button, pressed);
	}

	protected void notifyAxisChanged (Axis axis, float state) {
		Listener[] listeners = this.listeners.toArray();
		for (int i = 0, n = listeners.length; i < n; i++)
			listeners[i].axisChanged(axis, state);
	}

	/**
	 * Returns the target with the specified name or alias (case insensitive).
	 */
	static public Target getTarget (String name) {
		if (name == null) throw new IllegalArgumentException("name cannot be null.");
		return nameToTarget.get(name.trim().toLowerCase());
	}

	/**
	 * Listener to be notified when the device manipulates a button or axis.
	 */
	static public class Listener {
		public void buttonChanged (Button button, boolean pressed) {
		}

		public void axisChanged (Axis axis, float state) {
		}
	}
}
