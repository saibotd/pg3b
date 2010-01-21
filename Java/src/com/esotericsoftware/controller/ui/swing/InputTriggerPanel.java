
package com.esotericsoftware.controller.ui.swing;

import java.awt.AWTEvent;
import java.awt.Component;
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
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import com.esotericsoftware.controller.device.Axis;
import com.esotericsoftware.controller.device.Button;
import com.esotericsoftware.controller.device.Target;
import com.esotericsoftware.controller.input.Input;
import com.esotericsoftware.controller.input.InputDevice;
import com.esotericsoftware.controller.input.JInputJoystick;
import com.esotericsoftware.controller.input.Keyboard;
import com.esotericsoftware.controller.input.Mouse;
import com.esotericsoftware.controller.input.XboxController;
import com.esotericsoftware.controller.input.Mouse.MouseInput;
import com.esotericsoftware.controller.ui.Action;
import com.esotericsoftware.controller.ui.Config;
import com.esotericsoftware.controller.ui.DeviceAction;
import com.esotericsoftware.controller.ui.InputTrigger;
import com.esotericsoftware.controller.ui.MouseAction;
import com.esotericsoftware.controller.ui.MouseTranslation;
import com.esotericsoftware.controller.ui.Script;
import com.esotericsoftware.controller.ui.ScriptAction;
import com.esotericsoftware.controller.ui.TextModeAction;
import com.esotericsoftware.controller.ui.DeviceAction.Direction;
import com.esotericsoftware.controller.ui.swing.XboxControllerPanel.Listener;
import com.esotericsoftware.controller.util.Util;
import com.esotericsoftware.controller.xim.XIM2MouseTranslation;

public class InputTriggerPanel extends JPanel {
	private UI owner;
	private Config config;
	private InputTrigger trigger;
	private boolean isNewTrigger;
	private TimerTask monitorControllersTask;
	private float startMouseX, startMouseY;
	private Target highlighted;
	MouseTranslation translation;

	private JPanel titlePanel, axisButtonPanel, targetPanel;
	private JLabel triggerLabel;
	private JRadioButton targetRadio, scriptRadio, mouseRadio, textModeRadio;
	private JButton saveButton, cancelButton, deadzoneButton, mouseButton;
	private JComboBox targetCombo, targetDirectionCombo, scriptCombo;
	private JCheckBox altCheckBox, ctrlCheckBox, shiftCheckBox, anyCheckBox, noneCheckBox, invertTriggerCheckBox;
	private DefaultComboBoxModel scriptComboModel, targetComboModel, targetDirectionComboModel;

