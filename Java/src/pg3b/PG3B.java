
package pg3b;

import static com.esotericsoftware.minlog.Log.*;
import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.HashMap;

import pg3b.util.Listeners;

/**
 * Controls the PG3B hardware.
 */
public class PG3B {
	static private final HashMap<String, Target> nameToTarget = new HashMap();
	static {
		for (Axis axis : Axis.values()) {
			nameToTarget.put(axis.name().toLowerCase(), axis);
			String friendlyName = axis.toString().toLowerCase();
			nameToTarget.put(friendlyName, axis);
			nameToTarget.put(friendlyName.substring(0, friendlyName.length() - 5), axis);
			if (axis.getAlias() != null) nameToTarget.put(axis.getAlias().toLowerCase(), axis);
		}
		for (Button button : Button.values()) {
			nameToTarget.put(button.name().toLowerCase(), button);
			String friendlyName = button.toString().toLowerCase();
			nameToTarget.put(friendlyName, button);
			nameToTarget.put(friendlyName.substring(0, friendlyName.length() - 7), button);
			if (button.getAlias() != null) nameToTarget.put(button.getAlias().toLowerCase(), button);
		}
	}

	static private final char[] hex = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
	static private final int[] charToDigit = new int[102];
	static {
		charToDigit['0'] = 0;
		charToDigit['1'] = 1;
		charToDigit['2'] = 2;
		charToDigit['3'] = 3;
		charToDigit['4'] = 4;
		charToDigit['5'] = 5;
		charToDigit['6'] = 6;
		charToDigit['7'] = 7;
		charToDigit['8'] = 8;
		charToDigit['9'] = 9;
		charToDigit['A'] = 10;
		charToDigit['B'] = 11;
		charToDigit['C'] = 12;
		charToDigit['D'] = 13;
		charToDigit['E'] = 14;
		charToDigit['F'] = 15;
		charToDigit['a'] = 10;
		charToDigit['b'] = 11;
		charToDigit['c'] = 12;
		charToDigit['d'] = 13;
		charToDigit['e'] = 14;
		charToDigit['f'] = 15;
	}

	private final String port;
	private final SerialPort serialPort;
	private short sequenceNumber;
	private BufferedReader input;
	private OutputStreamWriter output;
	private final PG3BConfig config;
	private final Listeners<Listener> listeners = new Listeners(Listener.class);
	private final boolean[] buttonStates = new boolean[Button.values().length];
	private final float[] axisStates = new float[Axis.values().length];
	private final char[] buffer = new char[256];

	/**
	 * Creates a new PG3B with a timeout of 300.
	 * @param port The serial port to open.
	 */
	public PG3B (String port) throws IOException {
		this(port, 300);
	}

