
package com.esotericsoftware.controller.xim;

import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import com.esotericsoftware.controller.device.Device;
import com.esotericsoftware.controller.device.Stick;
import com.esotericsoftware.controller.ui.MouseTranslation;
import com.esotericsoftware.controller.util.Util;

public class XIM2MouseTranslation implements MouseTranslation {
	private static final int UPDATE_FREQUENCY = 60;

	static {
		XIM2.isValid(false);
	}

	private float smoothness = 0.3f, yxRatio = 1.5f, translationExponent = 0.75f, diagonalDampen = 0.25f;
	private int sensitivity = 1250;

	private transient float actualSmoothness, actualYXRatio, actualTranslationExponent;
	private transient int actualSensitivity;

	private transient float[] stickValues = new float[2];
	private transient ByteBuffer byteBuffer;
	private transient ShortBuffer shortBuffer;
	private transient float timer;
	private transient long lastTime;

	public XIM2MouseTranslation () {
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
		computeStickValues(deltaX, deltaY, yxRatio, translationExponent, sensitivity, diagonalDampen, deadzoneShape, deadzone,
			byteBuffer);
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
		device.set(stick, deflection[0], deflection[1]);
		if (deflection[0] == 0 && deflection[1] == 0) device.clearMouseDeltaStick();
	}

	public JPanel getPanel () {
		return new XIM2Panel();
	}

	public void updateFromPanel (JPanel panel) {
		((XIM2Panel)panel).update();
	}

	public int hashCode () {
		final int prime = 31;
		int result = 1;
		result = prime * result + Float.floatToIntBits(diagonalDampen);
		result = prime * result + sensitivity;
		result = prime * result + Float.floatToIntBits(smoothness);
		result = prime * result + Float.floatToIntBits(translationExponent);
		result = prime * result + Float.floatToIntBits(yxRatio);
		return result;
	}

	public boolean equals (Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		XIM2MouseTranslation other = (XIM2MouseTranslation)obj;
		if (Float.floatToIntBits(diagonalDampen) != Float.floatToIntBits(other.diagonalDampen)) return false;
		if (sensitivity != other.sensitivity) return false;
		if (Float.floatToIntBits(smoothness) != Float.floatToIntBits(other.smoothness)) return false;
		if (Float.floatToIntBits(translationExponent) != Float.floatToIntBits(other.translationExponent)) return false;
		if (Float.floatToIntBits(yxRatio) != Float.floatToIntBits(other.yxRatio)) return false;
		return true;
	}

	public String toString () {
		return "XIM2";
	}

	class XIM2Panel extends JPanel {
		private JSpinner yxRatioSpinner;
		private JSpinner translationExponentSpinner;
		private JSpinner sensitivitySpinner;
		private JSpinner diagonalDampenSpinner;
		private JSpinner smoothnessSpinner;

		public XIM2Panel () {
			super(new GridBagLayout());
			{
				JLabel label = new JLabel("YX ratio:");
				add(label, new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(
					0, 0, 0, 6), 0, 0));
			}
			{
				yxRatioSpinner = new JSpinner();
				add(yxRatioSpinner, new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL, new Insets(0, 0, 6, 0), 0, 0));
				yxRatioSpinner.setModel(Util.newFloatSpinnerModel(1f, -3, 3, 0.05f));
			}
			{
				JLabel label = new JLabel("Translation exponent:");
				add(label, new GridBagConstraints(0, 9, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(
					0, 0, 0, 6), 0, 0));
			}
			{
				translationExponentSpinner = new JSpinner();
				add(translationExponentSpinner, new GridBagConstraints(1, 9, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL, new Insets(0, 0, 6, 0), 0, 0));
				translationExponentSpinner.setModel(Util.newFloatSpinnerModel(0.75f, -2, 2, 0.05f));
			}
			{
				JLabel label = new JLabel("Sensitivity:");
				add(label, new GridBagConstraints(0, 10, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(0, 0, 0, 6), 0, 0));
			}
			{
				sensitivitySpinner = new JSpinner();
				add(sensitivitySpinner, new GridBagConstraints(1, 10, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL, new Insets(0, 0, 6, 0), 0, 0));
				sensitivitySpinner.setModel(new SpinnerNumberModel(1250, 1, 99999, 1));
			}
			{
				JLabel label = new JLabel("Diagonal dampen:");
				add(label, new GridBagConstraints(0, 11, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(0, 0, 0, 6), 0, 0));
			}
			{
				diagonalDampenSpinner = new JSpinner();
				add(diagonalDampenSpinner, new GridBagConstraints(1, 11, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL, new Insets(0, 0, 6, 0), 0, 0));
				diagonalDampenSpinner.setModel(Util.newFloatSpinnerModel(0f, 0, 1, 0.05f));
			}
			{
				JLabel label = new JLabel("Smoothness:");
				add(label, new GridBagConstraints(0, 12, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(0, 0, 0, 6), 0, 0));
			}
			{
				smoothnessSpinner = new JSpinner();
				add(smoothnessSpinner, new GridBagConstraints(1, 12, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL, new Insets(0, 0, 6, 0), 0, 0));
				smoothnessSpinner.setModel(Util.newFloatSpinnerModel(0.3f, 0, 1, 0.05f));
			}

			yxRatioSpinner.setValue(getYXRatio());
			smoothnessSpinner.setValue(getSmoothness());
			diagonalDampenSpinner.setValue(getDiagonalDampen());
			sensitivitySpinner.setValue(getSensitivity());
			translationExponentSpinner.setValue(getTranslationExponent());
		}

		public void update () {
			setYXRatio((Float)yxRatioSpinner.getValue());
			setSmoothness((Float)smoothnessSpinner.getValue());
			setDiagonalDampen((Float)diagonalDampenSpinner.getValue());
			setSensitivity((Integer)sensitivitySpinner.getValue());
			setTranslationExponent((Float)translationExponentSpinner.getValue());
		}
	}

	static native void setSmoothness (float intensity, int updateFrequency, float yxRatio, float translationExponent,
		float sensitivity);

	static native void computeStickValues (float deltaX, float deltaY, float yxRatio, float translationExponent,
		float sensitivity, float diagonalDampen, int deadZoneType, float deadzone, ByteBuffer buffer);

	public static void main (String[] args) {
		final XIM2MouseTranslation translation = new XIM2MouseTranslation();
		translation.setYXRatio(1);
		translation.setTranslationExponent(0.75f);
		translation.setDiagonalDampen(0.25f);
		translation.setSmoothness(0.3f);
		translation.setSensitivity(1250);
		JFrame frame = new JFrame("XIM2MouseTranslation");
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