	private Listener controllerPanelListener = new Listener() {
		public void axisChanged (Axis axis, float state) {
			if (!isVisible()) return;
			if (Math.abs(state) > 0.25f) {
				targetCombo.setSelectedItem(new TargetItem(axis));
				targetDirectionCombo.setSelectedIndex(state < 0 ? 0 : 1);
			}
		}

		public void buttonChanged (Button button, boolean pressed) {
			if (!isVisible()) return;
			if (pressed) targetCombo.setSelectedItem(new TargetItem(button));
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

		targetComboModel.removeAllElements();
		Target[] targets = new Target[] {Button.a, Button.b, Button.x, Button.y, Button.up, Button.down, Button.left, Button.right,
			Axis.leftTrigger, Axis.rightTrigger, Button.leftShoulder, Button.rightShoulder, Axis.leftStickX, Axis.leftStickY,
			Axis.rightStickX, Axis.rightStickY, Button.leftStick, Button.rightStick, Button.start, Button.back, Button.guide};
		for (Target target : targets)
			targetComboModel.addElement(new TargetItem(target, config.getTargetName(target)));

		Util.setEnabled(true, targetRadio, targetCombo, scriptRadio, scriptCombo, mouseRadio, mouseButton, textModeRadio);

		scriptComboModel.removeAllElements();
		scriptComboModel.addElement("<New Script>");
		for (Script script : owner.getScriptEditor().getItems())
			scriptComboModel.addElement(script.getName());

		if (trigger == null) {
			// New trigger.
			this.trigger = new InputTrigger();
			isNewTrigger = true;
			titlePanel.setBorder(BorderFactory.createTitledBorder("New Trigger"));

			translation = new XIM2MouseTranslation();

			triggerLabel.setText("Click to set trigger...");
			triggerLabel.setFont(triggerLabel.getFont().deriveFont(Font.ITALIC));
			targetRadio.setSelected(true);
			shiftCheckBox.setSelected(false);
			ctrlCheckBox.setSelected(false);
			altCheckBox.setSelected(false);
			anyCheckBox.setSelected(true);
			noneCheckBox.setSelected(false);
			invertTriggerCheckBox.setSelected(false);

			saveButton.setEnabled(false);
		} else {
			// Edit trigger.
			this.trigger = trigger;
			isNewTrigger = false;
			titlePanel.setBorder(BorderFactory.createTitledBorder("Edit Trigger"));

			triggerLabel.setFont(triggerLabel.getFont().deriveFont(Font.PLAIN));
			shiftCheckBox.setSelected(trigger.getShift());
			ctrlCheckBox.setSelected(trigger.getCtrl());
			altCheckBox.setSelected(trigger.getAlt());
			anyCheckBox.setSelected(false);
			noneCheckBox.setSelected(false);
			if (!trigger.getShift() && !trigger.getCtrl() && !trigger.getAlt()) {
				anyCheckBox.setSelected(!trigger.getNoModifiers());
				noneCheckBox.setSelected(trigger.getNoModifiers());
			}
			invertTriggerCheckBox.setSelected(trigger.getInvert());
			Input input = trigger.getInput();
			axisButtonPanel.setVisible(input.isAxis() && !(input instanceof MouseInput));
			setTriggerText(trigger);

			Action action = trigger.getAction();
			if (action instanceof ScriptAction) {
				scriptRadio.setSelected(true);
				scriptCombo.setSelectedItem(((ScriptAction)action).getScriptName());
			} else if (action instanceof MouseAction) {
				mouseRadio.doClick();
				translation = ((MouseAction)action).getMouseTranslation();
			} else if (action instanceof TextModeAction) {
				textModeRadio.doClick();
			} else if (action instanceof DeviceAction) {
				DeviceAction deviceAction = ((DeviceAction)action);
				targetRadio.setSelected(true);
				Target target = deviceAction.getTarget();
				if (target != null) targetCombo.setSelectedItem(new TargetItem(target));
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
				Util.setEnabled(false, targetRadio, targetCombo, scriptRadio, scriptCombo, invertTriggerCheckBox, mouseRadio,
					mouseButton, textModeRadio);
				targetRadio.setSelected(false);
				scriptRadio.setSelected(false);
			}

			saveButton.setEnabled(true);
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
					if (input instanceof MouseInput && ((MouseInput)input).isAxis()) continue;
					float value = input.getState();
					if (Math.abs(value) < 0.25f) continue;
					return input;
				}
				if (Math.abs(startMouseX - Mouse.instance.getX()) > 25) return new MouseInput("x");
				if (Math.abs(startMouseY - Mouse.instance.getY()) > 25) return new MouseInput("y");
				return null;
			}

			public void run () {
				final Input input = getLastInput();
				if (input == null) return;
				trigger.setInput(input);
				SwingUtilities.invokeLater(new Runnable() {
					public void run () {
						setTriggerText(trigger);
						triggerLabel.setFont(triggerLabel.getFont().deriveFont(Font.PLAIN));
						cancelButton.setEnabled(true);
						saveButton.setEnabled(true);
						axisButtonPanel.setVisible(input.isAxis() && !(input instanceof MouseInput));
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

		Target target = getSelectedTarget();
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

	private Target getSelectedTarget () {
		TargetItem item = (TargetItem)targetCombo.getSelectedItem();
		if (item == null) return null;
		return item.target;
	}

	private void setHighlighted (Target target, int directionIndex) {
		XboxControllerPanel controllerPanel = owner.getControllerPanel();
		if (highlighted != null) controllerPanel.setHighlighted(highlighted, 0);
		highlighted = null;
		if (target == null) return;
		highlighted = target;
		if (highlighted != null) {
			if (targetDirectionCombo.isVisible())
				controllerPanel.setHighlighted(highlighted, directionIndex == 0 ? -1 : 1);
			else {
				controllerPanel.setHighlighted(highlighted, 1);
				controllerPanel.setHighlighted(highlighted, -1);
			}
		}
	}

	private void initializeEvents () {
		mouseButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				mouseRadio.setSelected(true);
				targetCombo.setSelectedItem(null);
				scriptCombo.setSelectedItem(null);
				new MouseDialog(owner, translation).setVisible(true);
			}
		});

		// Highlight controller target.
		MouseAdapter targetHighlightListener = new MouseAdapter() {
			public void mouseEntered (MouseEvent event) {
				setHighlighted(getSelectedTarget(), targetDirectionCombo.getSelectedIndex());
			}

			public void mouseExited (MouseEvent event) {
				setHighlighted(null, 0);
			}
		};
		targetCombo.addMouseListener(targetHighlightListener);
		for (Component component : targetCombo.getComponents())
			component.addMouseListener(targetHighlightListener);
		targetDirectionCombo.addMouseListener(targetHighlightListener);
		for (Component component : targetDirectionCombo.getComponents())
			component.addMouseListener(targetHighlightListener);
		targetRadio.addMouseListener(targetHighlightListener);
		for (Component component : targetRadio.getComponents())
			component.addMouseListener(targetHighlightListener);
		targetPanel.addMouseListener(targetHighlightListener);

		targetCombo.setRenderer(new DefaultListCellRenderer() {
			public Component getListCellRendererComponent (JList list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {
				if (isSelected) setHighlighted(((TargetItem)value).target, targetDirectionCombo.getSelectedIndex());
				return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			}
		});

		targetDirectionCombo.setRenderer(new DefaultListCellRenderer() {
			public Component getListCellRendererComponent (JList list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {
				if (isSelected) setHighlighted(getSelectedTarget(), index);
				return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			}
		});

		PopupMenuListener highlightPopupListener = new PopupMenuListener() {
			public void popupMenuWillBecomeVisible (PopupMenuEvent e) {
			}

			public void popupMenuWillBecomeInvisible (PopupMenuEvent e) {
				setHighlighted(null, 0);
			}

			public void popupMenuCanceled (PopupMenuEvent e) {
			}
		};
		targetCombo.addPopupMenuListener(highlightPopupListener);
		targetDirectionCombo.addPopupMenuListener(highlightPopupListener);
		//

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

		mouseRadio.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				if (!mouseRadio.isSelected()) return;
				targetCombo.setSelectedItem(null);
				scriptCombo.setSelectedItem(null);
				updateTargetDirection();
			}
		});

		textModeRadio.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				if (!textModeRadio.isSelected()) return;
				targetCombo.setSelectedItem(null);
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
				startMouseX = Mouse.instance.getX();
				startMouseY = Mouse.instance.getY();
				listenForTrigger(true);
				cancelButton.setEnabled(false);
				saveButton.setEnabled(false);
			}
		});

		deadzoneButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				new InputDeadzoneDialog(owner, trigger).setVisible(true);
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

				trigger.setCtrl(ctrlCheckBox.isSelected());
				trigger.setAlt(altCheckBox.isSelected());
				trigger.setShift(shiftCheckBox.isSelected());
				trigger.setNoModifiers(noneCheckBox.isSelected());
				trigger.setInvert(invertTriggerCheckBox.isSelected());
				if (targetRadio.isSelected()) {
					DeviceAction action = new DeviceAction(getSelectedTarget());
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
				} else if (mouseRadio.isSelected()) {
					MouseAction action = new MouseAction();
					action.setMouseTranslation(translation);
					trigger.setAction(action);
				} else if (textModeRadio.isSelected()) {
					trigger.setAction(new TextModeAction());
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
			JLabel label = new JLabel("Trigger:");
			titlePanel.add(label, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(0, 6, 6, 0), 0, 0));
		}
		{
			JLabel label = new JLabel("Action:");
			titlePanel.add(label, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHEAST,
				GridBagConstraints.NONE, new Insets(4, 6, 6, 0), 0, 0));
		}
		{
			JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
			titlePanel.add(panel, new GridBagConstraints(2, 6, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(0, 0, 6, 6), 0, 0));
			{
				mouseRadio = new JRadioButton("Mouse Translation");
				panel.add(mouseRadio);
			}
			{
				mouseButton = new JButton("Edit");
				panel.add(mouseButton);
			}
		}
		{
			textModeRadio = new JRadioButton("Text Mode");
			titlePanel.add(textModeRadio, new GridBagConstraints(2, 7, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
				GridBagConstraints.NONE, new Insets(0, 6, 6, 6), 0, 0));
		}
		{
			JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
			titlePanel.add(panel, new GridBagConstraints(2, 5, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
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
			targetPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
			titlePanel.add(targetPanel, new GridBagConstraints(2, 4, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
				GridBagConstraints.NONE, new Insets(0, 0, 6, 6), 0, 0));
			{
				targetRadio = new JRadioButton("Device");
				targetPanel.add(targetRadio);
			}
			{
				targetCombo = new JComboBox();
				targetPanel.add(targetCombo);
				targetComboModel = new DefaultComboBoxModel();
				targetCombo.setModel(targetComboModel);
			}
			{
				targetDirectionCombo = new JComboBox();
				targetPanel.add(targetDirectionCombo);
				targetDirectionComboModel = new DefaultComboBoxModel();
				targetDirectionComboModel.addElement("Left");
				targetDirectionComboModel.addElement("Right");
				targetDirectionCombo.setModel(targetDirectionComboModel);
				targetDirectionCombo.setVisible(false);
			}
		}
		{
			JPanel bottomPanel = new JPanel(new GridBagLayout());
			titlePanel.add(bottomPanel, new GridBagConstraints(1, 8, 2, 1, 0.0, 0.0, GridBagConstraints.EAST,
				GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
			{
				JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
				bottomPanel.add(panel, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
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
				axisButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
				axisButtonPanel.setVisible(false);
				bottomPanel.add(axisButtonPanel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
					GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
				axisButtonPanel.setVisible(false);
				{
					deadzoneButton = new JButton("Deadzone");
					axisButtonPanel.add(deadzoneButton);
				}
				{
					invertTriggerCheckBox = new JCheckBox("Invert axis");
					axisButtonPanel.add(invertTriggerCheckBox);
				}
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
			triggerLabel.setBorder(new JTextField().getBorder());
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
			titlePanel.add(label, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
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
		group.add(mouseRadio);
		group.add(textModeRadio);
	}

	static private class TargetItem {
		public final Target target;
		public final String name;

		public TargetItem (Target target, String name) {
			this.target = target;
			this.name = name;
		}

		public TargetItem (Target target) {
			this.target = target;
			this.name = null;
		}

		public int hashCode () {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((target == null) ? 0 : target.hashCode());
			return result;
		}

		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			TargetItem other = (TargetItem)obj;
			if (target == null) {
				if (other.target != null) return false;
			} else if (!target.equals(other.target)) return false;
			return true;
		}

		public String toString () {
			return name;
		}
	}
}
