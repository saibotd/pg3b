
package com.esotericsoftware.controller.ui;

import java.io.IOException;

import javax.swing.JPanel;

import com.esotericsoftware.controller.device.Device;
import com.esotericsoftware.controller.device.Stick;

public class DefaultMouseTranslation implements MouseTranslation {
	private transient float timer;
	private transient long lastTime;

	public void update (Device device) {
		long time = System.currentTimeMillis();
		long delta = time - lastTime;
		lastTime = time;

		timer += delta;
		if (timer < 1000 / 60) return;
		timer = 0;

		if (device == null) return;

		Stick stick = device.getMouseDeltaStick();
		if (stick == null) return;

		float[] mouseDelta = device.getMouseDelta();
		float x = 0;
		if (mouseDelta[0] > 0)
			x = 1;
		else if (mouseDelta[0] < 0) x = -1;

		float y = 0;
		if (mouseDelta[1] > 0)
			y = 1;
		else if (mouseDelta[1] < 0) y = -1;

		device.set(stick, x, y);
		if (x == 0 && y == 0) device.clearMouseDeltaStick();
	}

	public JPanel getPanel () {
		return new JPanel();
	}

	public void updateFromPanel (JPanel panel) {
	}

	public boolean equals (Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		return true;
	}

	public String toString () {
		return "Default";
	}
}
