
package com.esotericsoftware.controller.ui.swing;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;

import com.esotericsoftware.controller.device.Deadzone;
import com.esotericsoftware.controller.ui.InputTrigger;
import com.esotericsoftware.controller.util.Util;

public class InputDeadzoneDialog extends JDialog {
	private final UI owner;
	private final InputTrigger trigger;

	private JComboBox shapeCombo;
	private JSpinner xSpinner, ySpinner;
	private JButton saveButton, cancelButton;

	public InputDeadzoneDialog (UI owner, InputTrigger trigger) {
		super(owner, "Input Deadzone", true);
		this.owner = owner;
		this.trigger = trigger;

		initializeLayout();
		initializeEvents();

		setLocationRelativeTo(owner);

		Deadzone deadzone = trigger.getDeadzone();
		if (deadzone != null) {
			xSpinner.setValue(deadzone.getSizeX());
			ySpinner.setValue(deadzone.getSizeY());
			shapeCombo.setSelectedIndex(deadzone instanceof Deadzone.Square ? 0 : 1);
		}
	}

	private void initializeEvents () {
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				dispose();
			}
		});

		saveButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				Deadzone deadzone = shapeCombo.getSelectedIndex() == 0 ? new Deadzone.Square() : new Deadzone.Round();
				deadzone.setSizeX((Float)xSpinner.getValue());
				deadzone.setSizeY((Float)ySpinner.getValue());
				if (deadzone.getSizeX() == 0 && deadzone.getSizeY() == 0) deadzone = null;
				trigger.setDeadzone(deadzone);
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
				new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0,
					0), 0, 0));
			{
				JLabel label = new JLabel("Shape:");
				panel.add(label, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(6, 6, 0, 6), 0, 0));
			}
			{
				shapeCombo = new JComboBox();
				panel.add(shapeCombo, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(6, 0, 0, 6), 0, 0));
				shapeCombo.setModel(new DefaultComboBoxModel(new Object[] {"Square", "Round"}));
			}
			{
				JLabel label = new JLabel("X axis:");
				panel.add(label, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(6, 6, 0, 6), 0, 0));
			}
			{
				xSpinner = new JSpinner();
				panel.add(xSpinner, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL, new Insets(6, 0, 0, 6), 0, 0));
				xSpinner.setModel(Util.newFloatSpinnerModel(0, 0, 1, 0.01f));
			}
			{
				JLabel label = new JLabel("Y axis:");
				panel.add(label, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(6, 6, 6, 6), 0, 0));
			}
			{
				ySpinner = new JSpinner();
				panel.add(ySpinner, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL, new Insets(6, 0, 6, 6), 0, 0));
				ySpinner.setModel(Util.newFloatSpinnerModel(0, 0, 1, 0.01f));
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
					cancelButton = new JButton();
					buttonPanel.add(cancelButton);
					cancelButton.setText("Cancel");
				}
				{
					saveButton = new JButton();
					buttonPanel.add(saveButton);
					saveButton.setText("Save");
				}
			}
		}
		pack();
		setSize(225, getHeight());
	}
}
