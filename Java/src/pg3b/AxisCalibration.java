
package pg3b;

import java.net.MalformedURLException;
import java.net.URL;

// BOZO - Move out of root.

/**
 * Stores calibration information about a PG3B axis.
 */
public class AxisCalibration {
	private final Axis axis;
	private byte[] calibrationTable;
	private float[] rawValues;

	public AxisCalibration (Axis axis) {
		this.axis = axis;
	}

	public byte[] getCalibrationTable () {
		return calibrationTable;
	}

	public void setCalibrationTable (byte[] calibrationTable) {
		this.calibrationTable = calibrationTable;
	}

	public float[] getRawValues () {
		return rawValues;
	}

	public void setRawValues (float[] rawValues) {
		this.rawValues = rawValues;
	}

	public Axis getAxis () {
		return axis;
	}

	/**
	 * Returns a URL to a chart image that shows the actual and calibrated axis values.
	 */
	public URL getChartURL () {
		try {
			if (rawValues == null) {
				return new URL("http://chart.apis.google.com/chart?chs=640x320&chf=bg,s,ffffff|c,s,ffffff&chxt=x,y&"
					+ "chxl=0:|0|63|127|191|255|1:|-1|0|1&cht=lc&chdl=Raw|Calibrated&chco=ff0000,0000ff&chdlp=b&chd=t:|");
			}
			StringBuilder raw = new StringBuilder(1024);
			StringBuilder calibrated = new StringBuilder(1024);
			for (int wiper = 0; wiper <= 255; wiper += 2) {
				raw.append((int)(rawValues[wiper] * 100 + 100) / 2);
				raw.append(",");
				if (calibrationTable != null) {
					int index = calibrationTable[wiper] & 0xFF;
					calibrated.append((int)(rawValues[index] * 100 + 100) / 2);
					calibrated.append(",");
				}
			}
			raw.setLength(raw.length() - 1);
			if (calibrationTable == null) {
				return new URL("http://chart.apis.google.com/chart?chs=640x320&chf=bg,s,ffffff|c,s,ffffff&chxt=x,y&"
					+ "chxl=0:|0|63|127|191|255|1:|-1|0|1&cht=lc&chdl=Raw|Calibrated&chco=ff0000,0000ff&chdlp=b&chd=t:" + raw);
			} else {
				calibrated.setLength(calibrated.length() - 1);
				return new URL("http://chart.apis.google.com/chart?chs=640x320&chf=bg,s,ffffff|c,s,ffffff&chxt=x,y&"
					+ "chxl=0:|0|63|127|191|255|1:|-1|0|1&cht=lc&chdl=Raw|Calibrated&chco=ff0000,0000ff&chdlp=b&chd=t:" + raw
					+ "|" + calibrated);
			}
		} catch (MalformedURLException ex) {
			throw new RuntimeException(ex);
		}
	}

	public String toString () {
		return axis.toString();
	}
}
