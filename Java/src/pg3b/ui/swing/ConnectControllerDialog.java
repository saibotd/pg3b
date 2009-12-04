
package pg3b.ui.swing;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.java.games.input.Controller;
import pg3b.XboxController;
import pg3b.util.UI;

public class ConnectControllerDialog extends JDialog {
	PG3BUI owner;
	JList controllerList;
	DefaultComboBoxModel controllerListModel;
	private JButton connectButton, cancelButton;

	public ConnectControllerDialog (PG3BUI owner) {
		super(owner, "Connect to Controller", true);
		this.owner = owner;

		initializeLayout();
		initializeEvents();

		for (Controller controller : XboxController.getAllControllers())
			controllerListModel.addElement(controller);
	}

	private void initializeEvents () {
		connectButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				owner.setController(new XboxController((Controller)controllerList.getSelectedValue()));
				dispose();
			}
		});

		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				dispose();
			}
		});

		UI.enableWhenModelHasSelection(controllerList.getSelectionModel(), connectButton);
	}

	private void initializeLayout () {
		setSize(360, 250);
		setLocationRelativeTo(getOwner());

		getContentPane().setLayout(new GridBagLayout());
		{
			JScrollPane scroll = new JScrollPane();
			getContentPane().add(
				scroll,
				new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(6, 6, 0,
					6), 0, 0));
			{
				controllerList = new JList();
				scroll.setViewportView(controllerList);
				controllerListModel = new DefaultComboBoxModel();
				controllerList.setModel(controllerListModel);
			}
		}
		{
			JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 6));
			getContentPane().add(
				panel,
				new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(0, 0, 0, 0), 0, 0));
			{
				cancelButton = new JButton("Cancel");
				panel.add(cancelButton);
			}
			{
				connectButton = new JButton("Connect");
				panel.add(connectButton);
			}
		}
		{
			JLabel label = new JLabel("<html>Please choose the Xbox controller for the PG3B.");
			getContentPane().add(
				label,
				new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(6, 6, 0,
					6), 0, 0));
		}
	}
}
