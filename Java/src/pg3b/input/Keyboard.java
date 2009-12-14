
package pg3b.input;

import static com.esotericsoftware.minlog.Log.*;
import static java.awt.event.KeyEvent.*;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;

import pg3b.ui.InputTrigger;
import pg3b.util.Listeners;

/**
 * The input device for the system wide keyboard.
 */
public class Keyboard implements InputDevice {
	static public final Keyboard instance = new Keyboard();

	private boolean[] keys = new boolean[256];
	private Listeners<Listener> listeners = new Listeners(Listener.class);
	private int lastKeyCode = -1;

	private Keyboard () {
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
			public boolean dispatchKeyEvent (KeyEvent event) {
				Listener[] listeners = Keyboard.this.listeners.toArray();
				switch (event.getID()) {
				case KeyEvent.KEY_PRESSED: {
					int keyCode = event.getKeyCode();
					if (keyCode > keys.length) break;
					lastKeyCode = keyCode;
					keys[keyCode] = true;
					for (int i = 0, n = listeners.length; i < n; i++)
						listeners[i].keyDown(Keyboard.this, keyCode);
					break;
				}
				case KeyEvent.KEY_RELEASED: {
					int keyCode = event.getKeyCode();
					if (keyCode > keys.length) break;
					keys[keyCode] = false;
					for (int i = 0, n = listeners.length; i < n; i++)
						listeners[i].keyUp(Keyboard.this, keyCode);
					break;
				}
				case KeyEvent.KEY_TYPED:
					for (int i = 0, n = listeners.length; i < n; i++)
						listeners[i].keyTyped(Keyboard.this, event.getKeyChar());
					break;
				}
				return false;
			}
		});
	}

	public boolean isPressed (int keyCode) {
		if (keyCode >= keys.length) throw new IllegalArgumentException("key must be < " + keys.length + ".");
		return keys[keyCode];
	}

	public boolean isCtrlDown () {
		return isPressed(VK_CONTROL);
	}

	public boolean isAltDown () {
		return isPressed(VK_ALT);
	}

	public boolean isShiftDown () {
		return isPressed(VK_SHIFT);
	}

	public void reset () {
		keys = new boolean[256];
	}

	public void addListener (Listener listener) {
		listeners.addListener(listener);
		if (TRACE) trace("pg3b", "Keyboard listener added: " + listener.getClass().getName());
	}

	public void removeListener (Listener listener) {
		listeners.removeListener(listener);
		if (TRACE) trace("pg3b", "Keyboard listener removed: " + listener.getClass().getName());
	}

	public boolean poll () {
		return true;
	}

	public boolean resetLastInput () {
		lastKeyCode = -1;
		return true;
	}

	public KeyboardInput getLastInput () {
		if (lastKeyCode == -1) return null;
		KeyboardInput input = new KeyboardInput();
		input.keyCode = lastKeyCode;
		return input;
	}

	public String toString () {
		return "Keyboard";
	}

	static public class KeyboardInput implements Input {
		private int keyCode;
		private transient float lastState = Float.NaN;

		public Float getState (InputTrigger trigger) {
			float state = instance.isPressed(keyCode) ? 1 : 0;
			if (state != 0 && !trigger.checkModifiers()) return null;
			if (state == lastState) return null;
			return state;
		}

		public Keyboard getInputDevice () {
			return instance;
		}

		public boolean isValid () {
			return true;
		}

		public String toString () {
			return KeyEvent.getKeyText(keyCode);
		}
	}

	static public class Listener {
		public void keyDown (Keyboard keyboard, int keyCode) {
		}

		public void keyUp (Keyboard keyboard, int keyCode) {
		}

		public void keyTyped (Keyboard keyboard, char c) {
		}
	}
}
