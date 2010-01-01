
package com.esotericsoftware.controller.ui.swing;

import static com.esotericsoftware.minlog.Log.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

import com.esotericsoftware.controller.device.Device;
import com.esotericsoftware.controller.ui.Action;
import com.esotericsoftware.controller.ui.Config;
import com.esotericsoftware.controller.ui.InputTrigger;
import com.esotericsoftware.controller.ui.Script;
import com.esotericsoftware.controller.ui.ScriptAction;
import com.esotericsoftware.controller.ui.Settings;
import com.esotericsoftware.controller.ui.Trigger;
import com.esotericsoftware.controller.util.FileChooser;
import com.esotericsoftware.controller.util.Util;
import com.esotericsoftware.minlog.Log;

public class ConfigEditor extends EditorPanel<Config> {
	private int lastSelectedTriggerIndex;

	private JTable triggersTable;
	private DefaultTableModel triggersTableModel;
	private JButton newTriggerButton, deleteTriggerButton, editTriggerButton;
	private JButton deadzonesButton, mouseButton;
	private JToggleButton activateButton;

	private Device device;

	public ConfigEditor (UI owner) {
		super(owner, Config.class, new File("config"), ".config");

		initializeLayout();
		initializeEvents();

		List<Config> items = getItems();
		for (Config config : items) {
			if (config.getName().equals(settings.selectedConfig)) {
				setSelectedItem(config);
				break;
			}
		}
		if (getSelectedItem() == null && items.size() > 0) setSelectedItem(items.get(0));
	}

	protected void updateFieldsFromItem (Config config) {
		triggersTableModel.setRowCount(0);
		if (config == null) {
			owner.updateActiveConfig();
		} else {
			for (Trigger trigger : config.getTriggers())
				triggersTableModel.addRow(new Object[] {trigger.getName(), trigger, trigger.getAction()});
			setSelectedTrigger(lastSelectedTriggerIndex);

			if (!config.getName().equals(settings.selectedConfig)) {
				settings.selectedConfig = config.getName();
				Settings.save();
			}
		}
		activateButton.setEnabled(config != null);
		// BOZO
		if (config != null && activateButton.isSelected()) config.setActive(true);
	}

	protected void clearItemSpecificState () {
		lastSelectedTriggerIndex = -1;
	}

	public JToggleButton getActivateButton () {
		return activateButton;
	}

	public void setSelectedTrigger (int index) {
		if (index == -1) {
			triggersTable.clearSelection();
			return;
		}
		if (index >= triggersTable.getRowCount()) return;
		triggersTable.setRowSelectionInterval(index, index);
		Util.scrollRowToVisisble(triggersTable, index);
	}

