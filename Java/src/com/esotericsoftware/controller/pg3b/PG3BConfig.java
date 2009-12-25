
package com.esotericsoftware.controller.pg3b;

import static com.esotericsoftware.minlog.Log.*;

import java.io.IOException;
import java.nio.charset.Charset;

import com.esotericsoftware.controller.device.Axis;

/**
 * Provides access to the configuration stored in the PG3B.
 */
public class PG3BConfig {
	static private final String MAGIC_NUMBER = "PG3B";
	static private final int INDEX_CRC = 0;
	static private final int INDEX_MAGIC = 1;
	static private final int INDEX_SIZE = 5;
	static private final int INDEX_VERSION = 6;
	static private final int INDEX_MODEL = 7;
	static private final int INDEX_CALIBRATION = 8;

	static private final int PAGE_BITS = 5;
	static private final int PAGE_SIZE = (1 << PAGE_BITS);
	static private final byte CONFIG_PAGE = 0;
	static private final int CALIBRATION_PAGES = 8;
	static private final int CALIBRATION_SIZE = CALIBRATION_PAGES * PAGE_SIZE;

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

	private final PG3B pg3b;
	private byte[] data;

	public PG3BConfig (PG3B pg3b) throws IOException {
		this.pg3b = pg3b;

		try {
			data = readPage(CONFIG_PAGE);
			String magicNumber = new String(data, INDEX_MAGIC, MAGIC_NUMBER.length(), Charset.forName("ASCII"));
			if (!magicNumber.equals(MAGIC_NUMBER)) throw new IOException("Invalid magic number for config page: " + magicNumber);
			byte crc = calculateCRC(data, INDEX_CRC + 1, data[INDEX_SIZE] - 1);
			if (crc != data[INDEX_CRC]) {
				throw new IOException("CRC check failed, page: 0, expected: " + Integer.toHexString(crc & 0xff) + ", actual: "
					+ Integer.toHexString(data[INDEX_CRC] & 0xff));
			}
			if (DEBUG) debug("PG3B config loaded, version: " + getVersion() + ", controller type: " + getControllerType());
		} catch (IOException ex) {
			// The device really shouldn't ever return an invalid config page.
			if (WARN) warn("Invalid PG3B config, resetting to defaults.", ex);
			data = new byte[32];
			System.arraycopy(MAGIC_NUMBER.getBytes("ASCII"), 0, data, INDEX_MAGIC, MAGIC_NUMBER.length());
			data[INDEX_SIZE] = (byte)data.length;
			save();
		}
	}

	/**
	 * Writes a page of configuration memory to the PG3B.
	 * @param pageData Must have a length of 32.
	 */
	void writePage (byte pageNumber, byte[] pageData) throws IOException {
		if (pageNumber < 0) throw new IllegalArgumentException("pageNumber cannot be <0: " + pageNumber);
		if (pageData == null) throw new IllegalArgumentException("pageData cannot be null.");
		if (pageData.length != 32) throw new IllegalArgumentException("pageData must be 32 bytes: " + pageData.length);

		byte[] writePage = new byte[pageData.length + 2];
		writePage[0] = pageNumber;
		System.arraycopy(pageData, 0, writePage, 1, pageData.length);
		writePage[writePage.length - 1] = calculateCRC(writePage, 1, writePage.length - 2);
		pg3b.command(Command.writePage, writePage);

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
	byte[] readPage (byte pageNumber) throws IOException {
		if (pageNumber < 0) throw new IllegalArgumentException("pageNumber cannot be <0: " + pageNumber);

		byte[] response = pg3b.command(Command.readPage, new byte[] {pageNumber});
		byte crc = calculateCRC(response, 0, response.length - 1);
		if (crc != response[response.length - 1]) {
			throw new IOException("CRC check failed, page: " + pageNumber + ", expected: " + Integer.toHexString(crc & 0xff)
				+ ", actual: " + Integer.toHexString(response[response.length - 1] & 0xff));
		}
		byte[] pageData = new byte[response.length - 1];
		System.arraycopy(response, 0, pageData, 0, pageData.length);
		return pageData;
	}

	byte calculateCRC (byte[] data, int start, int length) {
		int crc8 = 0xFF;
		for (int i = start; i < start + length; i++) {
			int index = crc8 ^ data[i];
			crc8 = crcTable[index & 0xFF];
		}
		return (byte)crc8;
	}

	public byte getVersion () {
		return data[INDEX_VERSION];
	}

	public void setVersion (byte version) {
		data[INDEX_VERSION] = version;
	}

	public ControllerType getControllerType () {
		ControllerType[] types = ControllerType.values();
		for (int i = 0; i < types.length; i++) {
			ControllerType type = types[i];
			if (type.code == data[INDEX_MODEL]) return type;
		}
		return null;
	}

	public void setControllerType (ControllerType type) {
		data[INDEX_MODEL] = type.code;
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

	public void setCalibrationTable (Axis axis, byte[] table) throws IOException {
		if (axis == null) throw new IllegalArgumentException("axis cannot be null.");
		if (table == null) throw new IllegalArgumentException("table cannot be null.");
		if (table.length != CALIBRATION_SIZE)
			throw new IllegalArgumentException("table must be " + CALIBRATION_SIZE + " bytes: " + table.length);

		byte[] pageData = new byte[PAGE_SIZE];
		int firstPage = axis.ordinal() * CALIBRATION_PAGES + 1;
		for (int i = 0; i < CALIBRATION_PAGES; i++) {
			System.arraycopy(table, i * PAGE_SIZE, pageData, 0, PAGE_SIZE);
			writePage((byte)(firstPage + i), pageData);
		}

		if (DEBUG) debug(axis + " calibration table written.");
	}

	public byte[] getCalibrationTable (Axis axis) throws IOException {
		if (axis == null) throw new IllegalArgumentException("axis cannot be null.");

		byte[] table = new byte[CALIBRATION_SIZE];
		int firstPage = axis.ordinal() * CALIBRATION_PAGES + 1;
		for (int i = 0; i < CALIBRATION_PAGES; i++) {
			byte[] pageData = readPage((byte)(firstPage + i));
			System.arraycopy(pageData, 0, table, i * PAGE_SIZE, PAGE_SIZE);
		}
		return table;
	}

	public void save () throws IOException {
		data[INDEX_CRC] = calculateCRC(data, INDEX_CRC + 1, data[INDEX_SIZE] - 1);
		writePage(CONFIG_PAGE, data);
		if (DEBUG) debug("PG3B config saved.");
	}
}
