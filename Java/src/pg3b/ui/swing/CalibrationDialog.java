
package pg3b.ui.swing;

import static com.esotericsoftware.minlog.Log.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
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

import pg3b.Axis;
import pg3b.AxisCalibration;
import pg3b.Diagnostics;
import pg3b.PG3B;
import pg3b.PG3BConfig;
import pg3b.input.XboxController;
import pg3b.util.LoaderDialog;
import pg3b.util.UI;

import com.esotericsoftware.minlog.Log;

public class CalibrationDialog extends JDialog {
	private final PG3B pg3b;
	private final XboxController controller;
	private List<AxisCalibration> calibrations = new ArrayList();

	private JTable table;
	private JLabel imageLabel;
	private JButton closeButton, calibrateButton;
	private DefaultTableModel tableModel;
	private JButton refreshButton;

	public CalibrationDialog (Frame owner, PG3B pg3b, XboxController controller) {
		super(owner, "Axes Calibration", true);

		this.pg3b = pg3b;
		this.controller = controller;

		initializeLayout();
		initializeEvents();

		populateTable();

		pack();
		setLocationRelativeTo(owner);

		readRawValues();

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
				AxisCalibration calibration = new AxisCalibration(axis);
				if (isCalibrated) calibration.setCalibrationTable(config.getCalibrationTable(axis));
				calibrations.add(calibration);
				tableModel.addRow(new Object[] {axis, isCalibrated ? "Yes" : "No"});
			}
		} catch (IOException ex) {
			if (Log.ERROR) error("Error reading axis calibrations.", ex);
			UI.errorDialog(this, "Error", "An error occurred while reading the calibration data from the PG3B.");
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
					Axis axis = calibration.getAxis();
					setMessage("Reading " + axis + "...");
					calibration.setRawValues(Diagnostics.getRawValues(axis, pg3b, controller));
					setPercentageComplete(i / (float)count);
					if (INFO) info(calibration.getAxis() + " chart:\n" + calibration.getChartURL());
				}
			}

			public void complete () {
				if (cancelled) return;
				if (failed()) {
					UI.errorDialog(CalibrationDialog.this, "Error", "An error occurred while reading the controller values.");
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
							Axis axis = calibration.getAxis();
							setMessage("Calibrating " + axis + "...");
							calibration.setCalibrationTable(Diagnostics.getCalibrationTable(axis, calibration.getRawValues()));
							config.setCalibrationTable(axis, calibration.getCalibrationTable());
							config.setCalibrated(axis, true);
							setPercentageComplete(++i / (float)count);
							if (INFO) info(calibration.getAxis() + " chart:\n" + calibration.getChartURL());
						}
						config.save();

						populateTable();
					}

					public void complete () {
						if (cancelled) return;
						if (failed()) {
							UI.errorDialog(CalibrationDialog.this, "Error", "An error occurred while saving the calibration values.");
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
}
