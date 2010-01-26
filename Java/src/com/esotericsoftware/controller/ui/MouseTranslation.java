
package com.esotericsoftware.controller.ui;

import javax.swing.JPanel;

import com.esotericsoftware.controller.device.Device;

public interface MouseTranslation {
	public float[] getDeflection (float deltaX, float deltaY);

	public void update (Device device);

	public JPanel getPanel ();

	public void updateFromPanel (JPanel panel);
}
