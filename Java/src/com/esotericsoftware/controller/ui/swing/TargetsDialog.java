
package com.esotericsoftware.controller.ui.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.esotericsoftware.controller.device.Axis;
import com.esotericsoftware.controller.device.Button;
import com.esotericsoftware.controller.ui.Config;

public class TargetsDialog extends JDialog {
	private final UI owner;
	private final Config config;

	private JTextField aText, bText;
	private JTextField xText, yText;
	private JTextField leftTriggerText, rightTriggerText;
	private JTextField leftShoulderText, rightShoulderText;
	private JTextField leftStickText, rightStickText;
	private JTextField upText, downText;
	private JTextField leftText, rightText;
	private JTextField startText, backText;
	private JButton saveButton, cancelButton;

	public TargetsDialog (UI owner, Config config) {
		super(owner, "Targets", true);
		this.owner = owner;
		this.config = config;

		initializeLayout();
		initializeEvents();

		setLocationRelativeTo(owner);

		Map<String, String> names = config.getTargetNames();
		aText.setText(names.get(Button.a.name()));
		bText.setText(names.get(Button.b.name()));
		xText.setText(names.get(Button.x.name()));
		yText.setText(names.get(Button.y.name()));
		leftTriggerText.setText(names.get(Axis.leftTrigger.name()));
		rightTriggerText.setText(names.get(Axis.rightTrigger.name()));
		leftShoulderText.setText(names.get(Button.leftShoulder.name()));
		rightShoulderText.setText(names.get(Button.rightShoulder.name()));
		leftStickText.setText(names.get(Button.leftStick.name()));
		rightStickText.setText(names.get(Button.rightStick.name()));
		upText.setText(names.get(Button.up.name()));
		downText.setText(names.get(Button.down.name()));
		leftText.setText(names.get(Button.left.name()));
		rightText.setText(names.get(Button.right.name()));
		startText.setText(names.get(Button.start.name()));
		backText.setText(names.get(Button.back.name()));
	}

