
package com.esotericsoftware.controller.ui.swing;

import java.awt.AWTEvent;
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
import java.util.ArrayList;
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

import com.esotericsoftware.controller.device.Axis;
import com.esotericsoftware.controller.device.Button;
import com.esotericsoftware.controller.device.Target;
import com.esotericsoftware.controller.input.Input;
import com.esotericsoftware.controller.input.InputDevice;
import com.esotericsoftware.controller.input.JInputJoystick;
import com.esotericsoftware.controller.input.Keyboard;
import com.esotericsoftware.controller.input.Mouse;
import com.esotericsoftware.controller.input.XboxController;
import com.esotericsoftware.controller.ui.Action;
import com.esotericsoftware.controller.ui.Config;
import com.esotericsoftware.controller.ui.DeviceAction;
import com.esotericsoftware.controller.ui.InputTrigger;
import com.esotericsoftware.controller.ui.Script;
import com.esotericsoftware.controller.ui.ScriptAction;
import com.esotericsoftware.controller.ui.DeviceAction.Direction;
import com.esotericsoftware.controller.ui.swing.XboxControllerPanel.Listener;
import com.esotericsoftware.controller.util.Util;

public class InputTriggerPanel extends JPanel {
	private UI owner;
	private Config config;
	private InputTrigger trigger;
	private boolean isNewTrigger;
	private TimerTask monitorControllersTask;

	private JPanel titlePanel;
	private JLabel triggerLabel;
	private JRadioButton targetRadio, scriptRadio;
	private JButton saveButton, cancelButton;
	private JTextField descriptionText;
	private JComboBox targetCombo, targetDirectionCombo, scriptCombo;
	private JCheckBox altCheckBox, ctrlCheckBox, shiftCheckBox, anyCheckBox, noneCheckBox;
	private DefaultComboBoxModel scriptComboModel, targetComboModel, targetDirectionComboModel;

	private Listener controllerPanelListener = new Listener() {
		public void axisChanged (Axis axis, float state) {
			if (!isVisible()) return;
			if (Math.abs(state) > 0.25f) {
				targetCombo.setSelectedItem(axis);
				targetDirectionCombo.setSelectedIndex(state < 0 ? 0 : 1);
			}
		}

		public void buttonChanged (Button button, boolean pressed) {
			if (!isVisible()) return;
			if (pressed) targetCombo.setSelectedItem(button);
		}
	};

	public InputTriggerPanel (UI owner) {
		this.owner = owner;

		initializeLayout();
		initializeEvents();

		owner.getControllerPanel().addListener(controllerPanelListener);
	}

	public void setTrigger (Config config, InputTrigger trigger) {
		this.config = config;

		Util.setEnabled(true, targetRadio, targetCombo, scriptRadio, scriptCombo);

		scriptComboModel.removeAllElements();
		scriptComboModel.addElement("<New Script>");
		for (Script script : owner.getScriptEditor().getItems())
			scriptComboModel.addElement(script.getName());

		if (trigger == null) {
			// New trigger.
			this.trigger = new InputTrigger();
			isNewTrigger = true;
			titlePanel.setBorder(BorderFactory.createTitledBorder("New Trigger"));

			triggerLabel.setText("Click to set trigger...");
			triggerLabel.setFont(triggerLabel.getFont().deriveFont(Font.ITALIC));
			descriptionText.setText("");
			targetRadio.setSelected(true);
			shiftCheckBox.setSelected(false);
			ctrlCheckBox.setSelected(false);
			altCheckBox.setSelected(false);
			anyCheckBox.setSelected(true);
			noneCheckBox.setSelected(false);

			saveButton.setEnabled(false);
		} else {
			// Edit trigger.
			this.trigger = trigger;
			isNewTrigger = false;
			titlePanel.setBorder(BorderFactory.createTitledBorder("Edit Trigger"));

			triggerLabel.setFont(triggerLabel.getFont().deriveFont(Font.PLAIN));
			descriptionText.setText(trigger.getDescription());
			shiftCheckBox.setSelected(trigger.getShift());
			ctrlCheckBox.setSelected(trigger.getCtrl());
			altCheckBox.setSelected(trigger.getAlt());
			anyCheckBox.setSelected(false);
			noneCheckBox.setSelected(false);
			if (!trigger.getShift() && !trigger.getCtrl() && !trigger.getAlt()) {
				anyCheckBox.setSelected(!trigger.getNoModifiers());
				noneCheckBox.setSelected(trigger.getNoModifiers());
			}
			setTriggerText(trigger);

			Action action = trigger.getAction();
			if (action instanceof ScriptAction) {
				scriptRadio.setSelected(true);
				scriptCombo.setSelectedItem(((ScriptAction)action).getScriptName());
			} else if (action instanceof DeviceAction) {
				DeviceAction deviceAction = ((DeviceAction)action);
				targetRadio.setSelected(true);
				Target target = deviceAction.getTarget();
				if (target != null) targetCombo.setSelectedItem(target);
				Direction direction = deviceAction.getDirection();
				switch (direction) {
				case up:
				case left:
					targetDirectionCombo.setSelectedIndex(0);
					break;
				case down:
				case right:
					targetDirectionCombo.setSelectedIndex(1);
					break;
				}
			} else {
				// Unknown action, can't change it.
				Util.setEnabled(false, targetRadio, targetCombo, scriptRadio, scriptCombo);
				targetRadio.setSelected(false);
				scriptRadio.setSelected(false);
			}
		}
	}

