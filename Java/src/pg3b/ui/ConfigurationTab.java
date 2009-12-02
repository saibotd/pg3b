
package pg3b.ui;

import static com.esotericsoftware.minlog.Log.error;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Event;
import net.java.games.input.EventQueue;
import net.java.games.input.Component.Identifier;

import pg3b.PG3B.Axis;
import pg3b.PG3B.Button;
import pg3b.ui.Config.Input;
import pg3b.ui.ControllerPanel.Listener;
import pg3b.ui.util.DirectoryMonitor;
import pg3b.ui.util.UI;

import com.esotericsoftware.minlog.Log;

public class ConfigurationTab extends JPanel {
	static final Timer timer = new Timer("MonitorControllers", true);

	final PG3BUI owner;
	CardLayout cardLayout;
	Settings settings = Settings.get();
	ConfigCard configCard;
	InputCard inputCard;

	public ConfigurationTab (PG3BUI owner) {
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
					inputsTableModel.setRowCount(0);
					for (Input input : config.getInputs())
						inputsTableModel.addRow(new Object[] {input.getDescription()});

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
					if (configsList.getSelectedIndex() == -1 && configsListModel.getSize() > 0) configsList.setSelectedIndex(0);
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
					inputCard.showPanel(null);
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

		public void showPanel () {
			inputCard.hidePanel();
			cardLayout.show(ConfigurationTab.this, "configCard");
		}
	}

	private class InputCard extends JPanel {
		Input input;
		TimerTask monitorInputTask;
		JPanel inputPanel;
		JRadioButton pg3bRadio, scriptRadio;
		JButton saveButton, cancelButton;
		JTextField inputText, descriptionText;
		JComboBox pg3bCombo, scriptCombo;
		DefaultComboBoxModel scriptComboModel, pg3bComboModel;

		Listener controllerPanelListener = new Listener() {
			public void axisChanged (Axis axis, float state) {
				if (Math.abs(state) > 0.1f) pg3bCombo.setSelectedItem(axis);
			}

			public void buttonChanged (Button button, boolean pressed) {
				if (pressed) pg3bCombo.setSelectedItem(button);
			}
		};

		public InputCard () {
			initializeLayout();
			initializeEvents();
		}

		public void showPanel (Input input) {
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
				if (input.getTarget() != null) pg3bCombo.setSelectedItem(input.getTarget());
			}

			owner.getControllerPanel().addListener(controllerPanelListener);
			cardLayout.show(ConfigurationTab.this, "inputCard");
		}

		public void hidePanel () {
			owner.getControllerPanel().removeListener(controllerPanelListener);
		}

		void monitorInput (boolean enable) {
			if (monitorInputTask != null) {
				monitorInputTask.cancel();
				monitorInputTask = null;
			}
			if (!enable) return;
			monitorInputTask = new TimerTask() {
				boolean firstRun = true;

				public void run () {
					boolean disable = false;
					for (Controller controller : ControllerEnvironment.getDefaultEnvironment().getControllers()) {
						if (!controller.poll()) continue;
						EventQueue eventQueue = controller.getEventQueue();
						Event event = new Event();
						while (eventQueue.getNextEvent(event)) {
							if (firstRun) continue; // Clear out all pending events on the first run.
							Component component = event.getComponent();
							float value = event.getValue();
							if (value != 0) {
								String id = component.getIdentifier().toString();
								if (id.equals(" ")) id = "Spacebar";
								setInputComponent(component);
								disable = true;
							}
						}
					}
					firstRun = false;
					if (disable) {
						monitorInputTask.cancel();
						timer.schedule(new TimerTask() {
							public void run () {
								monitorInput(false);
							}
						}, 300);
					}
				}
			};
			timer.scheduleAtFixedRate(monitorInputTask, 125, 125);
		}

		void setInputComponent (Object object) {
			inputText.setText(object.toString());
			inputText.setFont(inputText.getFont().deriveFont(Font.PLAIN));
			
		}

		private void initializeEvents () {
			scriptCombo.addActionListener(new ActionListener() {
				public void actionPerformed (ActionEvent event) {
					if (scriptCombo.getSelectedItem() == null) return;
					scriptRadio.setSelected(true);
				}
			});

			pg3bCombo.addActionListener(new ActionListener() {
				public void actionPerformed (ActionEvent event) {
					if (pg3bCombo.getSelectedItem() == null) return;
					pg3bRadio.setSelected(true);
					scriptCombo.setSelectedItem(null);
				}
			});

			scriptRadio.addItemListener(new ItemListener() {
				public void itemStateChanged (ItemEvent event) {
					if (!scriptRadio.isSelected()) return;
					pg3bCombo.setSelectedItem(null);
					if (scriptCombo.getSelectedIndex() == -1) scriptCombo.setSelectedIndex(0);
				}
			});

			pg3bRadio.addItemListener(new ItemListener() {
				public void itemStateChanged (ItemEvent event) {
					if (!pg3bRadio.isSelected()) return;
					scriptCombo.setSelectedItem(null);
					if (pg3bCombo.getSelectedIndex() == -1) pg3bCombo.setSelectedIndex(0);
				}
			});

			inputText.addMouseListener(new MouseAdapter() {
				public void mouseClicked (MouseEvent event) {
					if (monitorInputTask != null) return;
					inputText.setText("Waiting for input...");
					inputText.setFont(inputText.getFont().deriveFont(Font.ITALIC));
					monitorInput(true);
				}
			});

			cancelButton.addActionListener(new ActionListener() {
				public void actionPerformed (ActionEvent event) {
					configCard.showPanel();
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
					GridBagConstraints.NONE, new Insets(4, 6, 6, 0), 0, 0));
			}
			{
				JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
				inputPanel.add(panel, new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(0, 0, 6, 6), 0, 0));
				{
					scriptRadio = new JRadioButton("Script");
					panel.add(scriptRadio);
				}
				{
					scriptCombo = new JComboBox();
					panel.add(scriptCombo);
					scriptComboModel = new DefaultComboBoxModel();
					scriptCombo.setModel(scriptComboModel);
				}
			}
			{
				JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
				inputPanel.add(panel, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(0, 0, 6, 6), 0, 0));
				{
					pg3bRadio = new JRadioButton("PG3B");
					panel.add(pg3bRadio);
				}
				{
					pg3bCombo = new JComboBox();
					panel.add(pg3bCombo);
					pg3bComboModel = new DefaultComboBoxModel(new Object[] {Button.a, Button.b, Button.x, Button.y, Button.up,
						Button.down, Button.left, Button.right, Button.leftShoulder, Button.rightShoulder, Button.leftStick,
						Button.rightStick, Button.start, Button.back, Button.guide, Axis.leftStickX, Axis.leftStickY, Axis.rightStickX,
						Axis.rightStickY, Axis.leftTrigger, Axis.rightTrigger});
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
