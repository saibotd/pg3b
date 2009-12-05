
package pg3b;

import static com.esotericsoftware.minlog.Log.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pg3b.util.Loader;

import com.esotericsoftware.minlog.Log;

public class Diagnostics {
	static public int TIMEOUT = 250;

	static public List<AxisCalibration> calibrate (final PG3B pg3b, final XboxController controller, Loader loader) {
		ArrayList<AxisCalibration> results = new ArrayList();
		int i = 0;
		Axis[] values = Axis.values();
		for (Axis axis : values) {
			loader.setMessage("Calibrating " + axis + "...");
			loader.throwCancelled();
			try {
				AxisCalibration calibration = pg3b.calibrate(axis, controller);
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
