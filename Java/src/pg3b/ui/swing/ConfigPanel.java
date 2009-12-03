
package pg3b.ui.swing;

import static com.esotericsoftware.minlog.Log.*;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import pg3b.ui.Config;
import pg3b.ui.Settings;
import pg3b.ui.Trigger;
import pg3b.util.DirectoryMonitor;
import pg3b.util.UI;

import com.esotericsoftware.minlog.Log;

// BOZO - Highlight triggers table for missing controllers, scripts, etc.
// BOZO - Add double click to edit trigger, edit button. Unable to edit non-ControllerTrigger.
// BOZO - Left align delete buttons.
// BOZO - Add key modifiers to trigger panel.

public class ConfigPanel extends JPanel {
	PG3BUI owner;
	Settings settings = Settings.get();
	File rootDir = new File("config");
	DirectoryMonitor<Config> monitor;

	JList configsList;
	DefaultComboBoxModel configsListModel;
	JTextField configNameText;
	JTable triggersTable;
	DefaultTableModel triggersTableModel;
	JButton newConfigButton, deleteConfigButton, newTriggerButton, deleteTriggerButton;
	JTextArea configDescriptionText;

	public ConfigPanel (PG3BUI owner) {
		this.owner = owner;

		initializeLayout();
		initializeEvents();

		monitor = new DirectoryMonitor<Config>(".config") {
			protected Config load (File file) throws IOException {
				return Config.load(file);
			}

			protected void updated () {
				Config selectedConfig = (Config)configsList.getSelectedValue();
				configsListModel.removeAllElements();
				for (Config config : getItems())
					configsListModel.addElement(config);
				configsList.setSelectedValue(selectedConfig, true);
			}
		};
		monitor.scan(rootDir, 3000);

		for (Config config : monitor.getItems()) {
			if (config.getName().equals(settings.selectedConfig)) {
				configsList.setSelectedValue(config, true);
				break;
			}
		}
		if (configsList.getSelectedIndex() == -1 && configsListModel.getSize() > 0) configsList.setSelectedIndex(0);
	}

