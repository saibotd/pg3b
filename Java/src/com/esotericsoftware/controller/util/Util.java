
package com.esotericsoftware.controller.util;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Timer;

import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class Util {
	static public final Timer timer = new Timer("UtilTimer", true);
	static public final boolean isWindows = System.getProperty("os.name").toLowerCase().contains("windows");

	static public void enableWhenModelHasSelection (final ListSelectionModel selectionModel, final Component... components) {
		enableWhenModelHasSelection(selectionModel, null, components);
	}

	static public void enableWhenModelHasSelection (final ListSelectionModel selectionModel, final Runnable runnable,
		final Component... components) {
		selectionModel.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged (ListSelectionEvent event) {
				if (event.getValueIsAdjusting()) return;
				setEnabled(selectionModel.getMinSelectionIndex() != -1, components);
				if (runnable != null) runnable.run();
			}
		});
		setEnabled(selectionModel.getMinSelectionIndex() != -1, components);
		if (runnable != null) runnable.run();
	}

	static public void errorDialog (Component parent, String title, String message) {
		JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
	}

	static public void setEnabled (boolean enabled, Component... components) {
		for (Component component : components)
			component.setEnabled(enabled);
	}

	static public void scrollRowToVisisble (JTable table, int index) {
		JViewport viewport = (JViewport)table.getParent();
		Rectangle rect = table.getCellRect(index, 0, true);
		Point position = viewport.getViewPosition();
		rect.setLocation(rect.x - position.x, rect.y - position.y);
		viewport.scrollRectToVisible(rect);
	}

	static public SpinnerNumberModel newFloatSpinnerModel (float value, float minimum, float maximum, float stepSize) {
		return new SpinnerNumberModel(new Float(value), new Float(minimum), new Float(maximum), new Float(stepSize));
	}
}
