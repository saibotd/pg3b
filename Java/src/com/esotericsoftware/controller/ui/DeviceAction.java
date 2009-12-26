
package com.esotericsoftware.controller.ui;

import java.io.IOException;

import com.esotericsoftware.controller.device.Axis;
import com.esotericsoftware.controller.device.Button;
import com.esotericsoftware.controller.device.Device;
import com.esotericsoftware.controller.device.Target;
import com.esotericsoftware.controller.ui.swing.UI;

// BOZO - Multiple triggers modifying the same target can result in the target being release while a trigger is still down.

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

	public boolean execute (Config config, Trigger trigger, Object payload) {
		Device device = UI.instance.getDevice();
		if (device == null) return false;
		float state = payload instanceof Float ? (Float)payload : 1;
		switch (direction) {
		case up:
			if (state > 0) state = -state;
		case left:
			if (state > 0) state = -state;
		}
		try {
			device.set(target, state);
			return true;
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
