
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
import com.esotericsoftware.controller.util.Util;

public class TextMode {
	static private final HashMap<Integer, Location> keyCodeToLocation = new HashMap();
	static private final HashMap<Character, Integer> charToKeyCode = new HashMap();
	static {
		int[] keyCodes = { //
		VK_A, VK_B, VK_C, VK_D, VK_E, VK_F, VK_G, VK_1, VK_2, VK_3, //
			VK_H, VK_I, VK_J, VK_K, VK_L, VK_M, VK_N, VK_4, VK_5, VK_6, //
			VK_O, VK_P, VK_Q, VK_R, VK_S, VK_T, VK_U, VK_7, VK_8, VK_9, // 
			VK_V, VK_W, VK_X, VK_Y, VK_Z, VK_MINUS, VK_AT, VK_UNDERSCORE, VK_0, VK_PERIOD, //
		// ---
		};
		char[] chars = { //
		'a', 'b', 'c', 'd', 'e', 'f', 'g', '1', '2', '3', //
			'h', 'i', 'j', 'k', 'l', 'm', 'n', '4', '5', '6', //
			'o', 'p', 'q', 'r', 's', 't', 'u', '7', '8', '9', // 
			'v', 'w', 'x', 'y', 'z', '-', '@', '_', '0', '.', //
		// ---
		};
		for (int i = 0, n = keyCodes.length; i < n; i++) {
			keyCodeToLocation.put(keyCodes[i], new Location(i));
			charToKeyCode.put(chars[i], keyCodes[i]);
		}
	}

	static private LinkedList presses = new LinkedList();
	static {
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

	static private boolean enabled, firstPage;
	static private int currentX = 1, currentY = 1;

	static private Keyboard.Listener keyboardListener = new Keyboard.Listener() {
		public void keyDown (int keyCode) {
			synchronized (presses) {
				if (enabled) moveTo(keyCode, Keyboard.instance.isShiftDown());
			}
		}
	};

	static public void setEnabled (boolean enabled) {
		synchronized (presses) {
			if (TextMode.enabled == enabled) return;
			TextMode.enabled = enabled;
			presses.clear();
			if (enabled) {
				firstPage = true;
				currentX = currentY = 1;
				Keyboard.instance.addListener(keyboardListener);
			} else
				Keyboard.instance.removeListener(keyboardListener);
		}
	}

	static public void send (String text) {
		synchronized (presses) {
			for (char c : text.toCharArray()) {
				Integer keyCode = c == ' ' ? VK_SPACE : charToKeyCode.get(c);
				if (keyCode == null) return;
				moveTo(keyCode, Character.isUpperCase(c));
			}
			moveTo(VK_ENTER, false);
		}
	}

	static private void moveTo (int keyCode, boolean shift) {
		switch (keyCode) {
		case VK_SPACE:
			addPress(Button.y);
			return;
		case VK_BACK_SPACE:
			addPress(Button.x);
			return;
		case VK_DELETE:
			addPress(Button.rightShoulder);
			addPress(Button.x);
			return;
		case VK_RIGHT:
			addPress(Button.rightShoulder);
			return;
		case VK_LEFT:
			addPress(Button.leftShoulder);
			return;
		case VK_CAPS_LOCK:
			addPress(Button.leftStick);
			return;
		case VK_ENTER:
			addPress(Button.start);
			return;
		case VK_ESCAPE:
			setEnabled(false);
			return;
		}

		Location location = keyCodeToLocation.get(keyCode);
		if (location == null) return;

		if (firstPage != location.firstPage) {
			addPress(location.firstPage ? Axis.rightTrigger : Axis.leftTrigger);
			firstPage = !firstPage;
		}

		int targetX = location.x, targetY = location.y;
		int deltaX = currentX - targetX;
		int incrementX = deltaX > 0 ? -1 : 1;
		if (Math.abs(deltaX) > 6) incrementX = -incrementX;
		int incrementY = currentY - targetY > 0 ? -1 : 1;
		while (currentX != targetX || currentY != targetY) {
			ArrayList<Target> targets = new ArrayList(2);
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
			addPress(targets);
		}
		if (shift) addPress(Button.leftStick);
		addPress(Button.a);
	}

	static private void addPress (Object object) {
		synchronized (presses) {
			presses.addLast(object);
			presses.notifyAll();
		}
	}

	static private class Location {
		final public boolean firstPage;
		final public int x, y;

		public Location (int index) {
			this.firstPage = index < 40;
			this.x = index / 10;
			this.y = index % 10;
		}
	}
}
