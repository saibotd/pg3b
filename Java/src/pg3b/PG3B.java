
package pg3b;

import static com.esotericsoftware.minlog.Log.*;
import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Formatter;
import java.util.HashMap;

import com.esotericsoftware.minlog.Log;

public class PG3B {
	static private final HashMap<String, Target> nameToTarget = new HashMap();
	static {
		for (Axis axis : Axis.values()) {
			nameToTarget.put(axis.name().toLowerCase(), axis);
			String friendlyName = axis.toString().toLowerCase();
			nameToTarget.put(friendlyName, axis);
			nameToTarget.put(friendlyName.substring(0, friendlyName.length() - 5), axis);
		}
		for (Button button : Button.values()) {
			nameToTarget.put(button.name().toLowerCase(), button);
			String friendlyName = button.toString().toLowerCase();
			nameToTarget.put(friendlyName, button);
			nameToTarget.put(friendlyName.substring(0, friendlyName.length() - 7), button);
		}
	}

	private final SerialPort serialPort;
	private int sequenceNumber;
	private BufferedReader input;
	private Formatter output;
	private OutputStream outputStream;
	private final String port;

	public PG3B (String port) throws IOException {
		this(port, 100);
	}

	public PG3B (String port, int timeout) throws IOException {
		if (port == null) throw new IllegalArgumentException("portID cannot be null.");
		this.port = port;

		try {
			CommPortIdentifier identifier = CommPortIdentifier.getPortIdentifier(port);
			CommPort commPort = identifier.open("PG3B", 2000);
			if (!(commPort instanceof SerialPort)) throw new IOException("Port is not serial: " + port);

			serialPort = (SerialPort)commPort;
			serialPort.setSerialPortParams(230400, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
			serialPort.setDTR(true);
			// serialPort.enableReceiveTimeout(timeout);

			Charset ascii = Charset.forName("ASCII");
			input = new BufferedReader(new InputStreamReader(serialPort.getInputStream(), ascii), 256);
			output = new Formatter(new OutputStreamWriter(serialPort.getOutputStream(), ascii));

			command(Command.setCalibrationEnabled, 0);
			command(Command.setIsWireless, 1);
		} catch (Exception ex) {
			close();
			throw new IOException("Error opening connection on port: " + port, ex);
		}
	}

	private synchronized void primitive (Command command, int commandArgument, int argumentSize) throws IOException {
		String commandFormat = argumentSize == 2 ? "X %04X %C %02X\r" : "X %04X %C %04X\r";
		if (TRACE) trace("pg3b", "Sent: " + String.format(commandFormat, sequenceNumber, command.code, commandArgument).trim());
		output.format(commandFormat, sequenceNumber, command.code, commandArgument);
		output.flush();

		String ack = String.format("X %04X OK", sequenceNumber++);
		while (true) {
			String response = input.readLine();
			if (response == null) throw new IOException("Connection was closed.");
			if (response.equals(ack)) {
				if (TRACE) trace("pg3b", "Ackd: " + ack);
				break;
			}
		}
	}

	private void command (Command command, int commandArgument) throws IOException {
		primitive(command, commandArgument, 2);
	}

	private void action (int actionCode) throws IOException {
		primitive(Command.action, actionCode, 4);
	}

	private short getActionCode (short key, short value) {
		return (short)(key | value);
	}

	private short getActionKey (Device device, short target) {
		short d = (short)device.ordinal();
		return (short)(d << 12 | target << 8);
	}

	public boolean isConnected () {
		try {
			command(Command.setDebugMessagesEnabled, 0);
			return true;
		} catch (IOException ignored) {
			return false;
		}
	}

	public void set (Button button, boolean pressed) throws IOException {
		if (button == null) throw new IllegalArgumentException("button cannot be null.");

		if (DEBUG) debug("pg3b", "Button " + button + ": " + pressed);

		int state = pressed ? 6 : 7;
		short actionKey = getActionKey(Device.xbox, (short)state);
		short actionCode = getActionCode(actionKey, (short)button.ordinal());
		action(actionCode);
	}

	public void set (Axis axis, float state) throws IOException {
		if (axis == null) throw new IllegalArgumentException("axis cannot be null.");
		if (state < -1 || state > 1) throw new IllegalArgumentException("state must be between -1 and 1 (inclusive): " + state);

		if (DEBUG) debug("pg3b", "Axis " + axis + ": " + state);

		float wiperValue;
		if (axis == Axis.leftTrigger || axis == Axis.rightTrigger)
			wiperValue = 255 - state * 255;
		else {
			wiperValue = (state + 1) * 127;
			if (axis != Axis.leftStickY && axis != Axis.rightStickY) wiperValue = 255 - wiperValue;
		}

		short actionKey = getActionKey(Device.xbox, (short)axis.ordinal());
		short actionCode = getActionCode(actionKey, (short)wiperValue);
		action(actionCode);
	}

	public void set (Stick stick, float stateX, float stateY) throws IOException {
		Axis axisX = stick == Stick.left ? Axis.leftStickX : Axis.rightStickX;
		Axis axisY = stick == Stick.left ? Axis.leftStickY : Axis.rightStickY;
		set(axisX, stateX);
		set(axisY, stateY);
	}

	public void set (Target target, boolean pressed) throws IOException {
		if (target instanceof Button)
			set((Button)target, pressed);
		else if (target instanceof Axis)
			set((Axis)target, pressed ? 1 : 0);
		else
			throw new IllegalArgumentException("target must be a button or axis.");
	}

	public void set (Target target, float state) throws IOException {
		if (target instanceof Button)
			set((Button)target, state != 0);
		else if (target instanceof Axis)
			set((Axis)target, state);
		else
			throw new IllegalArgumentException("target must be a button or axis.");
	}

	public void set (String target, boolean pressed) throws IOException {
		set(getTarget(target), pressed);
	}

	public void set (String target, float state) throws IOException {
		set(getTarget(target), state);
	}

	public String getPort () {
		return port;
	}

	public void close () {
		if (serialPort != null) serialPort.close();
	}

	public AxisCalibration calibrate (Axis axis, XboxController controller) throws IOException {
		boolean isTrigger = axis == Axis.leftTrigger || axis == Axis.rightTrigger;
		if (isTrigger) {
			// The triggers are mapped to the same Z axis by the MS driver and interfere with each other if not zero.
			set(Axis.leftTrigger, 0);
			set(Axis.rightTrigger, 0);
		}

		float[] actualValues = new float[256];
		try {
			for (int wiper = 0; wiper <= 255; wiper++) {
				float deflection = isTrigger ? wiper / 255f : wiper / 255f * 2 - 1;
				set(axis, deflection);
				if (Thread.interrupted()) return null;
				try {
					Thread.sleep(16);
				} catch (InterruptedException ex) {
					return null;
				}
				actualValues[wiper] = controller.get(axis);
			}
		} finally {
			set(axis, 0);
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

		return new AxisCalibration(calibrationTable, actualValues);
	}

	private int findClosestIndex (float[] actualValues, int target) {
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

	static public Target getTarget (String name) {
		if (name == null) throw new IllegalArgumentException("name cannot be null.");
		return nameToTarget.get(name.trim().toLowerCase());
	}

	static private enum Device {
		// Ordinals defined by firmware.
		none, keyboard, mouse, usb, xbox, arcade, joystick, script
	}

	static private enum Command {
		action('A'), //
		eventAction('E'), //
		setCalibrationEnabled('C'), //
		setDebugMessagesEnabled('D'), //
		setFrequency('F'), //
		setIsWireless('G'), //
		setResolution('R'), //
		setScaling('S'), //
		initializeProfile('P'), //
		finalizeProfile('Q'), //
		writeEeprom('W');

		char code;

		private Command (char code) {
			this.code = code;
		}
	}

	public static void main (String[] args) throws Exception {
		Log.set(LEVEL_INFO);

		XboxController controller = new XboxController(XboxController.getAllControllers().get(0));
		PG3B pg3b = new PG3B("COM3");

		for (float i = -1; i <= 1; i += 0.05) {
			pg3b.set(Axis.leftStickX, i);
			Thread.sleep(50);
			float value = controller.get(Axis.leftStickX);
			System.out.println(i + ", " + value);
		}

		pg3b.close();
	}
}
