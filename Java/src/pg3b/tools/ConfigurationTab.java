
package pg3b.tools;

import static com.esotericsoftware.minlog.Log.error;

import java.awt.CardLayout;
import java.awt.Color;
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

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import pg3b.PG3B.Axis;
import pg3b.PG3B.Button;
import pg3b.tools.Config.Input;
import pg3b.tools.util.DirectoryMonitor;
import pg3b.tools.util.UI;

import com.esotericsoftware.minlog.Log;

public class ConfigurationTab extends JPanel {
	final PG3BTool owner;
	CardLayout cardLayout;
	private ConfigCard configCard;
	private InputCard inputCard;

	public ConfigurationTab (PG3BTool owner) {
		this.owner = owner;
		setLayout(cardLayout = new CardLayout());
		add(configCard = new ConfigCard(), "configCard");
		add(inputCard = new InputCard(), "inputCard");
	}

	private class ConfigCard extends JPanel {
		File rootDir = new File("config");
		DirectoryMonitor<Config> monitor;
		JList configsList;
		DefaultComboBoxModel configsListModel;
		JTextField configNameText;
		JTable inputsTable;
		DefaultTableModel inputsTableModel;
		JButton newConfigButton, deleteConfigButton, newInputButton, deleteInputButton;
		JTextArea configDescriptionText;