	void listenForTrigger (boolean enable) {
		if (monitorControllersTask != null) {
			monitorControllersTask.cancel();
			monitorControllersTask = null;
		}
		if (!enable) return;
		final ArrayList<InputDevice> devices = new ArrayList();
		devices.add(Keyboard.instance);
		devices.add(Mouse.instance);
		devices.addAll(XboxController.getAll());
		devices.addAll(JInputJoystick.getAll());
		for (InputDevice device : devices)
			device.resetLastInput();
		monitorControllersTask = new TimerTask() {
			private Input getLastInput () {
				for (InputDevice device : devices) {
					if (!device.poll()) continue;
					Input input = device.getLastInput();
					if (input == null) continue;
					float value = input.getState();
					if (Math.abs(value) < 0.25f) continue;
					return input;
				}
				return null;
			}

			public void run () {
				Input input = getLastInput();
				if (input == null) return;
				trigger.setInput(input);
				SwingUtilities.invokeLater(new Runnable() {
					public void run () {
						setTriggerText(trigger);
						triggerLabel.setFont(triggerLabel.getFont().deriveFont(Font.PLAIN));
						cancelButton.setEnabled(true);
						saveButton.setEnabled(true);
						updateTargetDirection();
					}
				});
				listenForTrigger(false);
			}
		};
		Util.timer.scheduleAtFixedRate(monitorControllersTask, 125, 125);
	}

	public void setTriggerText (InputTrigger trigger) {
		if (trigger.getInput() == null)
			triggerLabel.setText("Click to set trigger...");
		else {
			boolean ctrl = trigger.getCtrl();
			boolean alt = trigger.getAlt();
			boolean shift = trigger.getShift();
			trigger.setCtrl(ctrlCheckBox.isSelected());
			trigger.setAlt(altCheckBox.isSelected());
			trigger.setShift(shiftCheckBox.isSelected());
			triggerLabel.setText(trigger.toString());
			trigger.setCtrl(ctrl);
			trigger.setAlt(alt);
			trigger.setShift(shift);
		}
	}

