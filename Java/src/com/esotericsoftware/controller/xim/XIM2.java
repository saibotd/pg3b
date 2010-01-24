
package com.esotericsoftware.controller.xim;

import static com.esotericsoftware.minlog.Log.*;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.HashMap;

import com.esotericsoftware.controller.device.Axis;
import com.esotericsoftware.controller.device.Button;
import com.esotericsoftware.controller.device.Device;
import com.esotericsoftware.controller.util.WindowsRegistry;

/**
 * Controls the XIM2 hardware.
 */
public class XIM2 extends Device {
	static private Boolean isValid;

	static public boolean isValid (boolean forceRetry) {
		if (isValid != null) return isValid;
		String ximPath = WindowsRegistry.get("HKCU/Software/XIM", "");
		if (ximPath != null) {
			if (new File(ximPath).exists()) {
				try {
					System.load(ximPath + "\\SiUSBXp.dll");
					System.load(ximPath + "\\XIMCore.dll");
					System.loadLibrary("xim2");
					isValid = true;
					return true;
				} catch (Throwable ex) {
					if ((forceRetry && ERROR) || (!forceRetry && DEBUG)) error("Error loading XIM2 native libraries.", ex);
				}
			} else {
				if (ERROR) error("Invalid XIM2 installation path in registry at: HKCU/Software/XIM\nInvalid path: " + ximPath);
			}
		} else {
			if (ERROR) {
				error("XIM2 installation path not found in registry at: HKCU/Software/XIM\n"
					+ "Please ensure the XIM2 software is installed.");
			}
		}
		isValid = false;
		return false;
	}

	private ByteBuffer stateByteBuffer;
	private ShortBuffer axisStateBuffer;

	public XIM2 () throws IOException {
		checkResult(connect());

		stateByteBuffer = ByteBuffer.allocateDirect(28);
		stateByteBuffer.order(ByteOrder.nativeOrder());
		stateByteBuffer.position(16);
		axisStateBuffer = stateByteBuffer.slice().order(ByteOrder.nativeOrder()).asShortBuffer();
	}

	public void setButton (Button button, boolean pressed) throws IOException {
		int index = buttonToIndex[button.ordinal()];
		synchronized (this) {
			stateByteBuffer.put(index, (byte)(pressed ? 1 : 0));
			if (collectingChangesThread != Thread.currentThread()) checkResult(setState(stateByteBuffer, 0));
		}
	}

	public void setAxis (Axis axis, float state) throws IOException {
		int index = axisToIndex[axis.ordinal()];
		synchronized (this) {
			axisStateBuffer.put(index, (short)(32767 * state));
			if (collectingChangesThread != Thread.currentThread()) checkResult(setState(stateByteBuffer, 0));
		}
	}

	/**
	 * If true, the thumbsticks can be used while the XIM is running.
	 * @throws IOException When communication with the XIM fails.
	 */
	public void setThumsticksEnabled (boolean enabled) throws IOException {
		checkResult(setMode(enabled ? 1 : 0));
	}

	void checkResult (int status) throws IOException {
		if (status == 0) return;
		throw new IOException("Error communicating with XIM2: " + statusToMessage.get(status));
	}

	public void close () {
		disconnect();
	}

	public String toString () {
		return "XIM2";
	}

	static private HashMap<Integer, String> statusToMessage = new HashMap();
	static {
		statusToMessage.put(0, "OK");
		statusToMessage.put(101, "INVALID_INPUT_REFERENCE");
		statusToMessage.put(102, "INVALID_MODE");
		statusToMessage.put(103, "INVALID_STICK_VALUE");
		statusToMessage.put(104, "INVALID_TRIGGER_VALUE");
		statusToMessage.put(105, "INVALID_TIMEOUT_VALUE");
		statusToMessage.put(107, "INVALID_BUFFER");
		statusToMessage.put(108, "INVALID_DEADZONE_TYPE");
		statusToMessage.put(109, "HARDWARE_ALREADY_CONNECTED");
		statusToMessage.put(109, "HARDWARE_NOT_CONNECTED");
		statusToMessage.put(401, "DEVICE_NOT_FOUND");
		statusToMessage.put(402, "DEVICE_CONNECTION_FAILED");
		statusToMessage.put(403, "CONFIGURATION_FAILED");
		statusToMessage.put(404, "READ_FAILED");
		statusToMessage.put(405, "WRITE_FAILED");
		statusToMessage.put(406, "TRANSFER_CORRUPTION");
		statusToMessage.put(407, "NEEDS_CALIBRATION");
	}

	static private int[] buttonToIndex = new int[Button.values().length];
	static {
		buttonToIndex[Button.rightShoulder.ordinal()] = 0;
		buttonToIndex[Button.rightStick.ordinal()] = 1;
		buttonToIndex[Button.leftShoulder.ordinal()] = 2;
		buttonToIndex[Button.leftStick.ordinal()] = 3;
		buttonToIndex[Button.a.ordinal()] = 4;
		buttonToIndex[Button.b.ordinal()] = 5;
		buttonToIndex[Button.x.ordinal()] = 6;
		buttonToIndex[Button.y.ordinal()] = 7;
		buttonToIndex[Button.up.ordinal()] = 8;
		buttonToIndex[Button.down.ordinal()] = 9;
		buttonToIndex[Button.left.ordinal()] = 10;
		buttonToIndex[Button.right.ordinal()] = 11;
		buttonToIndex[Button.start.ordinal()] = 12;
		buttonToIndex[Button.back.ordinal()] = 13;
		buttonToIndex[Button.guide.ordinal()] = 14;
	}

	static private int[] axisToIndex = new int[Axis.values().length];
	static {
		axisToIndex[Axis.rightStickX.ordinal()] = 0;
		axisToIndex[Axis.rightStickY.ordinal()] = 1;
		axisToIndex[Axis.leftStickX.ordinal()] = 2;
		axisToIndex[Axis.leftStickY.ordinal()] = 3;
		axisToIndex[Axis.rightTrigger.ordinal()] = 4;
		axisToIndex[Axis.leftTrigger.ordinal()] = 5;
	}

	static native int connect ();

	static native void disconnect ();

	static native int setMode (int mode);

	static native int setState (ByteBuffer byteBuffer, float delay);
}