	private void initializeEvents () {
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				dispose();
			}
		});

		saveButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				LinkedHashMap names = new LinkedHashMap();
				names.put(Button.a.name(), aText.getText());
				names.put(Button.b.name(), bText.getText());
				names.put(Button.x.name(), xText.getText());
				names.put(Button.y.name(), yText.getText());
				names.put(Axis.leftTrigger.name(), leftTriggerText.getText());
				names.put(Axis.rightTrigger.name(), rightTriggerText.getText());
				names.put(Button.leftShoulder.name(), leftShoulderText.getText());
				names.put(Button.rightShoulder.name(), rightShoulderText.getText());
				names.put(Button.leftStick.name(), leftStickText.getText());
				names.put(Button.rightStick.name(), rightStickText.getText());
				names.put(Button.up.name(), upText.getText());
				names.put(Button.down.name(), downText.getText());
				names.put(Button.left.name(), leftText.getText());
				names.put(Button.right.name(), rightText.getText());
				names.put(Button.start.name(), startText.getText());
				names.put(Button.back.name(), backText.getText());
				config.setTargetNames(names);
				owner.getConfigTab().getConfigEditor().saveItem(true);
				dispose();
			}
		});
	}

	private void initializeLayout () {
		getContentPane().setLayout(new GridBagLayout());
		{
			JPanel panel = new JPanel(new GridBagLayout());
			getContentPane().add(
				panel,
				new GridBagConstraints(0, 0, 2, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0,
					0), 0, 0));
			panel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0, 0, 0)));
			panel.setBackground(Color.white);
			{
				JTextArea label = new JTextArea();
				panel.add(label, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH,
					new Insets(6, 6, 6, 6), 0, 0));
				label.setLineWrap(true);
				label.setWrapStyleWord(true);
				label.setEditable(false);
				label.setFocusable(false);
				label.setBorder(BorderFactory.createEmptyBorder());
				label.setText("The device's buttons and axes can be renamed to reflect their purpose in a specific game. ");
			}
		}
		{
			JPanel panel = new JPanel(new GridBagLayout());
			getContentPane().add(
				panel,
				new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0,
					0), 0, 0));
			{
				JLabel label = new JLabel("A button:");
				panel.add(label, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(6, 6, 0, 0), 0, 0));
			}
			{
				aText = newTextField();
				panel.add(aText, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
					new Insets(6, 6, 0, 6), 0, 0));
			}
			{
				bText = newTextField();
				panel.add(bText, new GridBagConstraints(3, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
					new Insets(6, 6, 0, 6), 0, 0));
			}
			{
				JLabel label = new JLabel("B button");
				panel.add(label, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(6, 6, 0, 0), 0, 0));
			}
			{
				xText = newTextField();
				panel.add(xText, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
					new Insets(6, 6, 0, 6), 0, 0));
			}
			{
				JLabel label = new JLabel("X button:");
				panel.add(label, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(6, 6, 0, 0), 0, 0));
			}
			{
				yText = newTextField();
				panel.add(yText, new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
					new Insets(6, 6, 0, 6), 0, 0));
			}
			{
				JLabel label = new JLabel("Y button:");
				panel.add(label, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(6, 6, 0, 0), 0, 0));
			}
			{
				leftTriggerText = newTextField();
				panel.add(leftTriggerText, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL, new Insets(6, 6, 0, 6), 0, 0));
			}
			{
				JLabel label = new JLabel("Left trigger axis:");
				panel.add(label, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(6, 6, 0, 0), 0, 0));
			}
			{
				rightTriggerText = newTextField();
				panel.add(rightTriggerText, new GridBagConstraints(3, 4, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL, new Insets(6, 6, 0, 6), 0, 0));
			}
			{
				JLabel label = new JLabel("Right trigger axis:");
				panel.add(label, new GridBagConstraints(2, 4, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(6, 6, 0, 0), 0, 0));
			}
			{
				leftShoulderText = newTextField();
				panel.add(leftShoulderText, new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL, new Insets(6, 6, 0, 6), 0, 0));
			}
			{
				JLabel label = new JLabel("Left shoulder button:");
				panel.add(label, new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(6, 6, 0, 0), 0, 0));
			}
			{
				rightShoulderText = newTextField();
				panel.add(rightShoulderText, new GridBagConstraints(3, 6, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL, new Insets(6, 6, 0, 6), 0, 0));
			}
			{
				JLabel label = new JLabel("Right shoulder button:");
				panel.add(label, new GridBagConstraints(2, 6, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(6, 6, 0, 0), 0, 0));
			}
			{
				leftStickText = newTextField();
				panel.add(leftStickText, new GridBagConstraints(1, 8, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL, new Insets(6, 6, 0, 6), 0, 0));
			}
			{
				JLabel label = new JLabel("Left stick button:");
				panel.add(label, new GridBagConstraints(0, 8, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(6, 6, 0, 0), 0, 0));
			}
			{
				rightStickText = newTextField();
				panel.add(rightStickText, new GridBagConstraints(3, 8, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL, new Insets(6, 6, 0, 6), 0, 0));
			}
			{
				JLabel label = new JLabel("Right stick button:");
				panel.add(label, new GridBagConstraints(2, 8, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(6, 6, 0, 0), 0, 0));
			}
			{
				upText = newTextField();
				panel.add(upText, new GridBagConstraints(1, 9, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL, new Insets(6, 6, 0, 6), 0, 0));
			}
			{
				JLabel label = new JLabel("Up button:");
				panel.add(label, new GridBagConstraints(0, 9, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(6, 6, 0, 0), 0, 0));
			}
			{
				downText = newTextField();
				panel.add(downText, new GridBagConstraints(3, 9, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL, new Insets(6, 6, 0, 6), 0, 0));
			}
			{
				JLabel label = new JLabel("Down button:");
				panel.add(label, new GridBagConstraints(2, 9, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(6, 6, 0, 0), 0, 0));
			}
			{
				leftText = newTextField();
				panel.add(leftText, new GridBagConstraints(1, 10, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL, new Insets(6, 6, 0, 6), 0, 0));
			}
			{
				JLabel label = new JLabel("Left button:");
				panel.add(label, new GridBagConstraints(0, 10, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(6, 6, 0, 0), 0, 0));
			}
			{
				rightText = newTextField();
				panel.add(rightText, new GridBagConstraints(3, 10, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL, new Insets(6, 6, 0, 6), 0, 0));
			}
			{
				JLabel label = new JLabel("Right button:");
				panel.add(label, new GridBagConstraints(2, 10, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(6, 6, 0, 0), 0, 0));
			}
			{
				startText = newTextField();
				panel.add(startText, new GridBagConstraints(1, 11, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL, new Insets(6, 6, 6, 6), 0, 0));
			}
			{
				JLabel label = new JLabel("Start button:");
				panel.add(label, new GridBagConstraints(0, 11, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(6, 6, 6, 0), 0, 0));
			}
			{
				backText = newTextField();
				panel.add(backText, new GridBagConstraints(3, 11, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL, new Insets(6, 6, 6, 6), 0, 0));
			}
			{
				JLabel label = new JLabel("Back button:");
				panel.add(label, new GridBagConstraints(2, 11, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(6, 6, 0, 0), 0, 0));
			}
		}
		{
			JPanel panel = new JPanel(new GridBagLayout());
			getContentPane().add(
				panel,
				new GridBagConstraints(0, 3, 2, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0,
					0), 0, 0));
			panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0, 0, 0)));
			{
				JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 6));
				panel.add(buttonPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(0, 0, 0, 0), 0, 0));
				{
					cancelButton = new JButton("Cancel");
					buttonPanel.add(cancelButton);
				}
				{
					saveButton = new JButton("Save");
					buttonPanel.add(saveButton);
				}
			}
		}
		pack();
		setSize(560, getHeight());
	}

	private JTextField newTextField () {
		return new JTextField() {
			public Dimension getPreferredSize () {
				Dimension size = super.getPreferredSize();
				size.width = 0;
				return size;
			}
		};
	}
}
