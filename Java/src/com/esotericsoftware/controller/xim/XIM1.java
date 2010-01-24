
package com.esotericsoftware.controller.xim;

import static com.esotericsoftware.minlog.Log.*;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.HashMap;

import com.esotericsoftware.controller.device.Axis;
import com.esotericsoftware.controller.device.Button;
import com.esotericsoftware.controller.device.Device;
import com.esotericsoftware.controller.util.WindowsRegistry;

/**
 * Controls the XIM1 hardware.
 */
public class XIM1 extends Device {
	static private Boolean isValid;

	static public boolean isValid (boolean forceRetry) {
		if (isValid != null) return isValid;
		String ximPath = WindowsRegistry.get("HKCU/Software/XIM", "");
		if (ximPath != null) {
			if (new File(ximPath).exists()) {
				try {
					System.load(ximPath + "\\XIMCore.dll");
					System.loadLibrary("xim1");
					isValid = true;
					return true;
				} catch (Throwable ex) {
					if ((forceRetry && ERROR) || (!forceRetry && DEBUG)) error("Error loading XIM1 native libraries.", ex);
				}
			} else {
				if (ERROR) error("Invalid XIM1 installation path in registry at: HKCU/Software/XIM\nInvalid path: " + ximPath);
			}
		} else {
			if (ERROR) {
				error("XIM1 installation path not found in registry at: HKCU/Software/XIM\n"
					+ "Please ensure the XIM1 software is installed.");
			}
		}
		isValid = false;
		return false;
	}

	private ByteBuffer stateByteBuffer;
	private IntBuffer buttonStateBuffer;

	public XIM1 () throws IOException {
		checkResult(connect());

		stateByteBuffer = ByteBuffer.allocateDirect(72);
		stateByteBuffer.order(ByteOrder.nativeOrder());
		buttonStateBuffer = stateByteBuffer.asIntBuffer();
	}

	public void setButton (Button button, boolean pressed) throws IOException {
		int index = buttonToIndex[button.ordinal()];
		synchronized (this) {
			buttonStateBuffer.put(index, pressed ? 1 : 0);
			if (collectingChangesThread != Thread.currentThread()) checkResult(setState(stateByteBuffer));
		}
	}

	public void setAxis (Axis axis, float state) throws IOException {
		int index = axisToIndex[axis.ordinal()];
		synchronized (this) {
			if (axis.isTrigger())
				buttonStateBuffer.put(index, state == 0 ? 0 : 1);
			else
				stateByteBuffer.put(index, (byte)(127 * state));
			if (collectingChangesThread != Thread.currentThread()) checkResult(setState(stateByteBuffer));
		}
	}

	void checkResult (int status) throws IOException {
		if (status == 0) return;
		throw new IOException("Error communicating with XIM1: " + statusToMessage.get(status));
	}

	public void close () {
		disconnect();
	}

	public String toString () {
		return "XIM1";
	}

	static private HashMap<Integer, String> statusToMessage = new HashMap();
	static {
		statusToMessage.put(0, "OK");
		statusToMessage.put(101, "INVALID_INPUT_REFERENCE");
		statusToMessage.put(102, "INVALID_STICK_VALUE");
		statusToMessage.put(103, "INVALID_BUFFER");
		statusToMessage.put(104, "INVALID_DEADZONE_TYPE");
		statusToMessage.put(105, "HARDWARE_ALREADY_CONNECTED");
		statusToMessage.put(106, "HARDWARE_NOT_CONNECTED");
		statusToMessage.put(401, "DEVICE_NOT_FOUND");
		statusToMessage.put(402, "DEVICE_CONNECTION_FAILED");
		statusToMessage.put(403, "CONFIGURATION_FAILED");
		statusToMessage.put(404, "READ_FAILED");
		statusToMessage.put(405, "WRITE_FAILED");
		statusToMessage.put(406, "TRANSFER_CORRUPTION");
	}

	static private int[] buttonToIndex = new int[Button.values().length];
	static {
		buttonToIndex[Button.leftShoulder.ordinal()] = 1;
		buttonToIndex[Button.leftStick.ordinal()] = 2;
		buttonToIndex[Button.rightShoulder.ordinal()] = 4;
		buttonToIndex[Button.rightStick.ordinal()] = 5;
		buttonToIndex[Button.a.ordinal()] = 6;
		buttonToIndex[Button.b.ordinal()] = 7;
		buttonToIndex[Button.x.ordinal()] = 8;
		buttonToIndex[Button.y.ordinal()] = 9;
		buttonToIndex[Button.up.ordinal()] = 10;
		buttonToIndex[Button.down.ordinal()] = 11;
		buttonToIndex[Button.left.ordinal()] = 12;
		buttonToIndex[Button.right.ordinal()] = 13;
		buttonToIndex[Button.start.ordinal()] = 14;
		buttonToIndex[Button.back.ordinal()] = 15;
		buttonToIndex[Button.guide.ordinal()] = 16;
	}

	static private int[] axisToIndex = new int[Axis.values().length];
	static {
		axisToIndex[Axis.rightStickX.ordinal()] = 68;
		axisToIndex[Axis.rightStickY.ordinal()] = 69;
		axisToIndex[Axis.leftStickX.ordinal()] = 70;
		axisToIndex[Axis.leftStickY.ordinal()] = 71;
		axisToIndex[Axis.rightTrigger.ordinal()] = 3;
		axisToIndex[Axis.leftTrigger.ordinal()] = 0;
	}

	static native int connect ();

	static native void disconnect ();

	static native int setState (ByteBuffer byteBuffer);
}