	private void updateTargetDirection () {
		targetDirectionCombo.setVisible(false);

		if (trigger == null) return;
		Input input = trigger.getInput();
		if (input == null || input.isAxis()) return;

		Target target = (Target)targetCombo.getSelectedItem();
		if (target instanceof Axis) {
			switch ((Axis)target) {
			case leftStickX:
			case rightStickX:
				targetDirectionCombo.setVisible(true);
				targetDirectionComboModel.removeAllElements();
				targetDirectionComboModel.addElement("Left");
				targetDirectionComboModel.addElement("Right");
				break;
			case leftStickY:
			case rightStickY:
				targetDirectionCombo.setVisible(true);
				targetDirectionComboModel.removeAllElements();
				targetDirectionComboModel.addElement("Up");
				targetDirectionComboModel.addElement("Down");
				break;
			}
		}
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
				updateTargetDirection();
			}
		});

		scriptRadio.addItemListener(new ItemListener() {
			public void itemStateChanged (ItemEvent event) {
				if (!scriptRadio.isSelected()) return;
				targetCombo.setSelectedItem(null);
				if (scriptCombo.getSelectedIndex() == -1) scriptCombo.setSelectedIndex(0);
				updateTargetDirection();
			}
		});

		targetRadio.addItemListener(new ItemListener() {
			public void itemStateChanged (ItemEvent event) {
				if (!targetRadio.isSelected()) return;
				scriptCombo.setSelectedItem(null);
				if (targetCombo.getSelectedIndex() == -1) targetCombo.setSelectedIndex(0);
			}
		});

		triggerLabel.addMouseListener(new MouseAdapter() {
			public void mousePressed (MouseEvent event) {
				if (monitorControllersTask != null) return;
				triggerLabel.setText("Waiting for trigger...");
				triggerLabel.setFont(triggerLabel.getFont().deriveFont(Font.ITALIC));
				triggerLabel.requestFocusInWindow();
				listenForTrigger(true);
				cancelButton.setEnabled(false);
				saveButton.setEnabled(false);
			}
		});

		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				owner.getConfigTab().showConfigEditor();
			}
		});

		saveButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				owner.getConfigTab().showConfigEditor();

				trigger.setDescription(descriptionText.getText());
				trigger.setCtrl(ctrlCheckBox.isSelected());
				trigger.setAlt(altCheckBox.isSelected());
				trigger.setShift(shiftCheckBox.isSelected());
				trigger.setNoModifiers(noneCheckBox.isSelected());
				if (targetRadio.isSelected()) {
					DeviceAction action = new DeviceAction((Target)targetCombo.getSelectedItem());
					trigger.setAction(action);
					if (targetDirectionCombo.isVisible())
						action.setDirection(Direction.valueOf(((String)targetDirectionCombo.getSelectedItem()).toLowerCase()));
					else
						action.setDirection(Direction.both);
				} else if (scriptRadio.isSelected()) {
					if (scriptCombo.getSelectedIndex() == 0)
						trigger.setAction(new ScriptAction(owner.getScriptEditor().newItem().getName()));
					else
						trigger.setAction(new ScriptAction((String)scriptCombo.getSelectedItem()));
				}

				if (isNewTrigger) config.getTriggers().add(trigger);
				owner.getConfigTab().getConfigEditor().saveItem(true);
				owner.getConfigTab().getConfigEditor().setSelectedTrigger(config.getTriggers().indexOf(trigger));
			}
		});

		ActionListener updateTriggerText = new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				anyCheckBox.setSelected(false);
				noneCheckBox.setSelected(false);
				if (!ctrlCheckBox.isSelected() && !altCheckBox.isSelected() && !shiftCheckBox.isSelected())
					anyCheckBox.setSelected(true);
				setTriggerText(trigger);
			}
		};
		ctrlCheckBox.addActionListener(updateTriggerText);
		altCheckBox.addActionListener(updateTriggerText);
		shiftCheckBox.addActionListener(updateTriggerText);

		anyCheckBox.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				noneCheckBox.setSelected(false);
				ctrlCheckBox.setSelected(false);
				altCheckBox.setSelected(false);
				shiftCheckBox.setSelected(false);
				if (!anyCheckBox.isSelected()) noneCheckBox.setSelected(true);
				setTriggerText(trigger);
			}
		});

		noneCheckBox.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				anyCheckBox.setSelected(false);
				ctrlCheckBox.setSelected(false);
				altCheckBox.setSelected(false);
				shiftCheckBox.setSelected(false);
				if (!noneCheckBox.isSelected()) anyCheckBox.setSelected(true);
				setTriggerText(trigger);
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
				targetRadio = new JRadioButton("Device");
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
			{
				targetDirectionCombo = new JComboBox();
				panel.add(targetDirectionCombo);
				targetDirectionComboModel = new DefaultComboBoxModel();
				targetDirectionComboModel.addElement("Left");
				targetDirectionComboModel.addElement("Right");
				targetDirectionCombo.setModel(targetDirectionComboModel);
				targetDirectionCombo.setVisible(false);
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
			triggerLabel = new JLabel() {
				{
					enableEvents(AWTEvent.MOUSE_WHEEL_EVENT_MASK);
				}
			};
			titlePanel.add(triggerLabel, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, new Insets(0, 6, 6, 6), 0, 0));
			triggerLabel.setOpaque(true);
			triggerLabel.setBorder(descriptionText.getBorder());
			triggerLabel.setFocusable(true);
		}
		{
			JPanel spacer = new JPanel();
			titlePanel.add(spacer, new GridBagConstraints(1, 5, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(0, 0, 0, 0), 0, 0));
			spacer.setMinimumSize(new Dimension(450, 0));
			spacer.setMaximumSize(new Dimension(450, 0));
			spacer.setPreferredSize(new Dimension(450, 0));
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
			{
				anyCheckBox = new JCheckBox("Any");
				panel.add(anyCheckBox);
			}
			{
				noneCheckBox = new JCheckBox("None");
				panel.add(noneCheckBox);
			}
		}

		ButtonGroup group = new ButtonGroup();
		group.add(targetRadio);
		group.add(scriptRadio);
	}
}
