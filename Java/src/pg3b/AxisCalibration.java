
package pg3b;

import java.net.MalformedURLException;
import java.net.URL;

public class AxisCalibration {
	private final int[] calibrationTable;
	private final float[] actualValues;
	private final boolean isInverted;

	public AxisCalibration (int[] calibrationTable, float[] actualValues, boolean isInverted) {
		this.calibrationTable = calibrationTable;
		this.actualValues = actualValues;
		this.isInverted = isInverted;
	}

	public URL getChartURL () {
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
		try {
			return new URL("http://chart.apis.google.com/chart?chs=640x320&chf=bg,s,ffffff|c,s,ffffff&chxt=x,y&"
				+ "chxl=0:|0|63|127|191|255|1:|-1|0|1&cht=lc&chdl=Calibrated|Raw&chco=0000ff,ff0000&chdlp=b&chd=t:" + calibrated
				+ "|" + raw);
		} catch (MalformedURLException ex) {
			throw new RuntimeException(ex);
		}
	}
}