	protected JPopupMenu getPopupMenu () {
		final Config config = getSelectedItem();
		JPopupMenu popupMenu = new JPopupMenu();
		popupMenu.add(new JMenuItem("Export...")).addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				FileChooser fileChooser = FileChooser.get(owner, "export", ".");
				if (!fileChooser.show("Export Config", true)) return;
				File file = fileChooser.getSelectedFile();
				if (!file.getName().endsWith(".zip")) file = new File(file.getParent(), file.getName() + ".zip");
				ZipOutputStream output = null;
				try {
					output = new ZipOutputStream(new FileOutputStream(file));

					output.putNextEntry(new ZipEntry("config/" + config.getName() + ".config"));
					ByteArrayOutputStream bytes = new ByteArrayOutputStream(512);
					config.save(new OutputStreamWriter(bytes));
					output.write(bytes.toByteArray());
					bytes.reset();

					StringBuilder buffer = new StringBuilder(256);
					for (Trigger trigger : config.getTriggers()) {
						Action action = trigger.getAction();
						if (!(action instanceof ScriptAction)) continue;
						Script script = ((ScriptAction)action).getScript();
						if (script == null) {
							buffer.append(((ScriptAction)action).getScriptName());
							buffer.append('\n');
							continue;
						}
						output.putNextEntry(new ZipEntry("scripts/" + script.getName() + ".script"));
						script.save(new OutputStreamWriter(bytes));
						output.write(bytes.toByteArray());
						bytes.reset();
					}

					if (buffer.length() > 0) {
						Util.errorDialog(ConfigEditor.this, "Export Config",
							"The export completed with warnings.\nThe following scripts could not be found:\n" + buffer);
					}
				} catch (IOException ex) {
					if (Log.ERROR) error("Error exporting config.", ex);
				} finally {
					try {
						if (output != null) output.close();
					} catch (IOException ignored) {
					}
				}
			}
		});
		return popupMenu;
	}

	private void initializeEvents () {
		activateButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				if (activateButton.isSelected()) {
					Device device = owner.getDevice();
					if (device != null) {
						try {
							device.reset();
						} catch (IOException ex) {
							if (WARN) warn("Unable to reset device.", ex);
						}
					}
				}
				getSelectedItem().setActive(activateButton.isSelected());
			}
		});

		newTriggerButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				Config config = getSelectedItem();
				owner.getConfigTab().showInputTriggerPanel(config, null);
			}
		});

		deleteTriggerButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				getSelectedItem().getTriggers().remove(triggersTable.getSelectedRow());
				saveItem(true);
			}
		});

		editTriggerButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				Config config = getSelectedItem();
				InputTrigger trigger = (InputTrigger)config.getTriggers().get(triggersTable.getSelectedRow());
				owner.getConfigTab().showInputTriggerPanel(config, trigger);
			}
		});

		triggersTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged (ListSelectionEvent event) {
				if (event.getValueIsAdjusting()) return;
				Config config = getSelectedItem();
				int selectedRow = triggersTable.getSelectedRow();
				if (selectedRow != -1) lastSelectedTriggerIndex = selectedRow;
				editTriggerButton.setEnabled(selectedRow != -1 && config.getTriggers().get(selectedRow) instanceof InputTrigger);
			}
		});

		triggersTable.addMouseListener(new MouseAdapter() {
			public void mouseClicked (MouseEvent event) {
				if (event.getClickCount() != 2) return;
				editTriggerButton.doClick();
			}

			public void mousePressed (MouseEvent event) {
				showPopup(event);
			}

			public void mouseReleased (MouseEvent event) {
				showPopup(event);
			}

			private void showPopup (MouseEvent event) {
				if (!event.isPopupTrigger()) return;
				int selectedRow = triggersTable.getSelectedRow();
				if (selectedRow == -1) return;
				Config config = getSelectedItem();
				Action action = config.getTriggers().get(triggersTable.getSelectedRow()).getAction();
				if (!(action instanceof ScriptAction)) return;
				final Script script = ((ScriptAction)action).getScript();
				if (script == null) return;

				JPopupMenu popupMenu = new JPopupMenu();
				popupMenu.add(new JMenuItem("Goto Script...")).addActionListener(new ActionListener() {
					public void actionPerformed (ActionEvent event) {
						owner.getScriptEditor().setSelectedItem(script);
						owner.getTabs().setSelectedComponent(owner.getScriptEditor());
					}
				});
				popupMenu.show(triggersTable, event.getX(), event.getY());
			}
		});

		deadzonesButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				new DeviceDeadzoneDialog(owner, device, getSelectedItem()).setVisible(true);
			}
		});

		mouseButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				new XIMMouseDialog(owner, getSelectedItem()).setVisible(true);
			}
		});
	}

	private void initializeLayout () {
		{
			JScrollPane scroll = new JScrollPane();
			getContentPanel().add(
				scroll,
				new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0,
					0), 0, 0));
			{
				triggersTable = new JTable() {
					public boolean isCellEditable (int row, int column) {
						return false;
					}
				};
				scroll.setViewportView(triggersTable);
				triggersTableModel = new DefaultTableModel(new String[][] {}, new String[] {"Name", "Trigger", "Action"});
				triggersTable.setModel(triggersTableModel);
				triggersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				triggersTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
					public Component getTableCellRendererComponent (JTable table, Object value, boolean isSelected, boolean hasFocus,
						int row, int column) {
						hasFocus = false; // Disable cell focus.
						JLabel label = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
						label.setBorder(new EmptyBorder(new Insets(0, 4, 0, 0))); // Padding.
						label.setForeground(isSelected ? table.getSelectionForeground() : null);
						// Highlight invalid triggers and actions.
						if (column == 0) {
							Trigger trigger = getSelectedItem().getTriggers().get(row);
							if (!trigger.isValid()) label.setForeground(Color.red);
						} else if (column == 1) {
							Action action = getSelectedItem().getTriggers().get(row).getAction();
							if (action == null || !action.isValid()) label.setForeground(Color.red);
						}
						return label;
					}
				});
				triggersTable.setRowHeight(triggersTable.getRowHeight() + 9);
				TableColumnModel columnModel = triggersTable.getColumnModel();
				columnModel.getColumn(0).setPreferredWidth(340);
				columnModel.getColumn(1).setPreferredWidth(340);
				columnModel.getColumn(2).setPreferredWidth(320);
			}
		}
		{
			JPanel panel = new JPanel(new GridBagLayout());
			getContentPanel().add(
				panel,
				new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(6, 0, 6,
					0), 0, 0));
			{
				JPanel leftPanel = new JPanel(new GridBagLayout());
				panel.add(leftPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(0, 0, 0, 0), 0, 0));
				{
					activateButton = new JToggleButton("Activate");
					activateButton.setEnabled(false);
					leftPanel.add(activateButton, new GridBagConstraints(-1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
						GridBagConstraints.NONE, new Insets(0, 0, 0, 6), 0, 0));
				}
				{
					deadzonesButton = new JButton("Deadzones");
					leftPanel.add(deadzonesButton, new GridBagConstraints(-1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
						GridBagConstraints.NONE, new Insets(0, 0, 0, 6), 0, 0));
				}
				{
					mouseButton = new JButton("Mouse");
					leftPanel.add(mouseButton, new GridBagConstraints(-1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
						GridBagConstraints.NONE, new Insets(0, 0, 0, 6), 0, 0));
				}
			}
			{
				JPanel rightPanel = new JPanel(new GridLayout(1, 1, 6, 6));
				panel.add(rightPanel, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(0, 0, 0, 0), 0, 0));
				{
					deleteTriggerButton = new JButton("Delete");
					rightPanel.add(deleteTriggerButton);
				}
				{
					editTriggerButton = new JButton("Edit");
					rightPanel.add(editTriggerButton);
					editTriggerButton.setEnabled(false);
				}
				{
					newTriggerButton = new JButton("New");
					rightPanel.add(newTriggerButton);
				}
			}
		}

		Util.enableWhenModelHasSelection(getSelectionModel(), new Runnable() {
			public void run () {
				deadzonesButton.setEnabled(device != null && deadzonesButton.isEnabled());
			}
		}, triggersTable, newTriggerButton, deadzonesButton);
		Util.enableWhenModelHasSelection(triggersTable.getSelectionModel(), deleteTriggerButton);
	}

	public void setDevice (Device device) {
		this.device = device;
		triggersTable.repaint();
		deadzonesButton.setEnabled(device != null && getSelectedItem() != null);
	}
}
