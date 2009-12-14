
package pg3b;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Stores calibration information about a PG3B axis.
 */
public class AxisCalibration {
	private final Axis axis;
	private final byte[] calibrationTable;
	private final float[] actualValues;

	public AxisCalibration (Axis axis, byte[] calibrationTable, float[] actualValues) {
		this.axis = axis;
		this.calibrationTable = calibrationTable;
		this.actualValues = actualValues;
	}

	public Axis getAxis () {
		return axis;
	}

	public byte[] getTable () {
		return calibrationTable;
	}

	/**
	 * Returns a URL to a chart image that shows the actual and calibrated axis values.
	 */
	public URL getChartURL () {
		StringBuilder raw = new StringBuilder(1024);
		StringBuilder calibrated = new StringBuilder(1024);
		for (int wiper = 0; wiper <= 255; wiper += 2) {
			raw.append((int)(actualValues[wiper] * 100 + 100) / 2);
			raw.append(",");
			int index = calibrationTable[wiper] & 0xFF;
			calibrated.append((int)(actualValues[index] * 100 + 100) / 2);
			calibrated.append(",");
		}
		raw.setLength(raw.length() - 1);
		calibrated.setLength(calibrated.length() - 1);
		try {
			return new URL("http://chart.apis.google.com/chart?chs=640x320&chf=bg,s,ffffff|c,s,ffffff&chxt=x,y&"
				+ "chxl=0:|0|63|127|191|255|1:|-1|0|1&cht=lc&chdl=Calibrated|Raw&chco=0000ff,ff0000&chdlp=b&chd=t:" + calibrated
				+ "|" + raw);
		} catch (MalformedURLException ex) {
			throw new RuntimeException(ex);
		}
	}

	public String toString () {
		return axis.toString();
	}
}
