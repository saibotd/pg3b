
package com.esotericsoftware.controller.ui;

import static com.esotericsoftware.minlog.Log.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.omg.CORBA.TIMEOUT;

import com.esotericsoftware.controller.device.Axis;
import com.esotericsoftware.controller.device.Button;
import com.esotericsoftware.controller.device.Device;
import com.esotericsoftware.controller.device.Target;
import com.esotericsoftware.controller.input.XboxController;
import com.esotericsoftware.controller.pg3b.PG3B;
import com.esotericsoftware.controller.util.Loader;
import com.esotericsoftware.minlog.Log;

/**
 * Provides methods for various device diagnostic tasks.
 */
public class Diagnostics {
	/**
	 * The max amount of time to wait for {@link #waitForAxis(Device, XboxController, Axis, float)} and
	 * {@link #waitForButton(Device, XboxController, Button, boolean)}.
	 */
	static public int TIMEOUT = 250;

	/**
	 * Reads all the values for a specific axis.
	 */
	static public float[] getRawValues (Axis axis, PG3B pg3b, XboxController controller) throws IOException {
		if (axis == null) throw new IllegalArgumentException("axis cannot be null.");
		if (pg3b == null) throw new IllegalArgumentException("pg3b cannot be null.");
		if (controller == null) throw new IllegalArgumentException("controller cannot be null.");

		boolean calibrationEnabled = pg3b.isCalibrationEnabled();
		pg3b.setCalibrationEnabled(false);
		pg3b.reset();

		try {
			float[] actualValues = new float[256];
			for (int wiper = 0; wiper <= 255; wiper++) {
				float deflection = axis.isTrigger() ? wiper / 255f : wiper / 255f * 2 - 1;
				pg3b.set(axis, deflection);
				if (Thread.interrupted()) return null;
				try {
					Thread.sleep(16);
				} catch (InterruptedException ex) {
					return null;
				}
				actualValues[wiper] = controller.get(axis);
			}
			return actualValues;
		} finally {
			pg3b.set(axis, 0);
			pg3b.setCalibrationEnabled(calibrationEnabled);
			pg3b.reset();
		}
	}

	/**
	 * Returns the calibration table for the axis using the specified raw values.
	 */
	static public byte[] getCalibrationTable (Axis axis, float[] rawValues) {
		if (axis == null) throw new IllegalArgumentException("axis cannot be null.");

		byte[] calibrationTable = new byte[256];
		int minusOneIndex = findClosestIndex(rawValues, -1);
		int zeroIndex = findClosestIndex(rawValues, 0);
		int plusOneIndex = findClosestIndex(rawValues, 1);
		for (int wiper = 0; wiper <= 255; wiper++) {
			float deflection = axis.isTrigger() ? wiper / 255f : wiper / 255f * 2 - 1;
			int match = zeroIndex;
			for (int index = 0; index < 256; index++)
				if (Math.abs(rawValues[index] - deflection) < Math.abs(rawValues[match] - deflection)) match = index;
			calibrationTable[wiper] = (byte)match;
		}
		calibrationTable[0] = (byte)minusOneIndex;
		calibrationTable[127] = (byte)zeroIndex;
		calibrationTable[255] = (byte)plusOneIndex;

		return calibrationTable;
	}

	static private int findClosestIndex (float[] rawValues, int target) {
		// If target is negative, finds index of the last number closest to the target.
		// Otherwise, finds index of the first number closest to the target.
		int closestIndex = -1;
		float closestToZero = Float.MAX_VALUE;
		for (int i = 0; i < rawValues.length; i++) {
			float absValue = Math.abs(rawValues[i] - target);
			boolean isLess = target < 0 ? absValue <= closestToZero : absValue < closestToZero;
			if (isLess) {
				closestToZero = absValue;
				closestIndex = i;
			}
		}
		if (target == 0) {
			// If looking for zero, handle the closest value to zero appearing multiple times in a row.
			int zeroCount = 0;
			for (int i = closestIndex + 1; i < rawValues.length; i++) {
				float absValue = Math.abs(rawValues[i]);
				if (absValue == closestToZero)
					zeroCount++;
				else
					break;
			}
			closestIndex += zeroCount / 2;
		}
		return closestIndex;
	}

	/**
	 * Manipulats each PG3B button and axis and verifies that the controller changes appropriately.
	 */
	static public Map<Target, Boolean> roundTrip (final Device pg3b, final XboxController controller, Loader loader) {
		HashMap<Target, Boolean> status = new HashMap();

		for (Button button : Button.values()) {
			if (button == Button.guide) continue;
			loader.setMessage("Testing " + button + "...");
			loader.throwCancelled();
			try {
				boolean success = waitForButton(pg3b, controller, button, false);
				loader.throwCancelled();
				if (success) success = waitForButton(pg3b, controller, button, true);
				loader.throwCancelled();
				if (success) success = waitForButton(pg3b, controller, button, false);
				status.put(button, success);
			} finally {
				try {
					pg3b.set(button, false);
				} catch (IOException ignored) {
				}
			}
		}

		for (Axis axis : Axis.values()) {
			loader.setMessage("Testing " + axis + "...");
			loader.throwCancelled();
			try {
				boolean success = waitForAxis(pg3b, controller, axis, 1);
				loader.throwCancelled();
				if (axis != Axis.leftTrigger && axis != Axis.rightTrigger) {
					if (success) success = waitForAxis(pg3b, controller, axis, -1);
				}
				status.put(axis, success);
			} finally {
				try {
					pg3b.set(axis, 0);
				} catch (IOException ignored) {
				}
			}
		}

		return status;
	}

	/**
	 * Sets the button state and waits until the controller reports the same button state or {@link TIMEOUT} is reached.
	 */
	static public boolean waitForButton (Device pg3b, XboxController controller, Button button, boolean pressed) {
		try {
			pg3b.set(button, pressed);
			long startTime = System.currentTimeMillis();
			while (controller.get(button) != pressed) {
				if (System.currentTimeMillis() - startTime > TIMEOUT) {
					if (WARN) warn("Round trip timed out: " + button + " (actual: " + !pressed + ", needed: " + pressed + ")");
					return false;
				}
				Thread.yield();
			}
			return true;
		} catch (IOException ex) {
			if (Log.ERROR) error("Error setting button: " + button, ex);
			return false;
		}
	}

	/**
	 * Sets the axis state and waits until the controller reports the same axis state or {@link TIMEOUT} is reached.
	 */
	static public boolean waitForAxis (Device pg3b, XboxController controller, Axis axis, float value) {
		try {
			pg3b.set(axis, value);
			long startTime = System.currentTimeMillis();
			while (true) {
				float actualValue = controller.get(axis);
				if (Math.abs(actualValue - value) < 0.05f) break;
				if (System.currentTimeMillis() - startTime > TIMEOUT) {
					if (WARN) warn("Round trip timed out: " + axis + " (actual: " + actualValue + ", needed: " + value + ")");
					return false;
				}
				Thread.yield();
			}
			return true;
		} catch (IOException ex) {
			if (Log.ERROR) error("Error setting axis: " + axis, ex);
			return false;
		}
	}
}
