
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

public class DeadzoneDialog extends JFrame {
	private JPanel panel;
	private JPanel panel2;
	private JPanel xAxisPanel;
	private JLabel iconLabel;
	private JLabel jLabel1;
	private JLabel headerLabel;
	private JButton cancelButton;
	private JPanel jPanel1;
	private JPanel panel3;

	public DeadzoneDialog () {
		super("Deadzone");

		initializeLayout();
	}

	private void initializeLayout () {
		setSize(new Dimension(566, 376));
		GridBagLayout thisLayout = new GridBagLayout();
		getContentPane().setLayout(thisLayout);
		{
			panel = new JPanel();
			GridBagLayout panelLayout = new GridBagLayout();
			getContentPane().add(
				panel,
				new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0,
					0), 0, 0));
			panel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new java.awt.Color(0, 0, 0)));
			panel.setBackground(Color.white);
			panel.setLayout(panelLayout);
			{
				headerLabel = new JLabel();
				panel.add(headerLabel, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(0, 6, 6, 0), 0, 0));
				headerLabel.setText("Wizard step text.");
			}
			{
				jLabel1 = new JLabel();
				panel.add(jLabel1, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(6, 6, 6, 0), 0, 0));
				jLabel1.setText("Step Title");
				jLabel1.setFont(jLabel1.getFont().deriveFont(Font.BOLD));
			}
			{
				iconLabel = new JLabel("icon");
				panel.add(iconLabel, new GridBagConstraints(1, 0, 1, 2, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
					new Insets(6, 6, 6, 6), 0, 0));
			}
		}
		{
			panel2 = new JPanel();
			CardLayout panel2Layout = new CardLayout();
			panel2.setLayout(panel2Layout);
			getContentPane().add(
				panel2,
				new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0,
					0), 0, 0));
			{
				xAxisPanel = new JPanel();
				panel2.add(xAxisPanel, "jPanel2");
			}
		}
		{
			panel3 = new JPanel();
			GridBagLayout panel3Layout = new GridBagLayout();
			getContentPane().add(
				panel3,
				new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0,
					0), 0, 0));
			panel3.setLayout(panel3Layout);
			panel3.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new java.awt.Color(0, 0, 0)));
			{
				jPanel1 = new JPanel();
				FlowLayout jPanel1Layout = new FlowLayout();
				jPanel1Layout.setVgap(6);
				jPanel1Layout.setHgap(6);
				jPanel1.setLayout(jPanel1Layout);
				panel3.add(jPanel1, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
					new Insets(0, 0, 0, 0), 0, 0));
				{
					cancelButton = new JButton();
					jPanel1.add(cancelButton);
					cancelButton.setText("Cancel");
				}
			}
		}
	}
}
