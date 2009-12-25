
package com.esotericsoftware.controller.pg3b;

import static com.esotericsoftware.minlog.Log.*;
import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import com.esotericsoftware.controller.device.Axis;
import com.esotericsoftware.controller.device.Button;
import com.esotericsoftware.controller.device.Device;

/**
 * Controls the PG3B hardware.
 */
public class PG3B extends Device {
	static private final char[] hex = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
	static private final int[] charToDigit = new int[103];
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
	final SerialPort serialPort;
	private short sequenceNumber;
	private BufferedReader input;
	private OutputStreamWriter output;
	private final PG3BConfig config;
	private final char[] buffer = new char[256];
	private boolean debugEnabled, calibrationEnabled;

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
			// 2, 3, 4, 5: sequence number
			buffer[6] = ' ';
			// For sending... 7: command code, 8: space, 9+: command arguments
			// For receiving... 7, 8: OK

			if (INFO) info("Connected to PG3B on port: " + port);

			setDebugEnabled(false);
			setCalibrationEnabled(true);

			config = new PG3BConfig(this);
		} catch (Exception ex) {
			close();
			throw new IOException("Error opening connection on port: " + port, ex);
		}
	}

	private synchronized byte[] primitive (int length) throws IOException {
		output.write(buffer, 0, length);
		output.flush();
		if (TRACE) trace("Sent: " + new String(buffer, 0, length - 1));

		buffer[7] = 'O';
		buffer[8] = 'K';
		String responsePrefix = new String(buffer, 0, 9);
		while (true) {
			String response = input.readLine();
			if (response == null) throw new IOException("Connection was closed.");
			if (response.startsWith(responsePrefix)) {
				if (debugEnabled) {
					response = response.substring(9);
					while (true) {
						char c = response.length() == 0 ? '\n' : response.charAt(0);
						if (c == '\n') {
							if (TRACE) trace("Received: " + responsePrefix);
							return new byte[0];
						}
						if (c == ' ') {
							if (TRACE) trace("Received: " + responsePrefix + response);
							return hexStringToBytes(response, 1);
						}
						if (TRACE) trace("Debug: " + response);
						response = input.readLine();
						if (response == null) throw new IOException("Connection was closed.");
					}
				} else if (TRACE) {
					trace("Received: " + response);
				}
				return hexStringToBytes(response, 10);
			}
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

	synchronized byte[] commandWord (Command command, int commandArgument) throws IOException {
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

	synchronized byte[] commandByte (Command command, int commandArgument) throws IOException {
		int b = (sequenceNumber >> 8) & 0xFF;
		buffer[2] = hex[b / 16];
		buffer[3] = hex[b % 16];
		b = sequenceNumber & 0xFF;
		buffer[4] = hex[b / 16];
		buffer[5] = hex[b % 16];
		buffer[7] = command.code;
		buffer[8] = ' ';
		b = commandArgument & 0xFF;
		buffer[9] = hex[b / 16];
		buffer[10] = hex[b % 16];
		buffer[11] = '\r';
		sequenceNumber++;
		return primitive(12);
	}

	private byte[] hexStringToBytes (String s, int start) {
		int length = s.length();
		int byteCount = (length - start) / 2;
		if (byteCount == 0) return null;
		byte[] bytes = new byte[byteCount];
		for (int i = start, ii = 0; i < length; i += 2, ii++)
			bytes[ii] = (byte)((charToDigit[s.charAt(i)] << 4) + charToDigit[s.charAt(i + 1)]);
		return bytes;
	}

	short getActionCode (short key, short value) {
		return (short)(key | value);
	}

	short getActionKey (ActionDevice device, short target) {
		short d = (short)device.ordinal();
		return (short)(d << 12 | target << 8);
	}

	public void set (Button button, boolean pressed) throws IOException {
		if (button == null) throw new IllegalArgumentException("button cannot be null.");

		int state = pressed ? 6 : 7;
		short actionKey = getActionKey(ActionDevice.xbox, (short)state);
		short actionCode = getActionCode(actionKey, (short)button.ordinal());
		synchronized (this) {
			commandWord(Command.action, actionCode);
			buttonStates[button.ordinal()] = pressed;
		}
		if (DEBUG) debug(button + ": " + pressed);

		notifyButtonChanged(button, pressed);
	}

	public void set (Axis axis, float state) throws IOException {
		if (axis == null) throw new IllegalArgumentException("axis cannot be null.");
		float originalState = state;
		state = getDeflection(axis, state);

		float wiperValue;
		if (axis == Axis.leftTrigger || axis == Axis.rightTrigger)
			wiperValue = 255 - state * 255;
		else {
			wiperValue = (state + 1) * 127;
			if (axis != Axis.leftStickY && axis != Axis.rightStickY) wiperValue = 255 - wiperValue;
		}

		short actionKey = getActionKey(ActionDevice.xbox, (short)axis.ordinal());
		short actionCode = getActionCode(actionKey, (short)wiperValue);
		synchronized (this) {
			commandWord(Command.action, actionCode);
			axisStates[axis.ordinal()] = originalState;
		}
		if (DEBUG) debug(axis + ": " + state);

		notifyAxisChanged(axis, state);
	}

	public void close () {
		if (serialPort != null) serialPort.close();
	}

	/**
	 * Returns true if the PG3B is connected and responding.
	 */
	public boolean isConnected () {
		try {
			commandByte(Command.setDebugMessagesEnabled, debugEnabled ? 1 : 0);
			return true;
		} catch (IOException ignored) {
			return false;
		}
	}

	/**
	 * When disabled, the axis calibration tables in the PG3B's config are ignored.
	 */
	public void setCalibrationEnabled (boolean enabled) throws IOException {
		calibrationEnabled = enabled;
		commandByte(Command.setCalibrationEnabled, enabled ? 1 : 0);
		if (DEBUG) debug("Calibration enabled set to: " + enabled);
	}

	public boolean isCalibrationEnabled () {
		return calibrationEnabled;
	}

	/**
	 * When true, the PG3B will send back extra debug messages that are logged at the TRACE level.
	 */
	public void setDebugEnabled (boolean enabled) throws IOException {
		debugEnabled = enabled;
		commandByte(Command.setDebugMessagesEnabled, enabled ? 1 : 0);
		if (DEBUG) debug("Debug enabled set to: " + enabled);
	}

	public boolean isDebugEnabled () {
		return debugEnabled;
	}

	/**
	 * Returns the configuration for this PG3B.
	 */
	public PG3BConfig getConfig () {
		return config;
	}

	/**
	 * Returns the port where the PG3B is connected.
	 */
	public String getPort () {
		return port;
	}

	public String toString () {
		return "PG3B (" + port + ")";
	}
}
