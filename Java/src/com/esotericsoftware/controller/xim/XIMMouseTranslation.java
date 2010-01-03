
package com.esotericsoftware.controller.xim;

import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.MouseInfo;
import java.awt.Point;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import javax.swing.JFrame;
import javax.swing.JLabel;

import com.esotericsoftware.controller.device.Device;
import com.esotericsoftware.controller.device.Stick;
import com.esotericsoftware.controller.ui.MouseTranslation;

public class XIMMouseTranslation implements MouseTranslation {
	private static final int UPDATE_FREQUENCY = 120;

	static {
		XIM.load();
	}

	private float smoothness, yxRatio, translationExponent, diagonalDampen;
	private int sensitivity;

	private transient float actualSmoothness, actualYXRatio, actualTranslationExponent;
	private transient int actualSensitivity;

	private transient float[] stickValues = new float[2];
	private transient ByteBuffer byteBuffer;
	private transient ShortBuffer shortBuffer;
	private transient float timer;
	private transient long lastTime;

	public XIMMouseTranslation () {
		byteBuffer = ByteBuffer.allocateDirect(4);
		byteBuffer.order(ByteOrder.nativeOrder());
		shortBuffer = byteBuffer.asShortBuffer();
	}

	public float getSmoothness () {
		return smoothness;
	}

	public void setSmoothness (float smoothness) {
		this.smoothness = smoothness;
	}

	public float getYXRatio () {
		return yxRatio;
	}

	public void setYXRatio (float yxRatio) {
		this.yxRatio = yxRatio;
	}

	public float getTranslationExponent () {
		return translationExponent;
	}

	public void setTranslationExponent (float translationExponent) {
		this.translationExponent = translationExponent;
	}

	public int getSensitivity () {
		return sensitivity;
	}

	public void setSensitivity (int sensitivity) {
		this.sensitivity = sensitivity;
	}

	public float getDiagonalDampen () {
		return diagonalDampen;
	}

	public void setDiagonalDampen (float diagonalDampen) {
		this.diagonalDampen = diagonalDampen;
	}

	public float[] getDeflection (float deltaX, float deltaY) {
		if (this.smoothness != actualSmoothness || //
			this.yxRatio != actualYXRatio || //
			this.translationExponent != actualTranslationExponent || //
			this.sensitivity != actualSensitivity) {
			actualSmoothness = smoothness;
			actualYXRatio = yxRatio;
			actualTranslationExponent = translationExponent;
			actualSensitivity = sensitivity;
			setSmoothness(smoothness, UPDATE_FREQUENCY, yxRatio, translationExponent, sensitivity);
		}
		float deadzone = 0;
		int deadzoneShape = 1;
		computeStickValues(deltaX, deltaY, yxRatio, translationExponent, sensitivity, diagonalDampen, deadzoneShape, 0, byteBuffer);
		stickValues[0] = shortBuffer.get(0) / 32768f;
		stickValues[1] = shortBuffer.get(1) / 32768f;
		return stickValues;
	}

	public void update (Device device) {
		long time = System.currentTimeMillis();
		long delta = time - lastTime;
		lastTime = time;

		timer += delta;
		if (timer < 1000 / UPDATE_FREQUENCY) return;
		timer = 0;

		if (device == null) {
			getDeflection(0, 0);
			return;
		}
		Stick stick = device.getMouseDeltaStick();
		if (stick == null) {
			getDeflection(0, 0);
			return;
		}
		float[] mouseDelta = device.getMouseDelta();
		float[] deflection = getDeflection(mouseDelta[0], mouseDelta[1]);
		try {
			device.set(stick, deflection[0], deflection[1]);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	static native void setSmoothness (float intensity, int updateFrequency, float yxRatio, float translationExponent,
		float sensitivity);

	static native void computeStickValues (float deltaX, float deltaY, float yxRatio, float translationExponent,
		float sensitivity, float diagonalDampen, int deadZoneType, float deadzone, ByteBuffer buffer);

	public static void main (String[] args) {
		final XIMMouseTranslation translation = new XIMMouseTranslation();
		translation.setYXRatio(1);
		translation.setTranslationExponent(0.75f);
		translation.setDiagonalDampen(0.25f);
		translation.setSmoothness(0.3f);
		translation.setSensitivity(1250);
		JFrame frame = new JFrame("XIMMouseTranslation");
		final JLabel xLabel = new JLabel("X: 0");
		final JLabel yLabel = new JLabel("Y: 0");
		frame.getContentPane().setLayout(new GridLayout(2, 1));
		frame.getContentPane().add(xLabel);
		frame.getContentPane().add(yLabel);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		Thread thread = new Thread() {
			int x, y, lastX, lastY;

			public void run () {
				while (true) {
					EventQueue.invokeLater(new Runnable() {
						public void run () {
							Point location = MouseInfo.getPointerInfo().getLocation();
							int deltaX = location.x - lastX;
							int deltaY = location.y - lastY;
							lastX = location.x;
							lastY = location.y;
							float[] values = translation.getDeflection(deltaX, deltaY);
							xLabel.setText("X: " + values[0]);
							yLabel.setText("Y: " + values[1]);
						}
					});
					try {
						Thread.sleep(1000 / UPDATE_FREQUENCY);
					} catch (InterruptedException ex) {
					}
				}
			}
		};
		thread.setDaemon(true);
		thread.start();
	}
}
