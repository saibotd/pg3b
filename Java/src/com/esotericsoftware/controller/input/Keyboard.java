
package com.esotericsoftware.controller.input;

import static java.awt.event.KeyEvent.*;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;

import com.esotericsoftware.controller.util.Listeners;

/**
 * The input device for the system wide keyboard.
 */
public class Keyboard implements InputDevice {
	static public final Keyboard instance = new Keyboard();

	static private final HashMap<Integer, String> codeToName = new HashMap();
	static private final HashMap<String, Integer> nameToCode = new HashMap();
	static {
		int fieldModifiers = Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL;
		Field[] fields = KeyEvent.class.getDeclaredFields();
		for (int i = 0; i < fields.length; i++) {
			Field field = fields[i];
			try {
				if (field.getModifiers() == fieldModifiers && field.getType() == Integer.TYPE && field.getName().startsWith("VK_")) {
					int keyCode = field.getInt(null);
					if (keyCode == 0) continue;
					String name = KeyEvent.getKeyText(keyCode);
					codeToName.put(keyCode, name);
					nameToCode.put(name.toLowerCase(), keyCode);
				}
			} catch (IllegalAccessException ignored) {
			}
		}
	}

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
					if (keyCode == 0 || keyCode > keys.length) break;
					lastKeyCode = keyCode;
					keys[keyCode] = true;
					for (int i = 0, n = listeners.length; i < n; i++)
						listeners[i].keyDown(keyCode, event.getKeyChar());
					break;
				}
				case KeyEvent.KEY_RELEASED: {
					int keyCode = event.getKeyCode();
					if (keyCode > keys.length) break;
					keys[keyCode] = false;
					for (int i = 0, n = listeners.length; i < n; i++)
						listeners[i].keyUp(keyCode, event.getKeyChar());
					break;
				}
				case KeyEvent.KEY_TYPED:
					for (int i = 0, n = listeners.length; i < n; i++)
						listeners[i].keyTyped(event.getKeyChar());
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
	}

	public void removeListener (Listener listener) {
		listeners.removeListener(listener);
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

	static public String getName (int keyCode) {
		String name = codeToName.get(keyCode);
		if (name == null) return "<unknown>";
		return name;
	}

	static public int getKeyCode (String name) {
		Integer keyCode = nameToCode.get(name.toLowerCase());
		if (keyCode == null) return 0;
		return keyCode;
	}

	static public class KeyboardInput implements Input {
		private int keyCode;

		public float getState () {
			return instance.isPressed(keyCode) ? 1 : 0;
		}

		public float getOtherState () {
			return 0;
		}

		public Keyboard getInputDevice () {
			return instance;
		}

		public boolean isValid () {
			return true;
		}

		public boolean isAxis () {
			return false;
		}

		public boolean isAxisX () {
			return false;
		}

		public void setKeyCode (int keyCode) {
			this.keyCode = keyCode;
		}

		public int getKeyCode () {
			return keyCode;
		}

		public String toString () {
			return getName(keyCode);
		}

		public int hashCode () {
			final int prime = 31;
			int result = 1;
			result = prime * result + keyCode;
			return result;
		}

		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			KeyboardInput other = (KeyboardInput)obj;
			if (keyCode != other.keyCode) return false;
			return true;
		}
	}

	static public class Listener {
		public void keyDown (int keyCode, char c) {
		}

		public void keyUp (int keyCode, char c) {
		}

		public void keyTyped (char c) {
		}
	}
}