		public ConfigCard () {
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
					inputsTableModel.setRowCount(0);
					for (Input input : config.getInputs())
						inputsTableModel.addRow(new Object[] {input.getDescription()});
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
					Config config = new Config(new File("config", name + ".config"));
					try {
						config.save();
					} catch (IOException ex) {
						if (Log.ERROR) error("Unable to create config file: " + config.getFile(), ex);
						UI.errorDialog(ConfigurationTab.this, "Error", "An error occurred while attempting to create the config file.");
						return;
					}
					monitor.scan(rootDir);
					configsList.setSelectedValue(config, true);
				}
			});

			deleteConfigButton.addActionListener(new ActionListener() {
				public void actionPerformed (ActionEvent event) {
					Config config = (Config)configsList.getSelectedValue();
					if (JOptionPane.showConfirmDialog(ConfigurationTab.this,
						"Are you sure you want to delete the selected config?\nThis action cannot be undone.", "Confirm Delete",
						JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
					config.getFile().delete();
					monitor.scan(rootDir);
				}
			});

			FocusAdapter focusListener = new FocusAdapter() {
				public void focusLost (FocusEvent e) {
					synchronized (monitor) {
						Config config = (Config)configsList.getSelectedValue();
						try {
							Config oldConfig = config.clone();
							// Rename file if needed.
							String name = new File(configNameText.getText().trim()).getName();
							if (name.length() == 0) name = config.getName();
							if (!name.equalsIgnoreCase(config.getName())) {
								File newFile = new File(config.getFile().getParent(), name + ".config");
								config.getFile().renameTo(newFile);
								config = Config.load(newFile);
							}

							config.setDescription(configDescriptionText.getText());

							if (!oldConfig.equals(config)) {
								config.save();
								monitor.scan(rootDir);
							}
						} catch (IOException ex) {
							if (Log.ERROR) error("Unable to save config file: " + config.getFile(), ex);
							UI.errorDialog(ConfigurationTab.this, "Error", //
								"An error occurred while attempting to save the config file.");
							return;
						}
						configsList.setSelectedValue(config, true);
					}
				}
			};
			configNameText.addFocusListener(focusListener);
			configDescriptionText.addFocusListener(focusListener);

			newInputButton.addActionListener(new ActionListener() {
				public void actionPerformed (ActionEvent event) {
					cardLayout.show(ConfigurationTab.this, "inputCard");
				}
			});
		}

		private void initializeLayout () {
			setLayout(new GridBagLayout());
			{
				JScrollPane scroll = new JScrollPane();
				scroll.setMinimumSize(new Dimension(150, 3));
				scroll.setMaximumSize(new Dimension(150, 3));
				scroll.setPreferredSize(new Dimension(150, 3));
				add(scroll, new GridBagConstraints(1, 1, 1, 2, 0.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(6, 6, 0, 6), 0, 0));
				{
					configsList = new JList();
					scroll.setViewportView(configsList);
					configsList.setModel(configsListModel = new DefaultComboBoxModel());
					configsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				}
			}
			{
				JScrollPane scroll = new JScrollPane();
				add(scroll, new GridBagConstraints(2, 2, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(6, 0, 0, 6), 0, 0));
				{
					inputsTable = new JTable();
					scroll.setViewportView(inputsTable);
					inputsTableModel = new DefaultTableModel(new String[][] {}, new String[] {"Description", "Input", "Action"});
					inputsTable.setModel(inputsTableModel);
				}
			}
			{
				JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 6));
				add(panel, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(
					0, 0, 0, 0), 0, 0));
				{
					newConfigButton = new JButton("New");
					panel.add(newConfigButton);
				}
				{
					deleteConfigButton = new JButton("Delete");
					panel.add(deleteConfigButton);
				}
			}
			{
				JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 6));
				add(panel, new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(
					0, 0, 0, 0), 0, 0));
				{
					newInputButton = new JButton("New");
					panel.add(newInputButton);
				}
				{
					deleteInputButton = new JButton("Delete");
					panel.add(deleteInputButton);
				}
			}
			{
				JPanel panel = new JPanel(new GridBagLayout());
				add(panel, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 0, 0), 0, 0));
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
					panel.add(label, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHEAST,
						GridBagConstraints.NONE, new Insets(6, 6, 0, 6), 0, 0));
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

			UI.enableWhenListHasSelection(configsList, deleteConfigButton, inputsTable, newInputButton, deleteInputButton,
				configNameText, configDescriptionText, owner.getCaptureButton());
		}
	}

	private class InputCard extends JPanel {
		Input input;
		JPanel inputPanel;
		JRadioButton pg3bRadio, scriptRadio;
		JButton saveButton, cancelButton;
		JTextField inputText, descriptionText;
		JComboBox pg3bCombo, scriptCombo;
		DefaultComboBoxModel scriptComboModel, pg3bComboModel;

		public InputCard () {
			initializeLayout();
			initializeEvents();
		}

		public void show (Input input) {
			if (input == null) {
				input = new Input();
				inputPanel.setBorder(BorderFactory.createTitledBorder("New Input"));
			} else
				inputPanel.setBorder(BorderFactory.createTitledBorder("Edit Input"));
			this.input = input;
			descriptionText.setText(input.getDescription());

			scriptComboModel.removeAllElements();
			scriptComboModel.addElement("<New Script>");
			for (Script script : owner.getScriptsTab().getScripts())
				scriptComboModel.addElement(script);

			if (input.getScript() != null) {
				scriptRadio.setSelected(true);
				scriptCombo.setSelectedItem(input.getScript());
			} else {
				pg3bRadio.setSelected(true);
				pg3bCombo.setSelectedItem(input.getTarget());
			}
		}

		private void initializeEvents () {
			scriptCombo.addActionListener(new ActionListener() {
				public void actionPerformed (ActionEvent event) {
					if (scriptCombo.getSelectedItem() == null) return;
					scriptRadio.setSelected(true);
					pg3bCombo.setSelectedItem(null);
				}
			});

			pg3bCombo.addActionListener(new ActionListener() {
				public void actionPerformed (ActionEvent event) {
					if (pg3bCombo.getSelectedItem() == null) return;
					pg3bRadio.setSelected(true);
					scriptCombo.setSelectedItem(null);
				}
			});
		}

		private void initializeLayout () {
			setLayout(new GridBagLayout());
			inputPanel = new JPanel(new GridBagLayout());
			add(inputPanel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(0, 0, 0, 0), 0, 0));
			inputPanel.setBorder(BorderFactory.createTitledBorder("New Input"));
			{
				JLabel label = new JLabel("Description:");
				inputPanel.add(label, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(3, 6, 6, 0), 0, 0));
			}
			{
				descriptionText = new JTextField();
				inputPanel.add(descriptionText, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
					GridBagConstraints.HORIZONTAL, new Insets(0, 6, 6, 6), 0, 0));
			}
			{
				JLabel label = new JLabel("Input:");
				inputPanel.add(label, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(3, 6, 6, 0), 0, 0));
			}
			{
				JLabel label = new JLabel("Action:");
				inputPanel.add(label, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHEAST,
					GridBagConstraints.NONE, new Insets(0, 6, 6, 0), 0, 0));
			}
			{
				JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 6));
				inputPanel.add(panel, new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(0, 0, 6, 6), 0, 0));
				{
					scriptRadio = new JRadioButton("Script");
					panel.add(scriptRadio);
				}
				{
					scriptCombo = new JComboBox();
					panel.add(scriptCombo);
					scriptCombo.setModel(scriptComboModel = new DefaultComboBoxModel());
				}
			}
			{
				JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 6));
				inputPanel.add(panel, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(0, 0, 6, 6), 0, 0));
				{
					pg3bRadio = new JRadioButton("PG3B");
					panel.add(pg3bRadio);
				}
				{
					pg3bCombo = new JComboBox();
					panel.add(pg3bCombo);
					pg3bComboModel = new DefaultComboBoxModel(new Object[] {Axis.leftStickX, Axis.leftStickY, Axis.rightStickX,
						Axis.rightStickY, Axis.leftTrigger, Axis.rightTrigger, Button.a, Button.b, Button.x, Button.y, Button.up,
						Button.down, Button.left, Button.right, Button.leftShoulder, Button.rightShoulder, Button.leftStick,
						Button.rightStick, Button.start, Button.back, Button.guide});
					pg3bCombo.setModel(pg3bComboModel);
				}
			}
			{
				JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 6));
				inputPanel.add(panel, new GridBagConstraints(1, 4, 2, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(0, 0, 0, 0), 0, 0));
				{
					cancelButton = new JButton("Cancel");
					panel.add(cancelButton);
				}
				{
					saveButton = new JButton("Save");
					panel.add(saveButton);
				}
			}
			{
				inputText = new JTextField();
				inputPanel.add(inputText, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
					GridBagConstraints.HORIZONTAL, new Insets(0, 6, 6, 6), 0, 0));
				inputText.setEditable(false);
				inputText.setBackground(new Color(192, 192, 192));
				inputText.setFocusable(false);
			}
			{
				JPanel spacer = new JPanel();
				inputPanel.add(spacer, new GridBagConstraints(1, 5, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER,
					GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
				spacer.setMinimumSize(new Dimension(350, 0));
				spacer.setMaximumSize(new Dimension(350, 0));
				spacer.setPreferredSize(new Dimension(350, 0));
			}
			ButtonGroup group = new ButtonGroup();
			group.add(pg3bRadio);
			group.add(scriptRadio);
		}
	}
}
