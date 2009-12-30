
package com.esotericsoftware.controller.ui;

import com.esotericsoftware.controller.device.Device;

public interface MouseTranslation {
	public float[] getDeflection (float deltaX, float deltaY);

	public void update (Device device);
}
