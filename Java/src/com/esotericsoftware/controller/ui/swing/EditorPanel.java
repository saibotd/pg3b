
package com.esotericsoftware.controller.ui.swing;

import static com.esotericsoftware.minlog.Log.*;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.esotericsoftware.controller.ui.Editable;
import com.esotericsoftware.controller.ui.Settings;
import com.esotericsoftware.controller.util.DirectoryMonitor;
import com.esotericsoftware.controller.util.Util;
import com.esotericsoftware.minlog.Log;

public class EditorPanel<T extends Editable> extends JPanel {
	static Settings settings = Settings.get();

	protected UI owner;

	private Class<T> type;
	private File rootDir;
	private String extension;
	private DirectoryMonitor<T> monitor;
	private T selectedItem;
	private boolean isListAdjusting;

	private JList list;
	private DefaultComboBoxModel listModel;
	private JTextField nameText;
	private JPanel contentPanel;
	private JButton newButton, deleteButton;
	private JTextArea descriptionText;

	public EditorPanel () {
	}

	public EditorPanel (UI owner, final Class<T> type, File rootDir, String extension) {
		this.owner = owner;
		this.type = type;
		this.rootDir = rootDir;
		this.extension = extension;

		initializeLayout();
		initializeEvents();

		monitor = new DirectoryMonitor<T>(extension) {
			protected T load (File file) throws IOException {
				try {
					T newItem = type.newInstance();
					newItem.load(file);
					return newItem;
				} catch (Exception ex) {
					throw new IOException("Error creating new item: " + file, ex);
				}
			}

			protected void updated () {
				EventQueue.invokeLater(new Runnable() {
					public void run () {
						Component focused = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
						T selectedItem = getSelectedItem();
						isListAdjusting = true;
						listModel.removeAllElements();
						for (T item : getItems())
							listModel.addElement(item);
						isListAdjusting = false;
						setSelectedFile(selectedItem == null ? null : selectedItem.getFile());
						if (focused != null) focused.requestFocus();
					}
				});
			}
		};
		monitor.scan(rootDir, 3000);

		getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke('S', InputEvent.CTRL_MASK), "save");
		getActionMap().put("save", new AbstractAction() {
			public void actionPerformed (ActionEvent event) {
				saveItem(false);
			}
		});
	}

	/**
	 * @param item If null, clear the fields.
	 */
	protected void updateFieldsFromItem (T item) {
	}

	protected void updateItemFromFields (T item) {
	}

	protected void itemRenamed (T oldItem, T newItem) {
	}

	/**
	 * Due to the DirectoryMonitor, an item will be removed and then selected again when saved. Subclasses should store state (eg,
	 * scrollbar positions) between selections and clear the state only when clearItemSpecificState is called.
	 */
	protected void clearItemSpecificState () {
	}

	protected JPopupMenu getPopupMenu () {
		return null;
	}

	public JPanel getContentPanel () {
		return contentPanel;
	}

	public T getSelectedItem () {
		return selectedItem;
	}

	public void setSelectedItem (T item) {
		list.setSelectedValue(item, true);
	}

	public void setSelectedFile (File file) {
		for (int i = 0, n = listModel.getSize(); i < n; i++) {
			T listItem = (T)listModel.getElementAt(i);
			if (listItem.getFile().equals(file)) {
				isListAdjusting = true;
				list.clearSelection();
				isListAdjusting = false;
				list.setSelectedIndex(i);
				return;
			}
		}
		list.clearSelection();
	}

	public List<T> getItems () {
		return monitor.getItems();
	}

	public ListSelectionModel getSelectionModel () {
		return list.getSelectionModel();
	}

	public T newItem () {
		List<T> items = monitor.getItems();
		int i = 1;
		String name;
		outer: // 
		while (true) {
			name = "New " + type.getSimpleName();
			if (i > 1) name += " (" + i + ")";
			i++;
			for (T item : items)
				if (item.getName().equalsIgnoreCase(name)) continue outer;
			break;
		}
		T item;
		try {
			item = type.getConstructor(File.class).newInstance(new File(rootDir, name + extension));
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		nameText.setText(item.getName());
		descriptionText.setText(item.getDescription());
		updateFieldsFromItem(item);
		saveItem(item, true);
		owner.getStatusBar().setMessage(type.getSimpleName() + " created.");
		return item;
	}

	private void initializeEvents () {
		list.addMouseListener(new MouseAdapter() {
			public void mousePressed (MouseEvent event) {
				showPopup(event);
			}

			public void mouseReleased (MouseEvent event) {
				showPopup(event);
			}

			private void showPopup (MouseEvent event) {
				if (!event.isPopupTrigger() || getSelectedItem() == null) return;
				JPopupMenu popupMenu = getPopupMenu();
				if (popupMenu == null) return;
				popupMenu.show(list, event.getX(), event.getY());
			}
		});

		list.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged (ListSelectionEvent event) {
				if (event.getValueIsAdjusting() || isListAdjusting) return;
				T item = (T)list.getSelectedValue();
				if (item == null) {
					selectedItem = null;
					nameText.setText("");
					descriptionText.setText("");
				} else {
					// Update GUI even if items are equal but not the same, to load changes made directly to the selected item.
					if (item == selectedItem) return;
					boolean clearItemSpecificState = selectedItem == null || !selectedItem.getFile().equals(item.getFile());
					if (clearItemSpecificState) clearItemSpecificState();
					selectedItem = item;
					nameText.setText(item.getName());
					descriptionText.setText(item.getDescription());
					if (clearItemSpecificState) descriptionText.setCaretPosition(0);
				}
				updateFieldsFromItem(item);
			}
		});

		newButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				newItem();
			}
		});

		deleteButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				if (JOptionPane.showConfirmDialog(owner,
					"Are you sure you want to delete the selected item?\nThis action cannot be undone.", "Confirm Delete",
					JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
				if (getSelectedItem().getFile().delete())
					owner.getStatusBar().setMessage(type.getSimpleName() + " deleted.");
				else
					owner.getStatusBar().setMessage(type.getSimpleName() + " could not be deleted.");
				monitor.scan(rootDir);
				if (list.getSelectedIndex() == -1 && listModel.getSize() > 0) list.setSelectedIndex(0);
			}
		});

		FocusAdapter focusListener = new FocusAdapter() {
			public void focusLost (FocusEvent event) {
				saveItem(false);
			}
		};
		nameText.addFocusListener(focusListener);
		descriptionText.addFocusListener(focusListener);
	}

	public void saveItem (boolean force) {
		saveItem(getSelectedItem(), force);
	}

	public void saveItem (T item, boolean force) {
		Component focused = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
		synchronized (monitor) {
			if (item == null) return;
			if (!force && !item.getFile().exists()) return;

			T oldItem = (T)item.clone();

			if (!force) {
				// Rename file if needed.
				String name = new File(nameText.getText().trim()).getName();
				if (name.length() == 0) name = item.getName();
				if (!name.equalsIgnoreCase(item.getName())) {
					File newFile = new File(item.getFile().getParent(), name + extension);
					if (!newFile.exists()) {
						item.getFile().renameTo(newFile);
						try {
							item.load(newFile);
						} catch (IOException ex) {
							if (Log.ERROR) error("Unable to load file: " + item.getFile(), ex);
							Util.errorDialog(owner, "Error", //
								"An error occurred while attempting to load the file.");
							return;
						}
						itemRenamed(oldItem, item);
					}
				}
			}

			item.setDescription(descriptionText.getText());
			updateItemFromFields(item);

			if (!force && oldItem.equals(item)) return;
			try {
				item.save(new FileWriter(item.getFile()));
				monitor.scan(rootDir);
				owner.getStatusBar().setMessage(type.getSimpleName() + " saved.");
			} catch (IOException ex) {
				owner.getStatusBar().setMessage("Unable to save file.");
				if (Log.ERROR) error("Unable to save file: " + item.getFile(), ex);
				Util.errorDialog(owner, "Error", //
					"An error occurred while attempting to save the file.");
				return;
			}
			setSelectedFile(item.getFile());
		}
		if (focused != null) focused.requestFocus();
	}

	private void initializeLayout () {
		setLayout(new GridBagLayout());
		{
			JScrollPane scroll = new JScrollPane();
			scroll.setMinimumSize(new Dimension(150, 3));
			scroll.setMaximumSize(new Dimension(150, 3));
			scroll.setPreferredSize(new Dimension(150, 3));
			add(scroll, new GridBagConstraints(0, 0, 1, 1, 0.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(
				6, 6, 0, 12), 0, 0));
			{
				list = new JList();
				scroll.setViewportView(list);
				listModel = new DefaultComboBoxModel();
				list.setModel(listModel);
				list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				list.addMouseListener(new MouseAdapter() {
					public void mousePressed (MouseEvent event) {
						if (event.getButton() == 1) return;
						list.setSelectedIndex(list.locationToIndex(event.getPoint()));
					}
				});
			}
		}
		{
			JPanel panel = new JPanel(new GridLayout(1, 1, 6, 6));
			add(panel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(6,
				6, 6, 12), 0, 0));
			{
				deleteButton = new JButton("Delete");
				panel.add(deleteButton);
			}
			{
				newButton = new JButton("New");
				panel.add(newButton);
			}
		}
		{
			JPanel rightPanel = new JPanel(new GridBagLayout());
			add(rightPanel, new GridBagConstraints(1, 0, 1, 2, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				new Insets(0, 0, 0, 0), 0, 0));
			{
				JPanel panel = new JPanel(new GridBagLayout());
				rightPanel.add(panel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
					GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
				{
					panel.add(new JLabel("Name:"), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
						GridBagConstraints.NONE, new Insets(6, 6, 0, 6), 0, 0));
				}
				{
					nameText = new JTextField();
					panel.add(nameText, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
						GridBagConstraints.HORIZONTAL, new Insets(6, 0, 0, 6), 0, 0));
				}
				{
					JLabel label = new JLabel("Description:");
					panel.add(label, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHEAST,
						GridBagConstraints.NONE, new Insets(6, 6, 0, 6), 0, 0));
				}
				{
					JScrollPane scroll = new JScrollPane();
					panel.add(scroll, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
						GridBagConstraints.HORIZONTAL, new Insets(6, 0, 0, 6), 0, 0));
					scroll.setMinimumSize(new Dimension(3, 62));
					scroll.setMaximumSize(new Dimension(3, 62));
					scroll.setPreferredSize(new Dimension(3, 62));
					scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
					{
						descriptionText = new JTextArea();
						descriptionText.setLineWrap(true);
						descriptionText.setWrapStyleWord(true);
						scroll.setViewportView(descriptionText);
					}
				}
			}
			{
				contentPanel = new JPanel(new GridBagLayout());
				rightPanel.add(contentPanel, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
					GridBagConstraints.BOTH, new Insets(6, 0, 0, 6), 0, 0));
			}
		}

		Util.enableWhenModelHasSelection(list.getSelectionModel(), deleteButton, nameText, descriptionText);
	}
}
