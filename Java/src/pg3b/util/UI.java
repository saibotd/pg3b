
package pg3b.util;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JViewport;
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

	public static void scrollRowToVisisble (JTable table, int index) {
		JViewport viewport = (JViewport)table.getParent();
		Rectangle rect = table.getCellRect(index, 0, true);
		Point position = viewport.getViewPosition();
		rect.setLocation(rect.x - position.x, rect.y - position.y);
		viewport.scrollRectToVisible(rect);
	}
}
