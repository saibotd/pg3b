
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

import pg3b.util.UI;
import pg3b.xboxcontroller.XboxController;

public class ConnectControllerDialog extends JDialog {
	private PG3BUI owner;
	private JList controllerList;
	private DefaultComboBoxModel controllerListModel;
	private JButton refreshButton, connectButton, cancelButton;

	public ConnectControllerDialog (PG3BUI owner) {
		super(owner, "Connect to Controller", true);
		this.owner = owner;

		initializeLayout();
		initializeEvents();

		refresh();

		if (!UI.isWindows) refreshButton.setVisible(false);
	}

	private void refresh () {
		controllerListModel.removeAllElements();
		for (XboxController controller : XboxController.getControllers())
			controllerListModel.addElement(controller);
		if (controllerListModel.getSize() > 0) controllerList.setSelectedIndex(0);
	}

	private void initializeEvents () {
		connectButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				owner.setController((XboxController)controllerList.getSelectedValue());
				owner.getStatusBar().setMessage("Controller connected.");
				dispose();
			}
		});

		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				dispose();
			}
		});

		refreshButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				refresh();
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
				new GridBagConstraints(0, 1, 2, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(6, 6, 0,
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
				new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
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
				new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(6, 6, 0,
					6), 0, 0));
		}
		{
			refreshButton = new JButton("Refresh");
			getContentPane().add(
				refreshButton,
				new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(6, 6, 6,
					6), 0, 0));
		}
	}
}
