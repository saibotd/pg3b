
package pg3b.ui;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

public class CalibrationResultsFrame extends JFrame {
	static private CalibrationResultsFrame openFrame;

	Map<String, String> nameToURL;
	JComboBox chartCombo;
	DefaultComboBoxModel chartComboModel;
	JLabel imageLabel;

	public CalibrationResultsFrame (Map<String, String> nameToURL) {
		super("PG3B - Calibration Results");
		this.nameToURL = nameToURL;

		initializeLayout();
		initializeEvents();

		for (String name : nameToURL.keySet())
			chartComboModel.addElement(name);

		close();
		openFrame = this;
	}

	private void initializeEvents () {
		chartCombo.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				try {
					imageLabel.setIcon(new ImageIcon(new URL(nameToURL.get(chartCombo.getSelectedItem()))));
				} catch (MalformedURLException ex) {
					JOptionPane.showMessageDialog(CalibrationResultsFrame.this, "An error occurred downloading the chart image.");
				}
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
