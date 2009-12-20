
package pg3b.ui.swing;

import static com.esotericsoftware.minlog.Log.*;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import pg3b.Axis;
import pg3b.PG3B;

public class DeadzoneDialog extends JDialog {
	private PG3B pg3b;

	private JComboBox leftShapeCombo, rightShapeCombo;
	private JSpinner leftXSpinner, leftYSpinner, rightXSpinner, rightYSpinner;
	private JButton saveButton;
	private JPanel leftStickPanel, rightStickPanel;

	public DeadzoneDialog (Frame owner, PG3B pg3b) {
		super(owner, "Deadzones", true);

		this.pg3b = pg3b;

		initializeLayout();
		initializeEvents();

		setLocationRelativeTo(owner);
	}

	private void initializeEvents () {
		leftXSpinner.addChangeListener(new ChangeListener() {
			public void stateChanged (ChangeEvent event) {
				setAxis(Axis.leftStickX, leftXSpinner);
			}
		});
		leftYSpinner.addChangeListener(new ChangeListener() {
			public void stateChanged (ChangeEvent event) {
				setAxis(Axis.leftStickY, leftYSpinner);
			}
		});
		rightXSpinner.addChangeListener(new ChangeListener() {
			public void stateChanged (ChangeEvent event) {
				setAxis(Axis.rightStickX, rightXSpinner);
			}
		});
		rightYSpinner.addChangeListener(new ChangeListener() {
			public void stateChanged (ChangeEvent event) {
				setAxis(Axis.rightStickY, rightYSpinner);
			}
		});

		saveButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				// BOZO - Save deadzones.
				dispose();
			}
		});
	}

	private void setAxis (Axis axis, JSpinner spinner) {
		JPanel panel = (spinner == leftXSpinner || spinner == leftYSpinner) ? leftStickPanel : rightStickPanel;
		String title = (spinner == leftXSpinner || spinner == leftYSpinner) ? "Left Stick" : "Right Stick";
		try {
			pg3b.set(axis, (Integer)spinner.getValue() / 255f);
			panel.setBorder(BorderFactory.createTitledBorder(title));
		} catch (IOException ex) {
			if (DEBUG) debug("Error setting axis: " + axis, ex);
			panel.setBorder(BorderFactory.createTitledBorder(title + " (error)"));
		}
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
				label
					.setText("Slowly increase the value for each axis below. As soon as the game responds, decrease the value by one.");
			}
		}
		{
			JPanel panel = new JPanel(new GridBagLayout());
			getContentPane().add(
				panel,
				new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0,
					0), 0, 0));
			{
				leftStickPanel = new JPanel(new GridBagLayout());
				panel.add(leftStickPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
					GridBagConstraints.BOTH, new Insets(6, 6, 6, 0), 0, 0));
				leftStickPanel.setBorder(BorderFactory.createTitledBorder("Left Stick"));
				{
					JLabel label = new JLabel("Shape:");
					leftStickPanel.add(label, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
						GridBagConstraints.NONE, new Insets(6, 6, 12, 0), 0, 0));
				}
				{
					leftShapeCombo = new JComboBox();
					leftStickPanel.add(leftShapeCombo, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
						GridBagConstraints.NONE, new Insets(0, 6, 6, 6), 0, 0));
					leftShapeCombo.setModel(new DefaultComboBoxModel(new String[] {"Round", "Square"}));
				}
				{
					JLabel label = new JLabel("X axis:");
					leftStickPanel.add(label, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
						GridBagConstraints.NONE, new Insets(6, 6, 0, 0), 0, 0));
				}
				{
					leftXSpinner = new JSpinner();
					leftStickPanel.add(leftXSpinner, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
						GridBagConstraints.NONE, new Insets(6, 6, 0, 6), 0, 0));
					leftXSpinner.setModel(new SpinnerNumberModel(0, -255, 255, 1));
				}
				{
					JLabel label = new JLabel("Y axis:");
					leftStickPanel.add(label, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
						GridBagConstraints.NONE, new Insets(6, 6, 4, 0), 0, 0));
				}
				{
					leftYSpinner = new JSpinner();
					leftStickPanel.add(leftYSpinner, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
						GridBagConstraints.NONE, new Insets(6, 6, 4, 6), 0, 0));
					leftYSpinner.setModel(new SpinnerNumberModel(0, -255, 255, 1));
				}
			}
			{
				rightStickPanel = new JPanel(new GridBagLayout());
				panel.add(rightStickPanel, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
					GridBagConstraints.NONE, new Insets(6, 6, 6, 6), 0, 0));
				rightStickPanel.setBorder(BorderFactory.createTitledBorder("Right Stick"));
				{
					JLabel label = new JLabel("Shape:");
					rightStickPanel.add(label, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
						GridBagConstraints.NONE, new Insets(6, 6, 12, 0), 0, 0));
				}
				{
					rightShapeCombo = new JComboBox();
					rightStickPanel.add(rightShapeCombo, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
						GridBagConstraints.NONE, new Insets(0, 6, 6, 6), 0, 0));
					rightShapeCombo.setModel(new DefaultComboBoxModel(new String[] {"Round", "Square"}));
				}
				{
					JLabel label = new JLabel("X axis:");
					rightStickPanel.add(label, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
						GridBagConstraints.NONE, new Insets(6, 6, 0, 0), 0, 0));
				}
				{
					rightXSpinner = new JSpinner();
					rightStickPanel.add(rightXSpinner, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
						GridBagConstraints.NONE, new Insets(6, 6, 0, 6), 0, 0));
					rightXSpinner.setModel(new SpinnerNumberModel(0, -255, 255, 1));
				}
				{
					JLabel label = new JLabel("Y axis:");
					rightStickPanel.add(label, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
						GridBagConstraints.NONE, new Insets(6, 6, 4, 0), 0, 0));
				}
				{
					rightYSpinner = new JSpinner();
					rightStickPanel.add(rightYSpinner, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
						GridBagConstraints.NONE, new Insets(6, 6, 4, 6), 0, 0));
					rightYSpinner.setModel(new SpinnerNumberModel(0, -255, 255, 1));
				}
			}
		}
		{
			JLabel label = new JLabel("Tip: Click in a spinner, then use the up and down keys to change the value.");
			label.setFont(label.getFont().deriveFont(Font.ITALIC));
			getContentPane().add(
				label,
				new GridBagConstraints(0, 2, 2, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 6, 6,
					6), 0, 0));
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
					saveButton = new JButton();
					buttonPanel.add(saveButton);
					saveButton.setText("Save");
				}
			}
		}
		pack();
	}
}
