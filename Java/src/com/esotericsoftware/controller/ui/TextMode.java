
package com.esotericsoftware.controller.ui;

import static com.esotericsoftware.minlog.Log.*;
import static java.awt.event.KeyEvent.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import com.esotericsoftware.controller.device.Axis;
import com.esotericsoftware.controller.device.Button;
import com.esotericsoftware.controller.device.Device;
import com.esotericsoftware.controller.device.Target;
import com.esotericsoftware.controller.input.Keyboard;
import com.esotericsoftware.controller.ui.swing.UI;
import com.esotericsoftware.controller.util.Listeners;
import com.esotericsoftware.controller.util.Util;

public class TextMode {

	private TextMode () {
	}

	static private final HashMap<Character, Location> charToLocation = new HashMap();
	static private final LinkedList<Target> presses = new LinkedList();
	static private boolean enabled, caps;
	static private int currentPage, currentX, currentY, charsEntered, cursorPosition;
	static private Listeners<Listener> listeners = new Listeners(Listeners.class);
	static boolean ignoreEnter;

	static {
		char[] chars = {
		//
			'a', 'b', 'c', 'd', 'e', 'f', 'g', '1', '2', '3', //
			'h', 'i', 'j', 'k', 'l', 'm', 'n', '4', '5', '6', //
			'o', 'p', 'q', 'r', 's', 't', 'u', '7', '8', '9', // 
			'v', 'w', 'x', 'y', 'z', '-', '@', '_', '0', '.', //
			//
			',', ';', ':', '\'', '"', '!', '?', '¡', '¿', '%', //
			'[', ']', '{', '}', '`', '$', '£', '«', '»', '#', //
			'<', '>', '(', ')', '€', '¥', ' ', '~', '^', '\\', // 
			'|', '=', '*', '/', '+', ' ', ' ', ' ', '&', ' ', //
			//
			'à', 'á', 'â', 'ã', 'ä', 'å', 'ñ', 'œ', 'æ', 'ß', //
			'è', 'é', 'ê', 'ë', 'Þ', 'ç', 'ý', 'ÿ', 'º', 'ª', //
			'ì', 'í', 'î', 'ï', 'ù', 'ú', 'ü', 'û', 'µ', '\u015D', // 
			'ò', 'ó', 'ô', 'õ', 'ð', 'ö', 'ø', ' ', '×', ' ', //
		};
		for (int i = 0, n = chars.length; i < n; i++)
			if (chars[i] != ' ') charToLocation.put(chars[i], new Location(i));

		Thread thread = new Thread("TextMode") {
			public void run () {
				Target target;
				while (true) {
					synchronized (presses) {
						if (presses.isEmpty()) {
							try {
								presses.wait();
							} catch (InterruptedException ex) {
							}
							continue;
						}
						target = presses.pop();
					}
					Device device = UI.instance.getDevice();
					if (device == null) continue;
					boolean isButton = target instanceof Button;
					try {
						device.apply(target, isButton ? 1 : 0.5f);
						// Util.sleep(32);
						Util.sleep(96);
						device.apply(target, 0);
						// Util.sleep(target == Button.a || target instanceof Axis ? 150 : 64);
						Util.sleep(target == Button.a || target instanceof Axis ? 224 : 192);
					} catch (IOException ex) {
						if (ERROR) error("Error moving cursor to input text.", ex);
						presses.clear();
					}
				}
			}
		};
		thread.setDaemon(true);
		thread.start();
	}

	static private Keyboard.Listener keyboardListener = new Keyboard.Listener() {
		public void keyUp (int keyCode, char c) {
			if (keyCode == VK_ENTER) ignoreEnter = false;
		}

		public void keyDown (int keyCode, char c) {
			synchronized (presses) {
				if (!enabled) return;
				switch (keyCode) {
				case VK_RIGHT:
					if (DEBUG) debug("Queued text input: <right>");
					cursorPosition++;
					if (cursorPosition > charsEntered) cursorPosition = 0;
					presses.add(Button.rightShoulder);
					presses.notifyAll();
					return;
				case VK_LEFT:
					if (DEBUG) debug("Queued text input: <left>");
					cursorPosition--;
					if (cursorPosition < 0) cursorPosition = charsEntered;
					presses.add(Button.leftShoulder);
					presses.notifyAll();
					return;
				case VK_HOME: {
					if (DEBUG) debug("Queued text input: <home>");
					int increment = cursorPosition > charsEntered / 2 ? 1 : -1;
					Button button = increment < 0 ? Button.leftShoulder : Button.rightShoulder;
					while (cursorPosition != 0) {
						cursorPosition += increment;
						if (cursorPosition > charsEntered) cursorPosition = 0;
						presses.add(button);
					}
					presses.notifyAll();
					return;
				}
				case VK_END: {
					if (DEBUG) debug("Queued text input: <end>");
					int increment = cursorPosition > charsEntered / 2 ? 1 : -1;
					Button button = increment < 0 ? Button.leftShoulder : Button.rightShoulder;
					while (cursorPosition != charsEntered) {
						cursorPosition += increment;
						if (cursorPosition < 0) cursorPosition = charsEntered;
						presses.add(button);
					}
					presses.notifyAll();
					return;
				}
				case VK_F4:
					if (!Keyboard.instance.isCtrlDown()) return;
					// Fall through.
				case VK_ESCAPE:
					if (DEBUG && !presses.isEmpty()) debug("Text input queue cleared.");
					presses.clear();
					setEnabled(false);
					return;
				}
			}
		}

		public void keyTyped (char c) {
			synchronized (presses) {
				if (!enabled) return;
				press(c);
				presses.notifyAll();
			}
		}
	};

