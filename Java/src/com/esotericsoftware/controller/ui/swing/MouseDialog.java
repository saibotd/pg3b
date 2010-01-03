
package com.esotericsoftware.controller.ui.swing;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import com.esotericsoftware.controller.ui.Config;
import com.esotericsoftware.controller.ui.MouseTranslation;
import com.esotericsoftware.controller.util.Util;
import com.esotericsoftware.controller.xim.XIMMouseTranslation;

public class MouseDialog extends JDialog {
	private final UI owner;
	private Runnable saveRunnable;
	private MouseTranslation translation;

	private JSpinner yxRatioSpinner, smoothnessSpinner, diagonalDampenSpinner, sensitivitySpinner, translationExponentSpinner;
	private JButton saveButton, cancelButton;

	public MouseDialog (UI owner, MouseTranslation translation) {
		super(owner, "Mouse Translation", true);
		this.owner = owner;

		if (translation == null) translation = new XIMMouseTranslation();
		this.translation = translation;

		initializeLayout();
		initializeEvents();

		setLocationRelativeTo(owner);

		if (translation instanceof XIMMouseTranslation) {
			XIMMouseTranslation ximTranslation = (XIMMouseTranslation)translation;
			yxRatioSpinner.setValue(ximTranslation.getYXRatio());
			smoothnessSpinner.setValue(ximTranslation.getSmoothness());
			diagonalDampenSpinner.setValue(ximTranslation.getDiagonalDampen());
			sensitivitySpinner.setValue(ximTranslation.getSensitivity());
			translationExponentSpinner.setValue(ximTranslation.getTranslationExponent());
		}
	}

	public void setSaveRunnable (Runnable saveRunnable) {
		this.saveRunnable = saveRunnable;
	}

	public MouseTranslation getMouseTranslation () {
		return translation;
	}

	private void initializeEvents () {
		saveButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				if (translation instanceof XIMMouseTranslation) {
					XIMMouseTranslation ximTranslation = (XIMMouseTranslation)translation;
					ximTranslation.setYXRatio((Float)yxRatioSpinner.getValue());
					ximTranslation.setSmoothness((Float)smoothnessSpinner.getValue());
					ximTranslation.setDiagonalDampen((Float)diagonalDampenSpinner.getValue());
					ximTranslation.setSensitivity((Integer)sensitivitySpinner.getValue());
					ximTranslation.setTranslationExponent((Float)translationExponentSpinner.getValue());
				}
				if (saveRunnable != null) saveRunnable.run();
				dispose();
			}
		});

		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
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
				new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(6, 6, 0,
					6), 0, 0));
			{
				JLabel label = new JLabel("YX ratio:");
				panel.add(label, new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(0, 0, 0, 6), 0, 0));
			}
			{
				yxRatioSpinner = new JSpinner();
				panel.add(yxRatioSpinner, new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL, new Insets(0, 0, 6, 0), 0, 0));
				yxRatioSpinner.setModel(Util.newFloatSpinnerModel(1f, -3, 3, 0.05f));
			}
			{
				translationExponentSpinner = new JSpinner();
				panel.add(translationExponentSpinner, new GridBagConstraints(1, 9, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL, new Insets(0, 0, 6, 0), 0, 0));
				translationExponentSpinner.setModel(Util.newFloatSpinnerModel(0.75f, -2, 2, 0.05f));
			}
			{
				sensitivitySpinner = new JSpinner();
				panel.add(sensitivitySpinner, new GridBagConstraints(1, 10, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL, new Insets(0, 0, 6, 0), 0, 0));
				sensitivitySpinner.setModel(new SpinnerNumberModel(1250, 1, 99999, 1));
			}
			{
				JLabel label = new JLabel("Translation exponent:");
				panel.add(label, new GridBagConstraints(0, 9, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(0, 0, 0, 6), 0, 0));
			}
			{
				diagonalDampenSpinner = new JSpinner();
				panel.add(diagonalDampenSpinner, new GridBagConstraints(1, 11, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL, new Insets(0, 0, 6, 0), 0, 0));
				diagonalDampenSpinner.setModel(Util.newFloatSpinnerModel(0f, 0, 1, 0.05f));
			}
			{
				smoothnessSpinner = new JSpinner();
				panel.add(smoothnessSpinner, new GridBagConstraints(1, 12, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL, new Insets(0, 0, 6, 0), 0, 0));
				smoothnessSpinner.setModel(Util.newFloatSpinnerModel(0.3f, 0, 1, 0.05f));
			}
			{
				JLabel label = new JLabel("Sensitivity:");
				panel.add(label, new GridBagConstraints(0, 10, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(0, 0, 0, 6), 0, 0));
			}
			{
				JLabel label = new JLabel("Diagonal dampen:");
				panel.add(label, new GridBagConstraints(0, 11, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(0, 0, 0, 6), 0, 0));
			}
			{
				JLabel label = new JLabel("Smoothness:");
				panel.add(label, new GridBagConstraints(0, 12, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(0, 0, 0, 6), 0, 0));
			}
		}
		{
			JPanel panel = new JPanel(new GridBagLayout());
			getContentPane().add(
				panel,
				new GridBagConstraints(0, 3, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0,
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
	}
}
