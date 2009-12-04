
package pg3b.ui.swing;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;

import javax.swing.JButton;
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

import pg3b.ui.Config;
import pg3b.ui.ControllerTrigger;
import pg3b.ui.Settings;
import pg3b.ui.Trigger;
import pg3b.util.UI;

public class ConfigPanel extends EditorPanel<Config> {
	private int lastSelectedTriggerIndex;

	private JTable triggersTable;
	private DefaultTableModel triggersTableModel;
	private JButton newTriggerButton, deleteTriggerButton, editTriggerButton;

	public ConfigPanel (PG3BUI owner) {
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

	protected void itemSelected (Config config) {
		triggersTableModel.setRowCount(0);
		if (config == null)
			owner.getCaptureButton().setSelected(false);
		else {
			for (Trigger trigger : config.getTriggers())
				triggersTableModel.addRow(new Object[] {trigger, trigger.getAction(), trigger.getDescription()});
			setTriggerSelected(lastSelectedTriggerIndex);

			if (!config.getName().equals(settings.selectedConfig)) {
				settings.selectedConfig = config.getName();
				Settings.save();
			}
		}
	}

	public void setTriggerSelected (int index) {
		if (index >= triggersTable.getRowCount()) return;
		triggersTable.setRowSelectionInterval(index, index);
		UI.scrollRowToVisisble(triggersTable, index);
	}

	private void initializeEvents () {
		newTriggerButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				Config config = getSelectedItem();
				owner.getConfigTab().showTriggerPanel(config, null);
			}
		});

		deleteTriggerButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				Config config = getSelectedItem();
				config.getTriggers().remove(triggersTable.getSelectedRow());
				saveItem(config);
			}
		});

		editTriggerButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				Config config = getSelectedItem();
				ControllerTrigger trigger = (ControllerTrigger)config.getTriggers().get(triggersTable.getSelectedRow());
				owner.getConfigTab().showTriggerPanel(config, trigger);
			}
		});

		triggersTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged (ListSelectionEvent event) {
				if (event.getValueIsAdjusting()) return;
				Config config = getSelectedItem();
				int selectedRow = triggersTable.getSelectedRow();
				if (selectedRow != -1) lastSelectedTriggerIndex = selectedRow;
				editTriggerButton.setEnabled(selectedRow != -1 && config.getTriggers().get(selectedRow) instanceof ControllerTrigger);
			}
		});

		triggersTable.addMouseListener(new MouseAdapter() {
			public void mouseClicked (MouseEvent event) {
				if (event.getClickCount() != 2) return;
				editTriggerButton.doClick();
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
				triggersTableModel = new DefaultTableModel(new String[][] {}, new String[] {"Trigger", "Action", "Description"});
				triggersTable.setModel(triggersTableModel);
				triggersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				triggersTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
					public Component getTableCellRendererComponent (JTable table, Object value, boolean isSelected, boolean hasFocus,
						int row, int column) {
						hasFocus = false; // Disable cell focus.
						JLabel label = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
						label.setBorder(new EmptyBorder(new Insets(0, 4, 0, 0))); // Padding.
						return label;
					}
				});
				triggersTable.setRowHeight(triggersTable.getRowHeight() + 9);
				TableColumnModel columnModel = triggersTable.getColumnModel();
				columnModel.getColumn(0).setPreferredWidth(450);
				columnModel.getColumn(1).setPreferredWidth(310);
				columnModel.getColumn(2).setPreferredWidth(240);
			}
		}
		{
			JPanel panel = new JPanel(new GridLayout(1, 1, 6, 6));
			getContentPanel().add(
				panel,
				new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(6, 6, 6, 0), 0, 0));
			{
				deleteTriggerButton = new JButton("Delete");
				panel.add(deleteTriggerButton);
			}
			{
				editTriggerButton = new JButton("Edit");
				panel.add(editTriggerButton);
				editTriggerButton.setEnabled(false);
			}
			{
				newTriggerButton = new JButton("New");
				panel.add(newTriggerButton);
			}
		}

		UI.enableWhenModelHasSelection(getSelectionModel(), triggersTable, newTriggerButton, owner.getCaptureButton());
		UI.enableWhenModelHasSelection(triggersTable.getSelectionModel(), deleteTriggerButton);
	}
}
