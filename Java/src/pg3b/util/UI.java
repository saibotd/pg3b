
package pg3b.util;

import java.awt.Component;

import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class UI {
	static public void enableWhenModelHasSelection (final ListSelectionModel selectionModel, final Component... components) {
		selectionModel.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged (ListSelectionEvent event) {
				if (event.getValueIsAdjusting()) return;
				setEnabled(selectionModel.getMinSelectionIndex() != -1, components);
			}
		});
		setEnabled(selectionModel.getMinSelectionIndex() != -1, components);
	}

	static public void errorDialog (Component parent, String title, String message) {
		JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
	}

	static public void setEnabled (boolean enabled, Component... components) {
		for (Component component : components)
			component.setEnabled(enabled);
	}
}
