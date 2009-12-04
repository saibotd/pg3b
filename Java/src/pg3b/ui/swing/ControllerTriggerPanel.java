
package pg3b.ui.swing;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Event;
import net.java.games.input.EventQueue;
import pg3b.Axis;
import pg3b.Button;
import pg3b.Target;
import pg3b.ui.Action;
import pg3b.ui.Config;
import pg3b.ui.ControllerTrigger;
import pg3b.ui.PG3BAction;
import pg3b.ui.Script;
import pg3b.ui.ScriptAction;
import pg3b.ui.swing.XboxControllerPanel.Listener;
import pg3b.util.UI;

public class ControllerTriggerPanel extends JPanel {
	static final Timer timer = new Timer("MonitorControllers", true);

	PG3BUI owner;
	Config config;
	ControllerTrigger trigger;
	boolean isNewTrigger;
	TimerTask monitorConrollersTask;

	JPanel titlePanel;
	JRadioButton targetRadio, scriptRadio;
	JButton saveButton, cancelButton;
	JTextField triggerText, descriptionText;
	JComboBox targetCombo, scriptCombo;
	JCheckBox altCheckBox, ctrlCheckBox, shiftCheckBox;
	DefaultComboBoxModel scriptComboModel, targetComboModel;

	Listener controllerPanelListener = new Listener() {
		public void axisChanged (Axis axis, float state) {
			if (!isVisible()) return;
			if (Math.abs(state) > 0.1f) targetCombo.setSelectedItem(axis);
		}

		public void buttonChanged (Button button, boolean pressed) {
			if (!isVisible()) return;
			if (pressed) targetCombo.setSelectedItem(button);
		}
	};

	public ControllerTriggerPanel (PG3BUI owner) {
		this.owner = owner;

		initializeLayout();
		initializeEvents();

		owner.getControllerPanel().addListener(controllerPanelListener);
	}

	public void setTrigger (Config config, ControllerTrigger trigger) {
		this.config = config;

		UI.setEnabled(true, targetRadio, targetCombo, scriptRadio, scriptCombo);

		scriptComboModel.removeAllElements();
		scriptComboModel.addElement("<New Script>");
		for (Script script : owner.getScriptsTab().getItems())
			scriptComboModel.addElement(script);

		if (trigger == null) {
			// New trigger.
			this.trigger = new ControllerTrigger();
			isNewTrigger = true;
			titlePanel.setBorder(BorderFactory.createTitledBorder("New Trigger"));

			triggerText.setText("Click to set trigger...");
			triggerText.setFont(triggerText.getFont().deriveFont(Font.ITALIC));
			descriptionText.setText("");
			targetRadio.setSelected(true);
			shiftCheckBox.setSelected(false);
			ctrlCheckBox.setSelected(false);
			altCheckBox.setSelected(false);

			saveButton.setEnabled(false);
		} else {
			// Edit trigger.
			this.trigger = trigger;
			isNewTrigger = false;
			titlePanel.setBorder(BorderFactory.createTitledBorder("Edit Trigger"));

			setTriggerText(trigger);
			triggerText.setFont(triggerText.getFont().deriveFont(Font.PLAIN));
			descriptionText.setText(trigger.getDescription());
			shiftCheckBox.setSelected(trigger.getShift());
			ctrlCheckBox.setSelected(trigger.getCtrl());
			altCheckBox.setSelected(trigger.getAlt());

			Action action = trigger.getAction();
			if (action instanceof ScriptAction) {
				scriptRadio.setSelected(true);
				scriptCombo.setSelectedItem(((ScriptAction)action).getScriptName());
			} else if (action instanceof PG3BAction) {
				targetRadio.setSelected(true);
				Target target = ((PG3BAction)action).getTarget();
				if (target != null) targetCombo.setSelectedItem(target);
			} else {
				// Unknown action, can't change it.
				UI.setEnabled(false, targetRadio, targetCombo, scriptRadio, scriptCombo);
				targetRadio.setSelected(false);
				scriptRadio.setSelected(false);
			}
		}
	}

	void listenForTrigger (boolean enable) {
		if (monitorConrollersTask != null) {
			monitorConrollersTask.cancel();
			monitorConrollersTask = null;
		}
		if (!enable) return;
		monitorConrollersTask = new TimerTask() {
			boolean firstRun = true;

			public void run () {
				boolean triggerSet = false;
				for (Controller controller : ControllerEnvironment.getDefaultEnvironment().getControllers()) {
					if (!controller.poll()) continue;
					EventQueue eventQueue = controller.getEventQueue();
					Event event = new Event();
					while (eventQueue.getNextEvent(event)) {
						if (firstRun) continue; // Clear out all pending events on the first run.
						Component component = event.getComponent();
						float value = event.getValue();
						if (value != 0) {
							trigger.setComponent(controller, component);
							SwingUtilities.invokeLater(new Runnable() {
								public void run () {
									setTriggerText(trigger);
									triggerText.setFont(triggerText.getFont().deriveFont(Font.PLAIN));
									cancelButton.setEnabled(true);
									saveButton.setEnabled(true);
								}
							});
							listenForTrigger(false);
							return;
						}
					}
				}
				firstRun = false;
			}
		};
		timer.scheduleAtFixedRate(monitorConrollersTask, 125, 125);
	}

	public void setTriggerText (ControllerTrigger trigger) {
		boolean ctrl = trigger.getCtrl();
		boolean alt = trigger.getAlt();
		boolean shift = trigger.getShift();
		trigger.setCtrl(false);
		trigger.setAlt(false);
		trigger.setShift(false);
		triggerText.setText(trigger.toString());
		trigger.setCtrl(ctrl);
		trigger.setAlt(alt);
		trigger.setShift(shift);
	}

