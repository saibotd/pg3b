
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
import com.esotericsoftware.controller.input.Keyboard.Listener;
import com.esotericsoftware.controller.ui.swing.UI;
import com.esotericsoftware.controller.util.Listeners;
import com.esotericsoftware.controller.util.Util;

public class TextMode {
	private TextMode () {
	}

	static private final HashMap<Character, Location> charToLocation = new HashMap();
	static private final LinkedList presses = new LinkedList();
	static private boolean enabled;
	static private int currentPage, currentX = 1, currentY = 1;
	static private Listeners<Listener> listeners = new Listeners(Listeners.class);

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
			'â', 'ä', 'à', 'å', 'á', ' ', 'ñ', 'œ', 'æ', 'ß', //
			'é', 'ê', 'ë', 'è', 'Þ', 'ç', 'ý', 'ÿ', 'º', 'ª', //
			'ï', 'î', 'ì', 'í', 'ü', 'û', 'ù', 'ú', 'µ', ' ', // 
			'ô', 'ö', 'ò', 'ó', 'o', 'o', 'o', ' ', '×', ' ', //
		};
		for (int i = 0, n = chars.length; i < n; i++)
			if (chars[i] != ' ') charToLocation.put(chars[i], new Location(i));

		Thread thread = new Thread("TextMode") {
			public void run () {
				Object object;
				while (true) {
					synchronized (presses) {
						if (presses.isEmpty()) {
							try {
								presses.wait();
							} catch (InterruptedException ex) {
							}
							continue;
						}
						object = presses.pop();
					}
					Device device = UI.instance.getDevice();
					if (device == null) continue;
					try {
						if (object instanceof Target) {
							device.set((Target)object, 1);
							Util.sleep(60);
							device.set((Target)object, 0);
						} else if (object instanceof ArrayList) {
							for (Object target : (ArrayList)object)
								device.set((Target)target, 1);
							Util.sleep(60);
							for (Object target : (ArrayList)object)
								device.set((Target)target, 0);
						}
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
		public void keyDown (int keyCode, char c) {
			synchronized (presses) {
				if (!enabled) return;
				switch (c) {
				case VK_RIGHT:
					presses.add(Button.rightShoulder);
					presses.notifyAll();
					return;
				case VK_LEFT:
					presses.add(Button.leftShoulder);
					presses.notifyAll();
					return;
				case VK_CAPS_LOCK:
					presses.add(Button.leftStick);
					presses.notifyAll();
					return;
				case VK_ESCAPE:
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
				currentX = currentY = 1;
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
				if (enabled) {
					try {
						presses.wait();
					} catch (InterruptedException ex) {
					}
					continue;
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
			return;
		case VK_BACK_SPACE:
			presses.add(Button.x);
			return;
		case VK_DELETE:
			presses.add(Button.rightShoulder);
			presses.add(Button.x);
			return;
		case '\n':
			presses.add(Button.start);
			setEnabled(false);
			return;
		}

		Location location = charToLocation.get(c);
		if (location == null) return;

		int targetX = location.x, targetY = location.y;
		int deltaX = currentX - targetX;
		int incrementX = deltaX > 0 ? -1 : 1;
		if (Math.abs(deltaX) > 6) incrementX = -incrementX;
		int incrementY = currentY - targetY > 0 ? -1 : 1;
		while (currentX != targetX || currentY != targetY) {
			ArrayList<Target> targets = new ArrayList(2);
			checkPage(location.page, targets);
			if (currentX != targetX) {
				currentX += incrementX;
				targets.add(incrementX > 0 ? Button.right : Button.left);
				if (currentX < 0) currentX += 12;
				if (currentX == 12) currentX -= 12;
			}
			if (currentY != targetY && currentX != 0 && currentX != 11) {
				targets.add(incrementY > 0 ? Button.down : Button.up);
				currentY += incrementY;
				if (currentY < 0) currentY += 5;
				if (currentY == 5) currentY -= 5;
			}
			presses.add(targets);
		}
		checkPage(location.page, null);
		if (Character.isUpperCase(c)) presses.add(Button.leftStick);
		presses.add(Button.a);
	}

	static private void checkPage (int page, ArrayList<Target> targets) {
		if (currentPage == page) return;
		boolean forward = currentPage < page;
		if (Math.abs(currentPage - page) > 1) forward = !forward;
		currentPage = page;
		Axis axis = forward ? Axis.rightTrigger : Axis.leftTrigger;
		if (targets != null)
			targets.add(axis);
		else
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
			this.page = index / 40;
			this.x = index / 10;
			this.y = index % 10;
		}
	}

	static public class Listener {
		public void enter () {
		}

		public void exit () {
		}
	}
}
