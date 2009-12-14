
package pg3b.ui.swing;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;

import pg3b.AxisCalibration;

public class CalibrationResultsFrame extends JFrame {
	static private CalibrationResultsFrame openFrame;

	private List<AxisCalibration> results;
	private JComboBox chartCombo;
	private DefaultComboBoxModel chartComboModel;
	private JLabel imageLabel;

	public CalibrationResultsFrame (List<AxisCalibration> results) {
		super("PG3B - Calibration Results");
		this.results = results;

		initializeLayout();
		initializeEvents();

		for (AxisCalibration calibration : results)
			chartComboModel.addElement(calibration);

		close();
		openFrame = this;
	}

	private void initializeEvents () {
		chartCombo.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				AxisCalibration calibration = (AxisCalibration)chartCombo.getSelectedItem();
				imageLabel.setIcon(new ImageIcon(calibration.getChartURL()));
			}
		});
	}

	private void initializeLayout () {
		setSize(664, 405);
		setResizable(false);
		setIconImage(new ImageIcon(getClass().getResource("/chart.png")).getImage());
		getContentPane().setBackground(Color.white);
		getContentPane().setLayout(new GridBagLayout());
		{
			chartComboModel = new DefaultComboBoxModel();
			chartCombo = new JComboBox();
			getContentPane().add(
				chartCombo,
				new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(6, 6, 6,
					6), 0, 0));
			chartCombo.setModel(chartComboModel);
		}
		{
			imageLabel = new JLabel();
			getContentPane().add(
				imageLabel,
				new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 6, 6,
					6), 0, 0));
		}
	}

	static public void close () {
		if (openFrame != null) openFrame.dispose();
	}
}
