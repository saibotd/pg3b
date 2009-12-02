
package pg3b.util;

import java.awt.Component;

import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class UI {
	static public void enableWhenListHasSelection (final JList list, final Component... components) {
		list.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged (ListSelectionEvent event) {
				if (event.getValueIsAdjusting()) return;
				boolean enabled = list.getSelectedIndex() != -1;
				for (Component component : components)
					component.setEnabled(enabled);
			}
		});
		boolean enabled = list.getSelectedIndex() != -1;
		for (Component component : components)
			component.setEnabled(enabled);
	}

	public static void errorDialog (Component parent, String title, String message) {
		JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
	}
}
