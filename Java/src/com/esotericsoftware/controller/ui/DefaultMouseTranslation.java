
package com.esotericsoftware.controller.ui;

import com.esotericsoftware.controller.device.Device;

public class DefaultMouseTranslation implements MouseTranslation {
	public float[] getDeflection (float deltaX, float deltaY) {
		float x = 0;
		if (deltaX > 0) x = 1;
		if (deltaX < 0) x = -1;
		float y = 0;
		if (deltaY > 0) y = 1;
		if (deltaY < 0) y = -1;
		return new float[] {x, y};
	}

	public void update (Device device, float delta) {
	}
}
