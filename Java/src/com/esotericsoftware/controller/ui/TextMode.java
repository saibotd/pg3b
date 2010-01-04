
package com.esotericsoftware.controller.ui;

import java.awt.Point;
import java.io.IOException;
import java.util.HashMap;

import com.esotericsoftware.controller.device.Button;
import com.esotericsoftware.controller.device.Device;
import com.esotericsoftware.controller.util.Util;

public class TextMode {
	static private final HashMap<String, Point> locations = new HashMap();
	static {
		String[] pages = { //
		"a", "b", "c", "d", "e", "f", "g", "1", "2", "3", //
			"h", "i", "j", "k", "l", "m", "n", "4", "5", "6", //
			"o", "p", "q", "r", "s", "t", "u", "7", "8", "9", // 
			"v", "w", "x", "y", "z", "-", "@", "_", "0", ".", //
			// ---
			"a1", "b1", "c1", "d1", "e1", "f1", "g1", "1", "2", "3", //
			"h", "i", "j", "k", "l", "m", "n", "4", "5", "6", //
			"o", "p", "q", "r", "s", "t", "u", "7", "8", "9", // 
			"v", "w", "x", "y", "z", "-", "@", "_", "0", ".", //
		};
		for (int page = 0; page < pages.length; page++)
			for (int x = 1; x < 12; x++)
				for (int y = 0; y < 4; y++)
					locations.put(pages[page * 40 + y * 10 + x], new Point(x, y));
	}

	private final Device device;
	private int currentX = 1, currentY = 1;

	public TextMode (Device device) {
		this.device = device;
	}

	public synchronized void press (String value) throws IOException {
		Point point = locations.get(value);
		int deltaX = currentX - point.x;
		int incrementX = deltaX > 0 ? -1 : 1;
		if (Math.abs(deltaX) > 6) incrementX = -incrementX;
		int incrementY = currentY - point.y > 0 ? -1 : 1;
		while (currentX != point.x || currentY != point.y) {
			if (currentX != point.x) {
				device.set(incrementX > 0 ? Button.right : Button.left, true);
				currentX += incrementX;
				Util.sleep(60);
				device.set(incrementX > 0 ? Button.right : Button.left, false);
				if (currentX < 0) currentX += 12;
				if (currentX == 12) currentX -= 12;
			}
			if (currentY != point.y && currentX != 0 && currentX != 11) {
				device.set(incrementY > 0 ? Button.down : Button.up, true);
				currentY += incrementY;
				Util.sleep(60);
				device.set(incrementY > 0 ? Button.down : Button.up, false);
				if (currentY < 0) currentY += 5;
				if (currentY == 5) currentY -= 5;
			}
		}
	}
}
