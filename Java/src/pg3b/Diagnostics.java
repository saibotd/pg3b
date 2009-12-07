
package pg3b;

import static com.esotericsoftware.minlog.Log.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.omg.CORBA.TIMEOUT;

import pg3b.util.Loader;

import com.esotericsoftware.minlog.Log;

/**
 * Provides methods for various PG3B diagnostic tasks.
 */
public class Diagnostics {
	/**
	 * The max amount of time to wait for {@link #waitForAxis(PG3B, XboxController, Axis, float)} and
	 * {@link #waitForButton(PG3B, XboxController, Button, boolean)}.
	 */
	static public int TIMEOUT = 250;

	/**
	 * Calibrates all PG3B axes.
	 */
	static public List<AxisCalibration> calibrate (final PG3B pg3b, final XboxController controller, Loader loader) {
		ArrayList<AxisCalibration> results = new ArrayList();
		int i = 0;
		Axis[] values = Axis.values();
		for (Axis axis : values) {
			loader.setMessage("Calibrating " + axis + "...");
			loader.throwCancelled();
			try {
				AxisCalibration calibration = calibrate(pg3b, controller, axis);
				if (calibration == null) {
					loader.cancel();
					loader.throwCancelled();
				}
				results.add(calibration);
			} catch (IOException ex) {
				if (Log.ERROR) error("Error calibrating axis: " + axis, ex);
			}
			loader.setPercentageComplete(++i / (float)values.length);
		}
		return results;
	}

	/**
	 * Calibrates a specific PG3B axis.
	 */
	static public AxisCalibration calibrate (PG3B pg3b, XboxController controller, Axis axis) throws IOException {
		if (axis == null) throw new IllegalArgumentException("axis cannot be null.");
		if (controller == null) throw new IllegalArgumentException("controller cannot be null.");

		boolean isTrigger = axis == Axis.leftTrigger || axis == Axis.rightTrigger;
		if (isTrigger) {
			// The triggers are mapped to the same Z axis by the (crappy) MS driver and interfere with each other if not zero.
			pg3b.set(Axis.leftTrigger, 0);
			pg3b.set(Axis.rightTrigger, 0);
		}

		float[] actualValues = new float[256];
		try {
			for (int wiper = 0; wiper <= 255; wiper++) {
				float deflection = isTrigger ? wiper / 255f : wiper / 255f * 2 - 1;
				pg3b.set(axis, deflection);
				if (Thread.interrupted()) return null;
				try {
					Thread.sleep(16);
				} catch (InterruptedException ex) {
					return null;
				}
				actualValues[wiper] = controller.get(axis);
			}
		} finally {
			pg3b.set(axis, 0);
		}

		int[] calibrationTable = new int[256];
		int minusOneIndex = findClosestIndex(actualValues, -1);
		int zeroIndex = findClosestIndex(actualValues, 0);
		int plusOneIndex = findClosestIndex(actualValues, 1);
		for (int wiper = 0; wiper <= 255; wiper++) {
			float deflection = isTrigger ? wiper / 255f : wiper / 255f * 2 - 1;
			int match = zeroIndex;
			for (int index = minusOneIndex; index <= plusOneIndex; index++)
				if (Math.abs(actualValues[index] - deflection) < Math.abs(actualValues[match] - deflection)) match = index;
			calibrationTable[wiper] = match;
		}
		calibrationTable[0] = minusOneIndex;
		calibrationTable[127] = zeroIndex;
		calibrationTable[255] = plusOneIndex;

		return new AxisCalibration(axis, calibrationTable, actualValues);
	}

	static private int findClosestIndex (float[] actualValues, int target) {
		// If target is negative, finds index of the last number closest to the target.
		// Otherwise, finds index of the first number closest to the target.
		int closestIndex = -1;
		float closestToZero = Float.MAX_VALUE;
		for (int i = 0; i < actualValues.length; i++) {
			float absValue = Math.abs(actualValues[i] - target);
			boolean isLess = target < 0 ? absValue <= closestToZero : absValue < closestToZero;
			if (isLess) {
				closestToZero = absValue;
				closestIndex = i;
			}
		}
		if (target == 0) {
			// If looking for zero, handle the closest value to zero appearing multiple times in a row.
			int zeroCount = 0;
			for (int i = closestIndex + 1; i < actualValues.length; i++) {
				float absValue = Math.abs(actualValues[i]);
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
	static public Map<Target, Boolean> roundTrip (final PG3B pg3b, final XboxController controller, Loader loader) {
		HashMap<Target, Boolean> status = new HashMap();

		for (Button button : Button.values()) {
			if (button == Button.start || button == Button.guide) continue;
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
	static public boolean waitForButton (PG3B pg3b, XboxController controller, Button button, boolean pressed) {
		try {
			pg3b.set(button, pressed);
			long startTime = System.currentTimeMillis();
			while (controller.get(button) != pressed) {
				if (System.currentTimeMillis() - startTime > TIMEOUT) {
					if (WARN) warn("Timed out setting button: " + button);
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
	static public boolean waitForAxis (PG3B pg3b, XboxController controller, Axis axis, float value) {
		try {
			pg3b.set(axis, value);
			long startTime = System.currentTimeMillis();
			while (true) {
				float actualValue = controller.get(axis);
				if (Math.abs(actualValue - value) < 0.05f) break;
				if (System.currentTimeMillis() - startTime > TIMEOUT) {
					if (WARN) warn("Timed out setting axis: " + axis + " (actual value: " + actualValue + ", needed: " + value + ")");
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
