
package com.esotericsoftware.controller.ui;

import java.io.IOException;

import com.esotericsoftware.controller.device.Axis;
import com.esotericsoftware.controller.device.Button;
import com.esotericsoftware.controller.device.Device;
import com.esotericsoftware.controller.device.Target;
import com.esotericsoftware.controller.input.Input;
import com.esotericsoftware.controller.input.Mouse;
import com.esotericsoftware.controller.input.Mouse.MouseInput;
import com.esotericsoftware.controller.ui.swing.UI;

/**
 * An action that sets the state of a button or axis when executed.
 */
public class DeviceAction implements Action {
	private Target target;
	private Direction direction = Direction.both;

	public DeviceAction () {
	}

	public DeviceAction (Target target) {
		setTarget(target);
	}

	public Target getTarget () {
		return target;
	}

	public void setTarget (Target target) {
		if (target != null && !(target instanceof Button) && !(target instanceof Axis))
			throw new IllegalArgumentException("target must be a button or axis.");
		this.target = target;
	}

	public Direction getDirection () {
		return direction;
	}

	/**
	 * Sets the axis direction required for this action to execute.
	 */
	public void setDirection (Direction direction) {
		this.direction = direction;
	}

	public boolean isValid () {
		return UI.instance.getDevice() != null;
	}

	public void reset (Config config, Trigger trigger) {
	}

	public Object execute (Config config, Trigger trigger) {
		Device device = UI.instance.getDevice();
		if (device == null) return null;
		Object object = trigger.getPayload();
		float payload = object instanceof Float ? (Float)object : 1;
		switch (direction) {
		case up:
			if (payload > 0) payload = -payload;
		case left:
			if (payload > 0) payload = -payload;
		}
		try {
			// If the target is an axis and the trigger was activated by a mouse axis...
			if (target instanceof Axis && trigger instanceof InputTrigger) {
				Input input = ((InputTrigger)trigger).getInput();
				if (input instanceof Mouse.MouseInput) {
					String axis = ((MouseInput)input).getAxis();
					if (axis != null) {
						float deltaX = 0, deltaY = 0;
						if (axis.equals("x"))
							deltaX = payload;
						else if (axis.equals("y")) {
							deltaY = payload;
						}
						device.addMouseDelta(((Axis)target).getStick(), deltaX, deltaY);
						return payload;
					}
				}
			}
			device.set(target, payload);
			return payload;
		} catch (IOException ex) {
			throw new RuntimeException("Error executing action: " + this);
		}
	}

	public String toString () {
		if (target == null) return "";
		StringBuilder buffer = new StringBuilder();
		buffer.append("Device: ");
		buffer.append(target);
		if (direction != Direction.both) {
			buffer.append(' ');
			buffer.append(direction);
		}
		return buffer.toString();
	}

	static public enum Direction {
		up, down, left, right, both
	}
}
