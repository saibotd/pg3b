
package com.esotericsoftware.controller.ui.swing;

import static com.esotericsoftware.minlog.Log.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

import com.esotericsoftware.controller.device.Axis;
import com.esotericsoftware.controller.input.XboxController;
import com.esotericsoftware.controller.pg3b.PG3B;
import com.esotericsoftware.controller.pg3b.PG3BConfig;
import com.esotericsoftware.controller.ui.Diagnostics;
import com.esotericsoftware.controller.util.LoaderDialog;
import com.esotericsoftware.controller.util.Util;
import com.esotericsoftware.minlog.Log;

public class PG3BCalibrationDialog extends JDialog {
	static private List<AxisCalibration> calibrations = new ArrayList();
	static {
		for (Axis axis : Axis.values())
			calibrations.add(new AxisCalibration(axis));
	}

	private final PG3B pg3b;
	private final XboxController controller;

	private JTable table;
	private JLabel imageLabel;
	private JButton closeButton, calibrateButton;
	private DefaultTableModel tableModel;
	private JButton refreshButton;

	public PG3BCalibrationDialog (UI owner, PG3B pg3b, XboxController controller) {
		super(owner, "Axes Calibration", true);

		this.pg3b = pg3b;
		this.controller = controller;

		initializeLayout();
		initializeEvents();

		populateTable();

		pack();
		setLocationRelativeTo(owner);

		if (calibrations.get(calibrations.size() - 1).rawValues == null)
			readRawValues();
		else
			calibrateButton.setEnabled(true);

		setVisible(true);
	}

	private void populateTable () {
		int selectedRow = table.getSelectedRow();
		if (selectedRow == -1) selectedRow = 0;

		tableModel.setRowCount(0);
		try {
			PG3BConfig config = pg3b.getConfig();
			for (Axis axis : Axis.values()) {
				boolean isCalibrated = config.isCalibrated(axis);
				AxisCalibration calibration = calibrations.get(axis.ordinal());
				if (isCalibrated)
					calibration.calibrationTable = config.getCalibrationTable(axis);
				else
					calibration.calibrationTable = null;
				tableModel.addRow(new Object[] {axis, isCalibrated ? "Yes" : "No"});
			}
		} catch (IOException ex) {
			if (Log.ERROR) error("Error reading axis calibrations.", ex);
			Util.errorDialog(this, "Error", "An error occurred while reading the calibration data from the PG3B.");
			dispose();
			return;
		}

		table.getSelectionModel().setSelectionInterval(selectedRow, selectedRow);
	}

	private void readRawValues () {
		new LoaderDialog("Axes Calibration") {
			public void load () throws Exception {
				int i = 0, count = Axis.values().length;
				for (AxisCalibration calibration : calibrations) {
					throwCancelled();
					Axis axis = calibration.axis;
					setMessage("Reading " + axis + "...");
					calibration.rawValues = Diagnostics.getRawValues(axis, pg3b, controller);
					setPercentageComplete(++i / (float)count);
					if (DEBUG) debug(axis + " chart:\n" + calibration.getChartURL());
				}
			}

			public void complete () {
				if (cancelled) return;
				if (failed()) {
					Util.errorDialog(PG3BCalibrationDialog.this, "Error", "An error occurred while reading the controller values.");
					return;
				}
				calibrateButton.setEnabled(true);
				int selectedRow = table.getSelectedRow();
				if (selectedRow != -1) {
					table.clearSelection();
					table.getSelectionModel().setSelectionInterval(selectedRow, selectedRow);
				}
			}
		}.start("ReadRawValues");
	}