	/**
	 * @param port The serial port to open.
	 * @param timeout The number of millesconds to wait for the PG3B to respond both during initial connection and for each command
	 *           sent to it.
	 * @throws IOException When the PG3B could not be opened.
	 */
	public PG3B (String port, int timeout) throws IOException {
		if (port == null) throw new IllegalArgumentException("portID cannot be null.");
		if (port.length() == 0) throw new IllegalArgumentException("portID cannot be empty.");
		this.port = port;

		try {
			CommPortIdentifier identifier = CommPortIdentifier.getPortIdentifier(port);
			CommPort commPort = identifier.open("PG3B", timeout);
			if (!(commPort instanceof SerialPort)) throw new IOException("Port is not serial: " + port);

			serialPort = (SerialPort)commPort;
			serialPort.setSerialPortParams(230400, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
			serialPort.setDTR(true);
			serialPort.enableReceiveTimeout(timeout);
			serialPort.enableReceiveThreshold(0);

			Charset ascii = Charset.forName("ASCII");
			input = new BufferedReader(new InputStreamReader(serialPort.getInputStream(), ascii), 256);
			output = new OutputStreamWriter(serialPort.getOutputStream(), ascii);

			buffer[0] = 'X';
			buffer[1] = ' ';
			// 2, 3, 4, 5: sequence number.
			buffer[6] = ' ';
			// For sending... 7: command code, 8: space, 9+: command arguments.
			// For receiving... 7, 8: OK.

			config = new PG3BConfig(this);
		} catch (Exception ex) {
			close();
			throw new IOException("Error opening connection on port: " + port, ex);
		}
	}

	private synchronized byte[] primitive (int length) throws IOException {
		output.write(buffer, 0, length);
		output.flush();
		if (TRACE) trace("pg3b", "Sent: " + new String(buffer, 0, length - 1));

		buffer[7] = 'O';
		buffer[8] = 'K';
		String responsePrefix = new String(buffer, 0, 9);
		while (true) {
			String response = input.readLine();
			if (response == null) throw new IOException("Connection was closed.");
			if (TRACE) trace("pg3b", "Rcvd: " + response);
			if (response.startsWith(responsePrefix)) return hexStringToBytes(response, 10);
		}
	}

	synchronized byte[] command (Command command, byte[] commandArgument) throws IOException {
		int b = (sequenceNumber >> 8) & 0xFF;
		buffer[2] = hex[b / 16];
		buffer[3] = hex[b % 16];
		b = sequenceNumber & 0xFF;
		buffer[4] = hex[b / 16];
		buffer[5] = hex[b % 16];
		buffer[7] = command.code;
		buffer[8] = ' ';
		int c = 9;
		for (int i = 0, n = commandArgument.length; i < n; i++) {
			b = commandArgument[i] & 0xFF;
			buffer[c++] = hex[b / 16];
			buffer[c++] = hex[b % 16];
		}
		buffer[c++] = '\r';
		sequenceNumber++;
		return primitive(c);
	}

	synchronized byte[] command (Command command, int commandArgument) throws IOException {
		int b = (sequenceNumber >> 8) & 0xFF;
		buffer[2] = hex[b / 16];
		buffer[3] = hex[b % 16];
		b = sequenceNumber & 0xFF;
		buffer[4] = hex[b / 16];
		buffer[5] = hex[b % 16];
		buffer[7] = command.code;
		buffer[8] = ' ';
		b = (commandArgument >> 8) & 0xFF;
		buffer[9] = hex[b / 16];
		buffer[10] = hex[b % 16];
		b = commandArgument & 0xFF;
		buffer[11] = hex[b / 16];
		buffer[12] = hex[b % 16];
		buffer[13] = '\r';
		sequenceNumber++;
		return primitive(14);
	}

	private byte[] hexStringToBytes (String s, int start) {
		int length = s.length();
		int byteCount = (length - start) / 2;
		if (byteCount == 0) return null;
		byte[] bytes = new byte[byteCount];
		for (int i = start, ii = 0; i < length; i += 2, ii++)
			bytes[ii] = (byte)((charToDigit[s.charAt(i)] << 4) + charToDigit[s.charAt(i)]);
		return bytes;
	}

	private short getActionCode (short key, short value) {
		return (short)(key | value);
	}

	private short getActionKey (Device device, short target) {
		short d = (short)device.ordinal();
		return (short)(d << 12 | target << 8);
	}

	/**
	 * Returns true if the PG3B is connected and responding.
	 */
	public boolean isConnected () {
		try {
			command(Command.setDebugMessagesEnabled, 0);
			return true;
		} catch (IOException ignored) {
			return false;
		}
	}

	/**
	 * Sets the button state.
	 * @throws IOException When communication with the PG3B fails.
	 */
	public void set (Button button, boolean pressed) throws IOException {
		if (button == null) throw new IllegalArgumentException("button cannot be null.");

		int state = pressed ? 6 : 7;
		short actionKey = getActionKey(Device.xbox, (short)state);
		short actionCode = getActionCode(actionKey, (short)button.ordinal());
		synchronized (this) {
			command(Command.action, actionCode);
			buttonStates[button.ordinal()] = pressed;
		}
		if (DEBUG) debug("pg3b", "Button " + button + ": " + pressed);

		Listener[] listeners = this.listeners.toArray();
		for (int i = 0, n = listeners.length; i < n; i++)
			listeners[i].buttonChanged(button, pressed);
	}

	/**
	 * Sets the axis state.
	 * @throws IOException When communication with the PG3B fails.
	 */
	public void set (Axis axis, float state) throws IOException {
		if (axis == null) throw new IllegalArgumentException("axis cannot be null.");
		if (state < -1)
			state = -1;
		else if (state > 1) {
			state = 1;
		}

		float wiperValue;
		if (axis == Axis.leftTrigger || axis == Axis.rightTrigger)
			wiperValue = 255 - state * 255;
		else {
			wiperValue = (state + 1) * 127;
			if (axis != Axis.leftStickY && axis != Axis.rightStickY) wiperValue = 255 - wiperValue;
		}

		short actionKey = getActionKey(Device.xbox, (short)axis.ordinal());
		short actionCode = getActionCode(actionKey, (short)wiperValue);
		synchronized (this) {
			command(Command.action, actionCode);
			axisStates[axis.ordinal()] = state;
		}
		if (DEBUG) debug("pg3b", "Axis " + axis + ": " + state);

		Listener[] listeners = this.listeners.toArray();
		for (int i = 0, n = listeners.length; i < n; i++)
			listeners[i].axisChanged(axis, state);
	}

	/**
	 * Sets the button or axis state. If the target is an axis, it will be to 0 (false) or 1 (true).
	 * @throws IOException When communication with the PG3B fails.
	 */
	public void set (Target target, boolean pressed) throws IOException {
		if (target == null) throw new IllegalArgumentException("target cannot be null.");
		if (target instanceof Button)
			set((Button)target, pressed);
		else if (target instanceof Axis)
			set((Axis)target, pressed ? 1 : 0);
		else
			throw new IllegalArgumentException("target must be a button or axis.");
	}

	/**
	 * Sets the button or axis state. If the target is a button, it will be to not pressed (zero) or pressed (nonzero).
	 * @throws IOException When communication with the PG3B fails.
	 */
	public void set (Target target, float state) throws IOException {
		if (target == null) throw new IllegalArgumentException("target cannot be null.");
		if (target instanceof Button)
			set((Button)target, state != 0);
		else if (target instanceof Axis)
			set((Axis)target, state);
		else
			throw new IllegalArgumentException("target must be a button or axis.");
	}

	/**
	 * Sets the button or axis state. If the target is an axis, it will be to 0 (false) or 1 (true).
	 * @throws IOException When communication with the PG3B fails.
	 */
	public void set (String target, boolean pressed) throws IOException {
		if (target == null) throw new IllegalArgumentException("target cannot be null.");
		set(getTarget(target), pressed);
	}

	/**
	 * Sets the button or axis state. If the target is a button, it will be to not pressed (zero) or pressed (nonzero).
	 * @throws IOException When communication with the PG3B fails.
	 */
	public void set (String target, float state) throws IOException {
		if (target == null) throw new IllegalArgumentException("target cannot be null.");
		set(getTarget(target), state);
	}

	/**
	 * Sets the x and y axes for the specified stick.
	 * @throws IOException When communication with the PG3B fails.
	 */
	public void set (Stick stick, float stateX, float stateY) throws IOException {
		if (stick == null) throw new IllegalArgumentException("stick cannot be null.");
		Axis axisX = stick == Stick.left ? Axis.leftStickX : Axis.rightStickX;
		Axis axisY = stick == Stick.left ? Axis.leftStickY : Axis.rightStickY;
		set(axisX, stateX);
		set(axisY, stateY);
	}

	/**
	 * Sets the x and y axes for the specified stick.
	 * @throws IOException When communication with the PG3B fails.
	 */
	public void set (String stick, float stateX, float stateY) throws IOException {
		if (stick == null) throw new IllegalArgumentException("stick cannot be null.");
		stick = stick.toLowerCase();
		if (stick.equals("leftstick") || stick.equals("left") || stick.equals("l"))
			set(Stick.left, stateX, stateY);
		else if (stick.equals("rightstick") || stick.equals("right") || stick.equals("r"))
			set(Stick.right, stateX, stateY);
		else
			throw new IllegalArgumentException("stick must be leftStick or rightStick.");
	}

	/**
	 * Returns the last state of the button as set by the PG3B.
	 */
	public boolean get (Button button) {
		if (button == null) throw new IllegalArgumentException("button cannot be null.");
		return buttonStates[button.ordinal()];
	}

	/**
	 * Returns the last state of the axis as set by the PG3B.
	 */
	public float get (Axis axis) {
		if (axis == null) throw new IllegalArgumentException("axis cannot be null.");
		return axisStates[axis.ordinal()];
	}

	/**
	 * Returns the last state of the button or axis as set by the PG3B. If the taret is a button, either 0 (not pressed) or 1
	 * (pressed) is returned.
	 */
	public float get (Target target) {
		if (target == null) throw new IllegalArgumentException("target cannot be null.");
		if (target instanceof Button)
			return get((Button)target) ? 1 : 0;
		else if (target instanceof Axis)
			return get((Axis)target);
		else
			throw new IllegalArgumentException("target must be a button or axis.");
	}

	/**
	 * Returns the last state of the button or axis as set by the PG3B. If the taret is a button, either 0 (not pressed) or 1
	 * (pressed) is returned.
	 */
	public float get (String target) {
		return get(getTarget(target));
	}

	/**
	 * When disabled, the axis calibration tables in the PG3B's config are ignored.
	 */
	public void setCalibrationEnabled (boolean enabled) throws IOException {
		command(Command.setCalibrationEnabled, enabled ? 1 : 0);
	}

	/**
	 * Returns the configuration for this PG3B.
	 */
	public PG3BConfig getConfig () throws IOException {
		return config;
	}

	/**
	 * Returns the port where the PG3B is connected.
	 */
	public String getPort () {
		return port;
	}

	/**
	 * Closes the port for this PG3B. No further communication will be possible with this instance.
	 */
	public void close () {
		if (serialPort != null) serialPort.close();
	}

	/**
	 * Sets all buttons to released and all axes to zero.
	 */
	public void reset () throws IOException {
		for (Button button : Button.values())
			set(button, false);
		for (Axis axis : Axis.values())
			set(axis, 0);
	}

	/**
	 * Adds a listener to be notified when the PG3B manipulates a button or axis.
	 */
	public void addListener (Listener listener) {
		listeners.addListener(listener);
		if (TRACE) trace("pg3b", "PG3B listener added: " + listener.getClass().getName());
	}

	public void removeListener (Listener listener) {
		listeners.removeListener(listener);
		if (TRACE) trace("pg3b", "PG3B listener removed: " + listener.getClass().getName());
	}

	/**
	 * Returns the target with the specified name or alias (case insensitive).
	 */
	static public Target getTarget (String name) {
		if (name == null) throw new IllegalArgumentException("name cannot be null.");
		return nameToTarget.get(name.trim().toLowerCase());
	}

	/**
	 * Listener to be notified when the PG3B manipulates a button or axis.
	 */
	static public class Listener {
		public void buttonChanged (Button button, boolean pressed) {
		}

		public void axisChanged (Axis axis, float state) {
		}
	}
}
