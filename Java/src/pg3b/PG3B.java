
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

import net.java.games.input.Version;

import com.esotericsoftware.minlog.Log;

public class PG3B {
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

	public void set (Target target, float state) throws IOException {
		if (target == null) throw new IllegalArgumentException("target cannot be null.");
		if (state < -1 || state > 1) throw new IllegalArgumentException("state must be between -1 and 1 (inclusive): " + state);

		if (DEBUG) debug("pg3b", "Target " + target + ": " + state);

		float wiperValue;
		if (target == Target.leftTrigger || target == Target.rightTrigger)
			wiperValue = 255 - state * 255;
		else {
			wiperValue = (state + 1) * 127;
			if (target != Target.leftStickY && target != Target.rightStickY) wiperValue = 255 - wiperValue;
		}

		short actionKey = getActionKey(Device.xbox, (short)target.ordinal());
		short actionCode = getActionCode(actionKey, (short)wiperValue);
		action(actionCode);
	}

	public void set (Stick stick, float stateX, float stateY) throws IOException {
		Target targetX = stick == Stick.left ? Target.leftStickX : Target.rightStickX;
		Target targetY = stick == Stick.left ? Target.leftStickY : Target.rightStickY;
		set(targetX, stateX);
		set(targetY, stateY);
	}

	public String getPort () {
		return port;
	}

	public void close () {
		if (serialPort != null) serialPort.close();
	}

	public String calibrate (Target target, XboxController controller) throws IOException {
		boolean isTrigger = target == Target.leftTrigger || target == Target.rightTrigger;
		boolean isInverted = target == Target.leftStickY || target == Target.rightStickY;

		if (isTrigger) {
			// The triggers are mapped to the same Z axis by the MS driver and interfere with each other if not zero.
			set(Target.leftTrigger, 0);
			set(Target.rightTrigger, 0);
		}

		float[] actualValues = new float[256];
		for (int wiper = 0; wiper <= 255; wiper++) {
			float deflection = isTrigger ? wiper / 255f : wiper / 255f * 2 - 1;
			set(target, deflection);
			try {
				Thread.sleep(16);
			} catch (InterruptedException ignored) {
			}
			actualValues[isInverted ? 255 - wiper : wiper] = controller.get(target);
		}
		set(target, 0);

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

		// BOZO - Move out of this class.
		StringBuilder raw = new StringBuilder(1024);
		StringBuilder calibrated = new StringBuilder(1024);
		for (int i = 0; i <= 255; i += 2) {
			int wiper = isInverted ? 255 - i : i;
			raw.append((int)(actualValues[wiper] * 100 + 100) / 2);
			raw.append(",");
			calibrated.append((int)(actualValues[calibrationTable[wiper]] * 100 + 100) / 2);
			calibrated.append(",");
		}
		raw.setLength(raw.length() - 1);
		calibrated.setLength(calibrated.length() - 1);
		return "http://chart.apis.google.com/chart?chs=640x320&chf=bg,s,ffffff|c,s,ffffff&chxt=x,y&"
			+ "chxl=0:|0|63|127|191|255|1:|-1|0|1&cht=lc&chdl=Calibrated|Raw&chco=0000ff,ff0000&chdlp=b&chd=t:" + calibrated + "|"
			+ raw;
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

	static public enum Button {
		// Ordinals defined by firmware.
		a, b, x, y, up, down, left, right, leftShoulder, rightShoulder, leftStick, rightStick, start, guide, back
	}

	static public enum Target {
		// Ordinals defined by firmware.
		leftStickX, leftStickY, rightStickX, rightStickY, leftTrigger, rightTrigger
	}

	static public enum Stick {
		left, right
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
			pg3b.set(Target.leftStickX, i);
			Thread.sleep(50);
			float value = controller.get(Target.leftStickX);
			System.out.println(i + ", " + value);
		}

		pg3b.close();
	}
}