	private void initializeEvents () {
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged (ListSelectionEvent event) {
				if (event.getValueIsAdjusting()) return;
				int selectedRow = table.getSelectedRow();
				if (selectedRow == -1) return;
				imageLabel.setIcon(new ImageIcon(calibrations.get(selectedRow).getChartURL()));
			}
		});

		closeButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				dispose();
			}
		});

		refreshButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				readRawValues();
			}
		});

		calibrateButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				new LoaderDialog("Axes Calibration") {
					public void load () throws Exception {
						PG3BConfig config = pg3b.getConfig();
						int i = 0, count = Axis.values().length;
						for (AxisCalibration calibration : calibrations) {
							Axis axis = calibration.axis;
							setMessage("Calibrating " + axis + "...");
							calibration.calibrationTable = Diagnostics.getCalibrationTable(axis, calibration.rawValues);
							config.setCalibrationTable(axis, calibration.calibrationTable);
							config.setCalibrated(axis, true);
							setPercentageComplete(++i / (float)count);
							if (DEBUG) debug(axis + " chart:\n" + calibration.getChartURL());
						}
						config.save();
						pg3b.reset();

						populateTable();
					}

					public void complete () {
						if (cancelled) return;
						if (failed()) {
							Util.errorDialog(PG3BCalibrationDialog.this, "Error",
								"An error occurred while saving the calibration values.");
							return;
						}
						int selectedRow = table.getSelectedRow();
						if (selectedRow != -1) {
							table.clearSelection();
							table.getSelectionModel().setSelectionInterval(selectedRow, selectedRow);
						}
					}
				}.start("SaveCalibration");
			}
		});
	}

	private void initializeLayout () {
		setResizable(false);
		setIconImage(new ImageIcon(getClass().getResource("/chart.png")).getImage());
		getContentPane().setLayout(new GridBagLayout());
		{
			JPanel panel = new JPanel(new GridBagLayout());
			getContentPane().add(
				panel,
				new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0,
					0), 0, 0));
			{
				JScrollPane scroll = new JScrollPane();
				panel.add(scroll, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
					new Insets(6, 0, 6, 0), 0, 0));
				{
					JPanel tablePanel = new JPanel(new BorderLayout()) {
						public Dimension getPreferredSize () {
							Dimension size = super.getPreferredSize();
							size.width = 200;
							return size;
						}
					};
					scroll.setViewportView(tablePanel);
					{
						table = new JTable() {
							public boolean isCellEditable (int row, int column) {
								return false;
							}
						};
						tablePanel.add(table, BorderLayout.CENTER);
						tablePanel.add(table.getTableHeader(), BorderLayout.NORTH);
						table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
						table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
							public Component getTableCellRendererComponent (JTable table, Object value, boolean isSelected,
								boolean hasFocus, int row, int column) {
								hasFocus = false; // Disable cell focus.
								JLabel label = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
									column);
								label.setBorder(new EmptyBorder(new Insets(0, 4, 0, 0))); // Padding.
								label.setForeground(isSelected ? table.getSelectionForeground() : null);
								return label;
							}
						});
						tableModel = new DefaultTableModel();
						tableModel.addColumn("Axis");
						tableModel.addColumn("Calibrated");
						table.setModel(tableModel);
						TableColumnModel columnModel = table.getColumnModel();
						columnModel.getColumn(0).setPreferredWidth(660);
						columnModel.getColumn(1).setPreferredWidth(340);
					}
				}
			}
			{
				JPanel imagePanel = new JPanel(new GridBagLayout());
				panel.add(imagePanel, new GridBagConstraints(0, 1, 1, 1, 0.0, 1.0, GridBagConstraints.CENTER,
					GridBagConstraints.NONE, new Insets(0, 6, 0, 6), 0, 0));
				imagePanel.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.black));
				imagePanel.setBackground(Color.white);
				{
					imageLabel = new JLabel();
					imagePanel.add(imageLabel, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
						GridBagConstraints.BOTH, new Insets(6, 6, 6, 6), 0, 0));
					imageLabel.setPreferredSize(new Dimension(640, 320));
				}
			}
		}
		{
			JPanel panel = new JPanel(new GridBagLayout());
			getContentPane().add(
				panel,
				new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(6, 6, 6,
					6), 0, 0));
			{
				JPanel leftPanel = new JPanel(new GridLayout(1, 1, 6, 6));
				panel.add(leftPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(0, 0, 0, 0), 0, 0));
				{
					refreshButton = new JButton("Refresh");
					leftPanel.add(refreshButton);
				}
				{
					calibrateButton = new JButton("Calibrate");
					leftPanel.add(calibrateButton);
					calibrateButton.setEnabled(false);
				}
			}
			{
				JPanel rightPanel = new JPanel(new GridLayout(1, 1, 6, 6));
				panel.add(rightPanel, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(0, 0, 0, 0), 0, 0));
				{
					closeButton = new JButton("Close");
					rightPanel.add(closeButton);
				}
			}
		}
	}

	static private class AxisCalibration {
		public final Axis axis;
		public byte[] calibrationTable;
		public float[] rawValues;

		public AxisCalibration (Axis axis) {
			this.axis = axis;
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
						+ "chxl=0:|0|63|127|191|255|1:|-1|0|1&cht=lc&chdl=Raw|Calibrated&chco=ff0000,0000ff&chdlp=b&chd=t:" + raw + "|"
						+ calibrated);
				}
			} catch (MalformedURLException ex) {
				throw new RuntimeException(ex);
			}
		}

		public String toString () {
			return axis.toString();
		}
	}
}