	static public void setEnabled (boolean enabled) {
		synchronized (presses) {
			if (TextMode.enabled == enabled) return;
			TextMode.enabled = enabled;
			if (enabled) {
				presses.clear();
				currentPage = 0;
				currentX = 1;
				currentY = 0;
				caps = false;
				charsEntered = 0;
				cursorPosition = 0;
				ignoreEnter = Keyboard.instance.isPressed(VK_ENTER);
				Keyboard.instance.addListener(keyboardListener);
				if (INFO) info("Entered text mode.");
			} else {
				Keyboard.instance.removeListener(keyboardListener);
				if (INFO) info("Exited text mode.");
			}
			presses.notifyAll();
		}
	}

	/**
	 * Enables text mode and blocks until it is disabled.
	 */
	static public void block () {
		setEnabled(true);
		while (true) {
			synchronized (presses) {
				if (!enabled) break;
				try {
					presses.wait();
				} catch (InterruptedException ex) {
				}
			}
		}
	}

	static public void send (String text) {
		synchronized (presses) {
			for (char c : text.toCharArray())
				press(c);
			presses.notifyAll();
		}
	}

	static private void press (char c) {
		switch (c) {
		case ' ':
			presses.add(Button.y);
			if (DEBUG) debug("Queued text input: <space>");
			return;
		case VK_BACK_SPACE:
			presses.add(Button.x);
			if (DEBUG) debug("Queued text input: <backspace>");
			return;
		case VK_DELETE:
			presses.add(Button.rightShoulder);
			presses.add(Button.x);
			if (DEBUG) debug("Queued text input: <delete>");
			return;
		case '\n':
			if (ignoreEnter) return;
			presses.add(Button.start);
			if (DEBUG) debug("Queued text input: <start>");
			setEnabled(false);
			return;
		}

		Location location = charToLocation.get(Character.toLowerCase(c));
		if (location == null) return;

		if (DEBUG) debug("Queued text input: " + c);

		int targetX = location.x, targetY = location.y;
		int deltaX = currentX - targetX;
		int incrementX = deltaX > 0 ? -1 : 1;
		if (Math.abs(deltaX) > 6) incrementX = -incrementX;
		int incrementY = currentY - targetY > 0 ? -1 : 1;
		while (currentX != targetX || currentY != targetY) {
			checkPage(location.page);
			if (currentX != targetX) {
				currentX += incrementX;
				presses.add(incrementX > 0 ? Button.right : Button.left);
				if (currentX < 0) currentX += 12;
				if (currentX == 12) currentX -= 12;
			}
			if (currentY != targetY && currentX != 0 && currentX != 11) {
				presses.add(incrementY > 0 ? Button.down : Button.up);
				currentY += incrementY;
				if (currentY < 0) currentY += 5;
				if (currentY == 5) currentY -= 5;
			}
		}
		checkPage(location.page);
		if (caps != Character.isUpperCase(c) && Character.toUpperCase(c) != Character.toLowerCase(c)) {
			caps = !caps;
			presses.add(Button.leftStick);
		}
		presses.add(Button.a);
		charsEntered++;
		cursorPosition++;
	}

	static private void checkPage (int page) {
		if (currentPage == page) return;
		boolean forward = currentPage < page;
		if (Math.abs(currentPage - page) > 1) forward = !forward;
		currentPage = page;
		Axis axis = forward ? Axis.leftTrigger : Axis.rightTrigger;
		presses.add(axis);
	}

	static public void addListener (Listener listener) {
		listeners.addListener(listener);
	}

	static public void removeListener (Listener listener) {
		listeners.removeListener(listener);
	}

	static private class Location {
		final public int page, x, y;

		public Location (int index) {
			page = index / 40;
			y = index / 10 % 4;
			x = index % 10 + 1;
		}
	}

	static public class Listener {
		public void enter () {
		}

		public void exit () {
		}
	}
}
