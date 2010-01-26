
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

import com.esotericsoftware.controller.ui.DefaultMouseTranslation;
import com.esotericsoftware.controller.ui.MouseTranslation;
import com.esotericsoftware.controller.util.Util;
import com.esotericsoftware.controller.xim.XIM1;
import com.esotericsoftware.controller.xim.XIM1MouseTranslation;
import com.esotericsoftware.controller.xim.XIM2;
import com.esotericsoftware.controller.xim.XIM2MouseTranslation;

public class MouseDialog extends JDialog {
	private final UI owner;
	private Runnable saveRunnable;
	private MouseTranslation translation;

	private JSpinner yxRatioSpinner, smoothnessSpinner, diagonalDampenSpinner, sensitivitySpinner, translationExponentSpinner;
	private JComboBox translationCombo;
	private DefaultComboBoxModel translationComboModel;
	private JButton saveButton, cancelButton;

	public MouseDialog (UI owner, MouseTranslation translation) {
		super(owner, "Mouse Translation", true);
		this.owner = owner;

		if (translation == null) {
			if (XIM2.isValid(false))
				translation = new XIM2MouseTranslation();
			else if (XIM1.isValid(false))
				translation = new XIM1MouseTranslation();
			else
				translation = new DefaultMouseTranslation();
		}
		this.translation = translation;

		initializeLayout();
		initializeEvents();

		setLocationRelativeTo(owner);

		if (translation instanceof XIM2MouseTranslation) {
			XIM2MouseTranslation ximTranslation = (XIM2MouseTranslation)translation;
			yxRatioSpinner.setValue(ximTranslation.getYXRatio());
			smoothnessSpinner.setValue(ximTranslation.getSmoothness());
			diagonalDampenSpinner.setValue(ximTranslation.getDiagonalDampen());
			sensitivitySpinner.setValue((float)ximTranslation.getSensitivity());
			translationExponentSpinner.setValue(ximTranslation.getTranslationExponent());
		} else if (translation instanceof XIM1MouseTranslation) {
			XIM1MouseTranslation ximTranslation = (XIM1MouseTranslation)translation;
			yxRatioSpinner.setValue(ximTranslation.getYXRatio());
			smoothnessSpinner.setValue(ximTranslation.getSmoothness());
			sensitivitySpinner.setValue(ximTranslation.getSensitivity());
			translationExponentSpinner.setValue(ximTranslation.getTranslationExponent());
			diagonalDampenSpinner.setEnabled(false);
		} else {
			yxRatioSpinner.setEnabled(false);
			smoothnessSpinner.setEnabled(false);
			sensitivitySpinner.setEnabled(false);
			translationExponentSpinner.setEnabled(false);
			diagonalDampenSpinner.setEnabled(false);
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
				if (translation instanceof XIM2MouseTranslation) {
					XIM2MouseTranslation ximTranslation = (XIM2MouseTranslation)translation;
					ximTranslation.setYXRatio((Float)yxRatioSpinner.getValue());
					ximTranslation.setSmoothness((Float)smoothnessSpinner.getValue());
					ximTranslation.setDiagonalDampen((Float)diagonalDampenSpinner.getValue());
					ximTranslation.setSensitivity(((Float)sensitivitySpinner.getValue()).intValue());
					ximTranslation.setTranslationExponent((Float)translationExponentSpinner.getValue());
				} else if (translation instanceof XIM1MouseTranslation) {
					XIM1MouseTranslation ximTranslation = (XIM1MouseTranslation)translation;
					ximTranslation.setYXRatio((Float)yxRatioSpinner.getValue());
					ximTranslation.setSmoothness((Float)smoothnessSpinner.getValue());
					ximTranslation.setSensitivity((Float)sensitivitySpinner.getValue());
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
			JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 6));
			getContentPane().add(
				panel,
				new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0,
					0, 0, 0), 0, 0));
			panel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0, 0, 0)));
			{
				JLabel label = new JLabel("Translation:");
				panel.add(label);
			}
			{
				translationComboModel = new DefaultComboBoxModel();
				translationCombo = new JComboBox();
				panel.add(translationCombo);
				translationCombo.setModel(translationComboModel);
			}
		}
		{
			JPanel panel = new JPanel(new GridBagLayout());
			getContentPane().add(
				panel,
				new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(6, 6, 0,
					6), 0, 0));
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
