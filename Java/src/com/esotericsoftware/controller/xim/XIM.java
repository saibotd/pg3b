
package com.esotericsoftware.controller.xim;

import static com.esotericsoftware.minlog.Log.*;

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
public class XIM extends Device {
	static {
		String ximPath = WindowsRegistry.get("HKCU/Software/XIM", "");
		if (ximPath != null) {
			System.load(ximPath + "/SiUSBXp.dll");
			System.load(ximPath + "/XIMCore.dll");
			System.loadLibrary("xim32");
		} else {
			if (ERROR) error("XIM installation path not found in registry at: HKCU/Software/XIM");
		}
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

	static private HashMap<Button, Integer> buttonToIndex = new HashMap();
	static {
		buttonToIndex.put(Button.rightShoulder, 0);
		buttonToIndex.put(Button.rightStick, 1);
		buttonToIndex.put(Button.leftShoulder, 2);
		buttonToIndex.put(Button.leftStick, 3);
		buttonToIndex.put(Button.a, 4);
		buttonToIndex.put(Button.b, 5);
		buttonToIndex.put(Button.x, 6);
		buttonToIndex.put(Button.y, 7);
		buttonToIndex.put(Button.up, 8);
		buttonToIndex.put(Button.down, 9);
		buttonToIndex.put(Button.left, 10);
		buttonToIndex.put(Button.right, 11);
		buttonToIndex.put(Button.start, 12);
		buttonToIndex.put(Button.back, 13);
		buttonToIndex.put(Button.guide, 14);
	}

	static private HashMap<Axis, Integer> axisToIndex = new HashMap();
	static {
		axisToIndex.put(Axis.rightStickX, 0);
		axisToIndex.put(Axis.rightStickY, 1);
		axisToIndex.put(Axis.leftStickX, 2);
		axisToIndex.put(Axis.leftStickY, 3);
		axisToIndex.put(Axis.rightTrigger, 4);
		axisToIndex.put(Axis.leftTrigger, 5);
	}

	private final ByteBuffer stateByteBuffer;
	private final ShortBuffer stateBuffer;
	private boolean collectingChanges;

	public XIM () throws IOException {
		checkResult(connect());

		stateByteBuffer = ByteBuffer.allocateDirect(28);
		stateByteBuffer.order(ByteOrder.nativeOrder());
		stateBuffer = stateByteBuffer.asShortBuffer();
	}

	public void close () {
		disconnect();
	}

	public void set (Button button, boolean pressed) throws IOException {
		int value = pressed ? 1 : 0;
		int index = buttonToIndex.get(button);
		short existingValue = stateBuffer.get(index / 2);
		int first, second;
		if (index % 2 == 0) {
			first = value & 0xFF;
			second = existingValue >> 8;
		} else {
			first = existingValue & 0xFF;
			second = value & 0xFF;
		}
		synchronized (this) {
			stateBuffer.put(index / 2, (short)(first + (second << 8)));
			if (collectingChanges) {
				buttonStates[button.ordinal()] = pressed;
				return;
			}
			checkResult(setState(stateByteBuffer, 200));
			buttonStates[button.ordinal()] = pressed;
		}
		if (DEBUG) debug(button + ": " + pressed);

		notifyButtonChanged(button, pressed);
	}

	public void set (Axis axis, float state) throws IOException {
		if (state <= -1) state = -1;
		if (state >= 1) state = 1;
		state = getDeflection(axis, state);
		int index = axisToIndex.get(axis);
		synchronized (this) {
			stateBuffer.put(7 + index, (short)(32767 * state));
			if (collectingChanges) {
				axisStates[axis.ordinal()] = state;
				return;
			}
			checkResult(setState(stateByteBuffer, 200));
			axisStates[axis.ordinal()] = state;
		}
		if (DEBUG) debug(axis + ": " + state);

		notifyAxisChanged(axis, state);
	}

	public void collectChanges () {
		collectingChanges = true;
	}

	public void applyChanges () throws IOException {
		collectingChanges = false;
		checkResult(setState(stateByteBuffer, 200));
	}

	/**
	 * If true, the thumbsticks can be used while the XIM is running.
	 */
	public void setThumsticksEnabled (boolean enabled) {
		setMode(enabled ? 1 : 0);
	}

	private void checkResult (int status) throws IOException {
		if (status == 0) return;
		throw new IOException("Error communicating with XIM: " + statusToMessage.get(status));
	}

	public String toString () {
		return "XIM";
	}

	static private native int connect ();

	static private native void disconnect ();

	static private native int setMode (int mode);

	static private native int setState (ByteBuffer byteBuffer, float timeout);

	static native void setSmoothness (float intensity, int inputUpdateFrequency, float stickYXRatio,
		float stickTranslationExponent, float stickSensitivity);

	static native void computeStickValues (float deltaX, float deltaY, float stickYXRatio, float stickTranslationExponent,
		float stickSensitivity, float stickDiagonalDampen, int stickDeadZoneType, float stickDeadZone, ByteBuffer byteBuffer);
}