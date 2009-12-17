
package pg3b.ui.swing;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

public class DeadzoneDialog extends JFrame {
	private JRadioButton circleRadio, squareRadio;
	private JSpinner xSpinner, ySpinner;
	private JPanel xAxisPanel, yAxisPanel;
	private JLabel titleLabel, messageLabel, iconLabel;
	private JButton cancelButton;
	private CardLayout cardLayout;

	public DeadzoneDialog () {
		super("Deadzone");

		initializeLayout();
	}

	private void initializeLayout () {
		setSize(new Dimension(566, 376));
		getContentPane().setLayout(new GridBagLayout());
		{
			JPanel panel = new JPanel(new GridBagLayout());
			getContentPane().add(
				panel,
				new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0,
					0), 0, 0));
			panel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0, 0, 0)));
			panel.setBackground(Color.white);
			{
				messageLabel = new JLabel("Wizard step text.");
				panel.add(messageLabel, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.WEST,
					GridBagConstraints.NONE, new Insets(0, 6, 6, 0), 0, 0));
			}
			{
				titleLabel = new JLabel("Step Title");
				panel.add(titleLabel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(6, 6, 6, 0), 0, 0));
				titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
			}
			{
				iconLabel = new JLabel("icon");
				panel.add(iconLabel, new GridBagConstraints(1, 0, 1, 2, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
					new Insets(6, 6, 6, 6), 0, 0));
			}
		}
		{
			cardLayout = new CardLayout();
			JPanel panel = new JPanel(cardLayout);
			getContentPane().add(
				panel,
				new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0,
					0), 0, 0));
			{
				JPanel shapePanel = new JPanel(new GridBagLayout());
				panel.add(shapePanel, "shape");
				{
					JLabel label = new JLabel("Deadzone shape:");
					shapePanel.add(label, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
						GridBagConstraints.NONE, new Insets(6, 6, 6, 6), 0, 0));
				}
				{
					circleRadio = new JRadioButton("Circle");
					shapePanel.add(circleRadio, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
						GridBagConstraints.NONE, new Insets(6, 0, 6, 6), 0, 0));
				}
				{
					squareRadio = new JRadioButton("Square");
					shapePanel.add(squareRadio, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
						GridBagConstraints.NONE, new Insets(6, 0, 6, 6), 0, 0));
				}
			}
			{
				xAxisPanel = new JPanel(new GridBagLayout());
				panel.add(xAxisPanel, "xaxis");
				{
					JLabel label = new JLabel("X deadzone:");
					xAxisPanel.add(label, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
						GridBagConstraints.NONE, new Insets(6, 6, 6, 0), 0, 0));
				}
				{
					xSpinner = new JSpinner();
					xAxisPanel.add(xSpinner, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
						GridBagConstraints.NONE, new Insets(6, 6, 6, 6), 0, 0));
					SpinnerNumberModel model = new SpinnerNumberModel(0, 0, 255, 1);
					xSpinner.setModel(model);
				}
			}
			{
				yAxisPanel = new JPanel(new GridBagLayout());
				panel.add(yAxisPanel, "yaxis");
				{
					JLabel label = new JLabel("X deadzone:");
					yAxisPanel.add(label, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
						GridBagConstraints.NONE, new Insets(6, 6, 6, 0), 0, 0));
				}
				{
					ySpinner = new JSpinner();
					yAxisPanel.add(ySpinner, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
						GridBagConstraints.NONE, new Insets(6, 6, 6, 6), 0, 0));
					SpinnerNumberModel model = new SpinnerNumberModel(0, 0, 255, 1);
					xSpinner.setModel(model);
				}
			}
		}
		{
			JPanel panel = new JPanel(new GridBagLayout());
			getContentPane().add(
				panel,
				new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0,
					0), 0, 0));
			panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0, 0, 0)));
			{
				JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 6));
				panel.add(buttonPanel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
					GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
				{
					cancelButton = new JButton();
					buttonPanel.add(cancelButton);
					cancelButton.setText("Cancel");
				}
			}
		}
	}
}
