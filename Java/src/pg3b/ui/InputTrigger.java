
package pg3b.ui;

import pg3b.input.Input;
import pg3b.input.Keyboard;
import pg3b.input.Mouse;

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

	public Object check () {
		if (input == null) return null;
		Float state = input.getState(this);
		if (state == null) return null;
		float value = state;
		if (getInput() instanceof Mouse.MouseInput) {
			System.out.println(state);
			value = Math.min(1, state / 10);
		}
		return value;
	}

	public String toString () {
		StringBuilder buffer = new StringBuilder();
		if (getCtrl()) buffer.append("ctrl+");
		if (getAlt()) buffer.append("alt+");
		if (getShift()) buffer.append("shift+");
		buffer.append(input);
		buffer.append(": ");
		buffer.append(input.getInputDevice());
		return buffer.toString();
	}
}
