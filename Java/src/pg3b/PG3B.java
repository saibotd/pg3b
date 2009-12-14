
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

	static private final int PAGE_BITS = 5;
	static private final int PAGE_SIZE = (1 << PAGE_BITS);

	static private final int[] crcTable = {
	// x^8 + x^2 + x^1 + x^0
		0x00, 0x07, 0x0E, 0x09, 0x1C, 0x1B, 0x12, 0x15, //
		0x38, 0x3F, 0x36, 0x31, 0x24, 0x23, 0x2A, 0x2D, //
		0x70, 0x77, 0x7E, 0x79, 0x6C, 0x6B, 0x62, 0x65, //
		0x48, 0x4F, 0x46, 0x41, 0x54, 0x53, 0x5A, 0x5D, //
		0xE0, 0xE7, 0xEE, 0xE9, 0xFC, 0xFB, 0xF2, 0xF5, //
		0xD8, 0xDF, 0xD6, 0xD1, 0xC4, 0xC3, 0xCA, 0xCD, //
		0x90, 0x97, 0x9E, 0x99, 0x8C, 0x8B, 0x82, 0x85, //
		0xA8, 0xAF, 0xA6, 0xA1, 0xB4, 0xB3, 0xBA, 0xBD, //
		0xC7, 0xC0, 0xC9, 0xCE, 0xDB, 0xDC, 0xD5, 0xD2, //
		0xFF, 0xF8, 0xF1, 0xF6, 0xE3, 0xE4, 0xED, 0xEA, //
		0xB7, 0xB0, 0xB9, 0xBE, 0xAB, 0xAC, 0xA5, 0xA2, //
		0x8F, 0x88, 0x81, 0x86, 0x93, 0x94, 0x9D, 0x9A, //
		0x27, 0x20, 0x29, 0x2E, 0x3B, 0x3C, 0x35, 0x32, //
		0x1F, 0x18, 0x11, 0x16, 0x03, 0x04, 0x0D, 0x0A, //
		0x57, 0x50, 0x59, 0x5E, 0x4B, 0x4C, 0x45, 0x42, //
		0x6F, 0x68, 0x61, 0x66, 0x73, 0x74, 0x7D, 0x7A, //
		0x89, 0x8E, 0x87, 0x80, 0x95, 0x92, 0x9B, 0x9C, //
		0xB1, 0xB6, 0xBF, 0xB8, 0xAD, 0xAA, 0xA3, 0xA4, //
		0xF9, 0xFE, 0xF7, 0xF0, 0xE5, 0xE2, 0xEB, 0xEC, //
		0xC1, 0xC6, 0xCF, 0xC8, 0xDD, 0xDA, 0xD3, 0xD4, //
		0x69, 0x6E, 0x67, 0x60, 0x75, 0x72, 0x7B, 0x7C, //
		0x51, 0x56, 0x5F, 0x58, 0x4D, 0x4A, 0x43, 0x44, //
		0x19, 0x1E, 0x17, 0x10, 0x05, 0x02, 0x0B, 0x0C, //
		0x21, 0x26, 0x2F, 0x28, 0x3D, 0x3A, 0x33, 0x34, //
		0x4E, 0x49, 0x40, 0x47, 0x52, 0x55, 0x5C, 0x5B, //
		0x76, 0x71, 0x78, 0x7F, 0x6A, 0x6D, 0x64, 0x63, //
		0x3E, 0x39, 0x30, 0x37, 0x22, 0x25, 0x2C, 0x2B, //
		0x06, 0x01, 0x08, 0x0F, 0x1A, 0x1D, 0x14, 0x13, //
		0xAE, 0xA9, 0xA0, 0xA7, 0xB2, 0xB5, 0xBC, 0xBB, //
		0x96, 0x91, 0x98, 0x9F, 0x8A, 0x8D, 0x84, 0x83, //
		0xDE, 0xD9, 0xD0, 0xD7, 0xC2, 0xC5, 0xCC, 0xCB, //
		0xE6, 0xE1, 0xE8, 0xEF, 0xFA, 0xFD, 0xF4, 0xF3 //
	};

	static private final char[] hex = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

	static private final Charset ascii = Charset.forName("ASCII");;

	private final SerialPort serialPort;
	private short sequenceNumber;
	private BufferedReader input;
	private OutputStreamWriter output;
	private final String port;
	private final Listeners<Listener> listeners = new Listeners(Listener.class);
	private final boolean[] buttonStates = new boolean[Button.values().length];
	private final float[] axisStates = new float[Axis.values().length];
	private final char[] buffer = new char[256];

	/**
	 * @param port The serial port to open.
	 */
	public PG3B (String port) throws IOException {
		this(port, 300);
	}

	/**
	 * @param port The serial port to open.
	 * @param timeout The amount of time to wait for the PG3B to respond both during initial connection and for each command sent
	 *           to it.
	 * @throws IOException When the PG3B could not be opened.
	 */
	public PG3B (String port, int timeout) throws IOException {
		if (port == null) throw new IllegalArgumentException("portID cannot be null.");
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

			input = new BufferedReader(new InputStreamReader(serialPort.getInputStream(), ascii), 256);
			output = new OutputStreamWriter(serialPort.getOutputStream(), ascii);

			buffer[0] = 'X';
			buffer[1] = ' ';
			// 2, 3, 4, 5: sequence number.
			buffer[6] = ' ';
			// For sending... 7: command code, 8: space, 9+: command arguments.
			// For receiving... 7, 8: OK.

			command(Command.setCalibrationEnabled, 0);
		} catch (Exception ex) {
			close();
			throw new IOException("Error opening connection on port: " + port, ex);
		}
	}

	private synchronized String primitive (int length) throws IOException {
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
			if (response.startsWith(responsePrefix)) return response;
		}
	}

	private synchronized byte[] command (Command command, byte[] commandArgument) throws IOException {
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
		String response = primitive(c);
		return hexStringToBytes(response, 10);
	}

	private synchronized byte[] command (Command command, int commandArgument) throws IOException {
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
		String response = primitive(14);
		return hexStringToBytes(response, 10);
	}

	private byte[] hexStringToBytes (String s, int start) {
		int length = s.length();
		if (length - start == 0) return null;
		byte[] bytes = new byte[(length - start) / 2];
		for (int i = start; i < length; i += 2)
			bytes[(i - start) / 2] = (byte)((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
		return bytes;
	}

	/**
	 * Writes a page of configuration memory to the PG3B.
	 */
	public void writePage (byte pageNumber, byte[] pageData) throws IOException {
		byte[] writePage = new byte[pageData.length + 2];
		writePage[0] = pageNumber;
		System.arraycopy(pageData, 0, writePage, 1, pageData.length);
		writePage[writePage.length - 1] = calculateCRC(writePage, 1, writePage.length - 2);
		command(Command.writePage, writePage);

		byte[] verifyPage = readPage(pageNumber);
		for (int i = 0; i < pageData.length; i++) {
			if (pageData[i] != verifyPage[i]) {
				throw new IOException("Failed to verify page, index: " + i + ", expected: " + pageData[i] + ", actual: "
					+ verifyPage[i]);
			}
		}
	}

	/**
	 * Reads a page of configuration memory from the PG3B.
	 */
	public byte[] readPage (byte pageNumber) throws IOException {
		byte[] response = command(Command.readPage, new byte[] {pageNumber});
		byte crc = calculateCRC(response, 0, response.length - 1);
		if (crc != response[response.length - 1]) {
			throw new IOException("CRC check failed, page: " + pageNumber + ", expected: " + Integer.toHexString(crc & 0xff)
				+ ", actual: " + Integer.toHexString(response[response.length - 1] & 0xff));
		}
		byte[] pageData = new byte[response.length - 1];
		System.arraycopy(response, 0, pageData, 0, pageData.length);
		return pageData;
	}

	private byte calculateCRC (byte[] data, int start, int length) {
		int crc8 = 0xFF;
		for (int i = start; i < start + length; i++) {
			int index = crc8 ^ data[i];
			crc8 = crcTable[index & 0xFF];
		}
		return (byte)crc8;
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
	 * Sets the calibration table for the specified axis.
	 */
	public void setCalibrationTable (Axis axis, byte[] table) throws IOException {
		byte[] pageData = new byte[PAGE_SIZE];
		int pagesPerRecord = table.length / PAGE_SIZE;
		int firstPage = axis.ordinal() * pagesPerRecord + 1;
		for (int pageCount = 0; pageCount < table.length / PAGE_SIZE; pageCount++) {
			for (int pageOffset = 0; pageOffset < PAGE_SIZE; pageOffset++)
				pageData[pageOffset] = table[pageCount * PAGE_SIZE + pageOffset];
			writePage((byte)(firstPage + pageCount), pageData);
		}

		Config config = getConfig();
		config.setCalibrated(axis, true);
		config.save();
	}

	/**
	 * Returns the configuration metadata for this PG3B.
	 */
	public Config getConfig () throws IOException {
		Config config = new Config();
		try {
			config.load();
		} catch (IOException ex) {
			if (WARN) warn("Invalid config, creating new config.", ex);
			config.save();
		}
		return config;
	}

	public void setControllerType (ControllerType type) throws IOException {
		command(Command.setIsWireless, type == ControllerType.wired ? 1 : 0);
		Config config = getConfig();
		config.setModel((byte)1);
		config.save();
	}

	/**
	 * Returns the port where the PG3B is connected.
	 */
	public String getPort () {
		return port;
	}

	/**
	 * Closes the port for this PG3B. No further communication with the PG3B will be possible with this instance.
	 */
	public void close () {
		if (serialPort != null) serialPort.close();
	}

	/**
	 * Sets all buttons to not pressed and all axes to zero.
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
		if (TRACE) trace("pg3b", "XboxController listener added: " + listener.getClass().getName());
	}

	public void removeListener (Listener listener) {
		listeners.removeListener(listener);
		if (TRACE) trace("pg3b", "XboxController listener removed: " + listener.getClass().getName());
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
		readPage('R'), //
		writePage('W');

		char code;

		private Command (char code) {
			this.code = code;
		}
	}

	public class Config {
		static private final String MAGIC_NUMBER = "PG3B";
		static private final int INDEX_CRC = 0;
		static private final int INDEX_MAGIC = 1;
		static private final int INDEX_SIZE = 5;
		static private final int INDEX_VERSION = 6;
		static private final int INDEX_MODEL = 7;
		static private final int INDEX_CALIBRATION = 8;

		private byte[] data = new byte[9];

		public Config () throws IOException {
			System.arraycopy(MAGIC_NUMBER.getBytes(ascii), 0, data, INDEX_MAGIC, 4);
			data[INDEX_SIZE] = (byte)data.length;
			data[INDEX_CRC] = calculateCRC(data, INDEX_CRC + 1, data[INDEX_SIZE] - 1);
		}

		public byte getVersion () {
			return data[INDEX_VERSION];
		}

		public void setVersion (byte version) {
			data[INDEX_VERSION] = version;
		}

		public byte getModel () {
			return data[INDEX_MODEL];
		}

		public void setModel (byte model) {
			data[INDEX_MODEL] = model;
		}

		public boolean isCalibrated (Axis axis) {
			int flag = 1 << axis.ordinal();
			return (data[INDEX_CALIBRATION] & flag) == flag;
		}

		public void setCalibrated (Axis axis, boolean calibrated) {
			int flag = 1 << axis.ordinal();
			if (calibrated)
				data[INDEX_CALIBRATION] |= flag;
			else
				data[INDEX_CALIBRATION] &= ~flag;
		}

		public void load () throws IOException {
			byte[] data = readPage((byte)0);
			String magicNumber = new String(data, INDEX_MAGIC, 4, Charset.forName("ASCII"));
			if (!magicNumber.equals(MAGIC_NUMBER)) throw new IOException("Invalid magic number for config page: " + magicNumber);
			byte crc = calculateCRC(data, INDEX_CRC + 1, data[INDEX_SIZE] - 1);
			if (crc != data[INDEX_CRC]) {
				throw new IOException("CRC check failed, page: 0, expected: " + Integer.toHexString(crc & 0xff) + ", actual: "
					+ Integer.toHexString(data[INDEX_CRC] & 0xff));
			}
			this.data = data;
		}

		public void save () throws IOException {
			writePage((byte)0, data);
			if (DEBUG) debug("Saved config.");
		}
	}
}
