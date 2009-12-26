
package com.esotericsoftware.controller.ui;

import com.esotericsoftware.controller.input.Input;
import com.esotericsoftware.controller.input.Keyboard;

/**
 * A trigger that executes its action based on the state of a JInput controller (keyboard, mouse, joystick, etc, essentially any
 * peripheral).
 */
public class InputTrigger extends Trigger {
	private Input input;
	private boolean shift, ctrl, alt, noModifiers;

	public InputTrigger () {
	}

	public InputTrigger (Input input, Action action) {
		setInput(input);
		setAction(action);
	}

	public Input getInput () {
		return input;
	}

	public void setInput (Input input) {
		this.input = input;
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

	public boolean getNoModifiers () {
		return noModifiers;
	}

	public void setNoModifiers (boolean noModifiers) {
		this.noModifiers = noModifiers;
	}

	public boolean isValid () {
		if (input == null) return false;
		return input.isValid();
	}

	public Poller getPoller () {
		if (input == null) return null;
		return input.getInputDevice();
	}

	/**
	 * Returns true if the current keyboard state meets the requirements for this trigger's keyboard modifiers.
	 */
	public boolean checkModifiers () {
		Keyboard keyboard = Keyboard.instance;
		if (noModifiers) {
			if (keyboard.isCtrlDown()) return false;
			if (keyboard.isAltDown()) return false;
			if (keyboard.isShiftDown()) return false;
		} else {
			if (ctrl && !keyboard.isCtrlDown()) return false;
			if (alt && !keyboard.isAltDown()) return false;
			if (shift && !keyboard.isShiftDown()) return false;
		}
		return true;
	}

	public boolean isActive () {
		if (input == null) return false;
		return input.getState() != 0 && checkModifiers();
	}

	public Float getState () {
		if (input == null) return null;
		return input.getState();
	}

	public String toString () {
		StringBuilder buffer = new StringBuilder();
		if (getCtrl()) buffer.append("ctrl+");
		if (getAlt()) buffer.append("alt+");
		if (getShift()) buffer.append("shift+");
		if (input == null)
			buffer.append("<none>");
		else {
			buffer.append(input);
			buffer.append(": ");
			buffer.append(input.getInputDevice());
		}
		return buffer.toString();
	}
}
