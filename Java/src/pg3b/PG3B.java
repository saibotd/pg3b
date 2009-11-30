
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
			// if (identifier.isCurrentlyOwned())
			// throw new IOException("Port is already in use: " + portID, new PortInUseException());

			CommPort commPort = identifier.open("PG3B", 2000);
			if (!(commPort instanceof SerialPort)) throw new IOException("Port is not serial: " + port);

			serialPort = (SerialPort)commPort;
			serialPort.setSerialPortParams(230400, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
			serialPort.setDTR(true);
			// serialPort.enableReceiveTimeout(timeout);

			Charset ascii = Charset.forName("ASCII");
			input = new BufferedReader(new InputStreamReader(serialPort.getInputStream(), ascii), 256);
			output = new Formatter(new OutputStreamWriter(serialPort.getOutputStream(), ascii));

			command(Command.SetCalibrationEnabled, 0);
			command(Command.SetIsWireless, 1);
		} catch (Exception ex) {
			close();
			throw new IOException("Error opening connection on port: " + port, ex);
		}
	}

	private void primitive (Command command, int commandArgument, int argumentSize) throws IOException {
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
		primitive(Command.Action, actionCode, 4);
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
			command(Command.SetDebugMessagesEnabled, 0);
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
			wiperValue = 256 - state * 256;
		else {
			wiperValue = (state + 1) * 128;
			if (target != Target.leftStickY && target != Target.rightStickY) wiperValue = 256 - wiperValue;
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

	public static void main (String[] args) throws Exception {
		Log.set(LEVEL_INFO);

		XboxController controller = new XboxController(XboxController.getAllControllers().get(0));
		PG3B pg3b = new PG3B("COM3");

		for (float i = -1; i <= 1; i += 0.05) {
			pg3b.set(Target.leftStickX, i);
			Thread.sleep(50);
			controller.poll();
			float value = controller.get(Target.leftStickX);
			System.out.println(i + ", " + value);
		}

		pg3b.close();
	}

	public void close () {
		if (serialPort != null) serialPort.close();
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
		Action('A'), //
		EventAction('E'), //
		SetCalibrationEnabled('C'), //
		SetDebugMessagesEnabled('D'), //
		SetFrequency('F'), //
		SetIsWireless('G'), //
		SetResolution('R'), //
		SetScaling('S'), //
		InitializeProfile('P'), //
		FinalizeProfile('Q'), //
		WriteEeprom('W');

		char code;

		private Command (char code) {
			this.code = code;
		}
	}
}