	private void initializeEvents () {
		scriptCombo.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				if (scriptCombo.getSelectedItem() == null) return;
				scriptRadio.setSelected(true);
			}
		});

		targetCombo.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				if (targetCombo.getSelectedItem() == null) return;
				targetRadio.setSelected(true);
				scriptCombo.setSelectedItem(null);
			}
		});

		scriptRadio.addItemListener(new ItemListener() {
			public void itemStateChanged (ItemEvent event) {
				if (!scriptRadio.isSelected()) return;
				targetCombo.setSelectedItem(null);
				if (scriptCombo.getSelectedIndex() == -1) scriptCombo.setSelectedIndex(0);
			}
		});

		targetRadio.addItemListener(new ItemListener() {
			public void itemStateChanged (ItemEvent event) {
				if (!targetRadio.isSelected()) return;
				scriptCombo.setSelectedItem(null);
				if (targetCombo.getSelectedIndex() == -1) targetCombo.setSelectedIndex(0);
			}
		});

		triggerText.addMouseListener(new MouseAdapter() {
			public void mousePressed (MouseEvent event) {
				if (monitorConrollersTask != null) return;
				triggerText.setText("Waiting for trigger...");
				triggerText.setFont(triggerText.getFont().deriveFont(Font.ITALIC));
				triggerText.requestFocusInWindow();
				listenForTrigger(true);
				cancelButton.setEnabled(false);
				saveButton.setEnabled(false);
			}
		});

		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				owner.getConfigTab().showConfigPanel();
			}
		});

		saveButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				owner.getConfigTab().showConfigPanel();

				trigger.setDescription(descriptionText.getText());
				trigger.setCtrl(ctrlCheckBox.isSelected());
				trigger.setAlt(altCheckBox.isSelected());
				trigger.setShift(shiftCheckBox.isSelected());
				if (targetRadio.isSelected()) {
					trigger.setAction(new PG3BAction((Target)targetCombo.getSelectedItem()));
				} else if (scriptRadio.isSelected()) {
					// BOZO - Handle new script.
					trigger.setAction(new ScriptAction((String)scriptCombo.getSelectedItem()));
				}

				if (isNewTrigger) config.getTriggers().add(trigger);
				owner.getConfigTab().getConfigPanel().saveItem(config);
				owner.getConfigTab().getConfigPanel().setTriggerSelected(config.getTriggers().indexOf(trigger));
			}
		});
	}

	private void initializeLayout () {
		setLayout(new GridBagLayout());
		titlePanel = new JPanel(new GridBagLayout());
		add(titlePanel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
			new Insets(0, 0, 0, 0), 0, 0));
		titlePanel.setBorder(BorderFactory.createTitledBorder("New Trigger"));
		{
			JLabel label = new JLabel("Description:");
			titlePanel.add(label, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(0, 6, 6, 0), 0, 0));
		}
		{
			descriptionText = new JTextField();
			titlePanel.add(descriptionText, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, new Insets(0, 6, 6, 6), 0, 0));
		}
		{
			JLabel label = new JLabel("Trigger:");
			titlePanel.add(label, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(0, 6, 6, 0), 0, 0));
		}
		{
			JLabel label = new JLabel("Action:");
			titlePanel.add(label, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHEAST,
				GridBagConstraints.NONE, new Insets(4, 6, 6, 0), 0, 0));
		}
		{
			JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
			titlePanel.add(panel, new GridBagConstraints(2, 4, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
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
			titlePanel.add(panel, new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(0, 0, 6, 6), 0, 0));
			{
				targetRadio = new JRadioButton("PG3B");
				panel.add(targetRadio);
			}
			{
				targetCombo = new JComboBox();
				panel.add(targetCombo);
				targetComboModel = new DefaultComboBoxModel(new Object[] {Button.a, Button.b, Button.x, Button.y, Button.up,
					Button.down, Button.left, Button.right, Button.leftShoulder, Button.rightShoulder, Button.leftStick,
					Button.rightStick, Button.start, Button.back, Button.guide, Axis.leftStickX, Axis.leftStickY, Axis.rightStickX,
					Axis.rightStickY, Axis.leftTrigger, Axis.rightTrigger});
				targetCombo.setModel(targetComboModel);
			}
		}
		{
			JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 6));
			titlePanel.add(panel, new GridBagConstraints(1, 5, 2, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
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
			triggerText = new JTextField();
			titlePanel.add(triggerText, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, new Insets(0, 6, 6, 6), 0, 0));
			triggerText.setEditable(false);
		}
		{
			JPanel spacer = new JPanel();
			titlePanel.add(spacer, new GridBagConstraints(1, 5, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(0, 0, 0, 0), 0, 0));
			spacer.setMinimumSize(new Dimension(375, 0));
			spacer.setMaximumSize(new Dimension(375, 0));
			spacer.setPreferredSize(new Dimension(375, 0));
		}
		{
			JLabel label = new JLabel("Key modifier:");
			titlePanel.add(label, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(0, 6, 6, 0), 0, 0));
		}
		{
			JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
			titlePanel.add(panel, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(0, 0, 6, 6), 0, 0));
			{
				ctrlCheckBox = new JCheckBox("Control");
				panel.add(ctrlCheckBox);
			}
			{
				altCheckBox = new JCheckBox("Alt");
				panel.add(altCheckBox);
			}
			{
				shiftCheckBox = new JCheckBox("Shift");
				panel.add(shiftCheckBox);
			}
		}

		ButtonGroup group = new ButtonGroup();
		group.add(targetRadio);
		group.add(scriptRadio);
	}
}