	private void initializeEvents () {
		configsList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged (ListSelectionEvent event) {
				if (event.getValueIsAdjusting()) return;
				Config config = (Config)configsList.getSelectedValue();
				if (config == null) {
					config = new Config();
					owner.getCaptureButton().setSelected(false);
				}
				configNameText.setText(config.getName());
				configDescriptionText.setText(config.getDescription());
				triggersTableModel.setRowCount(0);
				for (Trigger trigger : config.getTriggers())
					triggersTableModel.addRow(new Object[] {trigger, trigger.getAction(), trigger.getDescription()});

				if (!config.getName().equals(settings.selectedConfig)) {
					settings.selectedConfig = config.getName();
					Settings.save();
				}
			}
		});

		newConfigButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				List<Config> configs = monitor.getItems();
				int i = 0;
				String name;
				outer: // 
				while (true) {
					name = "New Config";
					if (i > 0) name += " (" + i + ")";
					i++;
					for (Config config : configs)
						if (config.getName().equalsIgnoreCase(name)) continue outer;
					break;
				}
				saveConfig(new Config(new File("config", name + ".config")));
			}
		});

		deleteConfigButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				Config config = (Config)configsList.getSelectedValue();
				if (JOptionPane.showConfirmDialog(owner,
					"Are you sure you want to delete the selected config?\nThis action cannot be undone.", "Confirm Delete",
					JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
				config.getFile().delete();
				monitor.scan(rootDir);
				if (configsList.getSelectedIndex() == -1 && configsListModel.getSize() > 0) configsList.setSelectedIndex(0);
			}
		});

		FocusAdapter focusListener = new FocusAdapter() {
			public void focusLost (FocusEvent e) {
				synchronized (monitor) {
					Config config = (Config)configsList.getSelectedValue();
					Config oldConfig = config.clone();

					// Rename file if needed.
					String name = new File(configNameText.getText().trim()).getName();
					if (name.length() == 0) name = config.getName();
					if (!name.equalsIgnoreCase(config.getName())) {
						File newFile = new File(config.getFile().getParent(), name + ".config");
						config.getFile().renameTo(newFile);
						try {
							config = Config.load(newFile);
						} catch (IOException ex) {
							if (Log.ERROR) error("Unable to save config file: " + config.getFile(), ex);
							UI.errorDialog(owner, "Error", //
								"An error occurred while attempting to save the config file.");
							return;
						}
					}

					config.setDescription(configDescriptionText.getText());

					if (!oldConfig.equals(config)) saveConfig(config);
				}
			}
		};
		configNameText.addFocusListener(focusListener);
		configDescriptionText.addFocusListener(focusListener);

		newTriggerButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				Config config = (Config)configsList.getSelectedValue();
				owner.getConfigTab().showTriggerPanel(config, null);
			}
		});

		deleteTriggerButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				Config config = (Config)configsList.getSelectedValue();
				config.getTriggers().remove(triggersTable.getSelectedRow());
				saveConfig(config);
			}
		});
	}

	public void saveConfig (Config config) {
		try {
			config.save();
			monitor.scan(rootDir);
		} catch (IOException ex) {
			if (Log.ERROR) error("Unable to save config file: " + config.getFile(), ex);
			UI.errorDialog(owner, "Error", //
				"An error occurred while attempting to save the config file.");
			return;
		}
		for (int i = 0, n = configsListModel.getSize(); i < n; i++) {
			Config listConfig = (Config)configsListModel.getElementAt(i);
			if (listConfig.getFile().equals(config.getFile())) {
				configsList.setSelectedIndex(i);
				break;
			}
		}
	}

	private void initializeLayout () {
		setLayout(new GridBagLayout());
		{
			JScrollPane scroll = new JScrollPane();
			scroll.setMinimumSize(new Dimension(150, 3));
			scroll.setMaximumSize(new Dimension(150, 3));
			scroll.setPreferredSize(new Dimension(150, 3));
			add(scroll, new GridBagConstraints(1, 1, 1, 2, 0.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(
				6, 6, 0, 6), 0, 0));
			{
				configsList = new JList();
				scroll.setViewportView(configsList);
				configsList.setModel(configsListModel = new DefaultComboBoxModel());
				configsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			}
		}
		{
			JScrollPane scroll = new JScrollPane();
			add(scroll, new GridBagConstraints(2, 2, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(
				6, 0, 0, 6), 0, 0));
			{
				triggersTable = new JTable();
				scroll.setViewportView(triggersTable);
				triggersTableModel = new DefaultTableModel(new String[][] {}, new String[] {"Trigger", "Action", "Description"});
				triggersTable.setModel(triggersTableModel);
			}
		}
		{
			JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 6));
			add(panel, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0,
				0, 0, 0), 0, 0));
			{
				deleteConfigButton = new JButton("Delete");
				panel.add(deleteConfigButton);
			}
			{
				newConfigButton = new JButton("New");
				panel.add(newConfigButton);
			}
		}
		{
			JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 6));
			add(panel, new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0,
				0, 0, 0), 0, 0));
			{
				deleteTriggerButton = new JButton("Delete");
				panel.add(deleteTriggerButton);
			}
			{
				newTriggerButton = new JButton("New");
				panel.add(newTriggerButton);
			}
		}
		{
			JPanel panel = new JPanel(new GridBagLayout());
			add(panel, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(
				0, 0, 0, 0), 0, 0));
			{
				panel.add(new JLabel("Name:"), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
					GridBagConstraints.NONE, new Insets(6, 6, 0, 6), 0, 0));
			}
			{
				configNameText = new JTextField();
				panel.add(configNameText, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
					GridBagConstraints.HORIZONTAL, new Insets(6, 0, 0, 6), 0, 0));
			}
			{
				JLabel label = new JLabel("Description:");
				panel.add(label, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHEAST, GridBagConstraints.NONE,
					new Insets(6, 6, 0, 6), 0, 0));
			}
			{
				JScrollPane scroll = new JScrollPane();
				panel.add(scroll, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
					GridBagConstraints.HORIZONTAL, new Insets(6, 0, 0, 6), 0, 0));
				scroll.setMinimumSize(new Dimension(3, 50));
				scroll.setMaximumSize(new Dimension(3, 50));
				scroll.setPreferredSize(new Dimension(3, 50));
				{
					configDescriptionText = new JTextArea();
					scroll.setViewportView(configDescriptionText);
				}
			}
		}

		UI.enableWhenModelHasSelection(configsList.getSelectionModel(), deleteConfigButton, triggersTable, newTriggerButton,
			configNameText, configDescriptionText, owner.getCaptureButton());

		UI.enableWhenModelHasSelection(triggersTable.getSelectionModel(), deleteTriggerButton);
	}
}
