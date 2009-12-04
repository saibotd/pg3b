
package pg3b.ui.swing;

import static com.esotericsoftware.minlog.Log.*;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import pg3b.ui.Editable;
import pg3b.ui.Settings;
import pg3b.util.DirectoryMonitor;
import pg3b.util.UI;

import com.esotericsoftware.minlog.Log;

// BOZO - Highlight triggers table for missing controllers, scripts, etc.

public class EditorPanel<T extends Editable> extends JPanel {
	static Settings settings = Settings.get();

	protected PG3BUI owner;

	private Class<T> type;
	private File rootDir;
	private String extension;
	private DirectoryMonitor<T> monitor;
	private T lastSelectedItem;

	private JList list;
	private DefaultComboBoxModel listModel;
	private JTextField nameText;
	private JPanel contentPanel;
	private JButton newButton, deleteButton;
	private JTextArea descriptionText;

	public EditorPanel () {
	}

	public EditorPanel (PG3BUI owner, final Class<T> type, File rootDir, String extension) {
		this.owner = owner;
		this.type = type;
		this.rootDir = rootDir;
		this.extension = extension;

		initializeLayout();
		initializeEvents();

		monitor = new DirectoryMonitor<T>(extension) {
			protected T load (File file) throws IOException {
				return Editable.load(file, type);
			}

			protected void updated () {
				T selectedItem = getSelectedItem();
				listModel.removeAllElements();
				for (T item : getItems())
					listModel.addElement(item);
				list.setSelectedValue(selectedItem, true);
			}
		};
		monitor.scan(rootDir, 3000);
	}

	/**
	 * @param item If null, clear the fields.
	 */
	protected void updateFieldsFromItem (T item) {
	}

	protected void updateItemFromFields (T item) {
	}

	/**
	 * Due to the DirectoryMonitor, an item will be removed and then selected again when saved. Subclasses should store state (eg,
	 * scrollbar positions) between selections and clear the state only when clearItemSpecificState is called.
	 */
	protected void clearItemSpecificState () {
	}

	public JPanel getContentPanel () {
		return contentPanel;
	}

	public T getSelectedItem () {
		return (T)list.getSelectedValue();
	}

	public void setSelectedItem (T item) {
		list.setSelectedValue(item, true);
	}

	public List<T> getItems () {
		return monitor.getItems();
	}

	public ListSelectionModel getSelectionModel () {
		return list.getSelectionModel();
	}

	private void initializeEvents () {
		list.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged (ListSelectionEvent event) {
				if (event.getValueIsAdjusting()) return;
				T item = getSelectedItem();
				if (item == null) {
					nameText.setText("");
					descriptionText.setText("");
				} else {
					if (lastSelectedItem == null || !lastSelectedItem.getFile().equals(item.getFile())) clearItemSpecificState();
					lastSelectedItem = item;
					nameText.setText(item.getName());
					descriptionText.setText(item.getDescription());
				}
				updateFieldsFromItem(item);
			}
		});

		newButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				List<T> items = monitor.getItems();
				int i = 0;
				String name;
				outer: // 
				while (true) {
					name = "New " + type.getSimpleName();
					if (i > 0) name += " (" + i + ")";
					i++;
					for (T item : items)
						if (item.getName().equalsIgnoreCase(name)) continue outer;
					break;
				}
				try {
					T item = type.getConstructor(File.class).newInstance(new File(rootDir, name + extension));
					updateFieldsFromItem(item);
					saveItem(item, true);
					owner.getStatusBar().setMessage(type.getSimpleName() + " created.");
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			}
		});

		deleteButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				if (JOptionPane.showConfirmDialog(owner,
					"Are you sure you want to delete the selected item?\nThis action cannot be undone.", "Confirm Delete",
					JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
				if (getSelectedItem().getFile().delete()) owner.getStatusBar().setMessage(type.getSimpleName() + " deleted.");
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
		synchronized (monitor) {
			if (item == null) return;
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
							item = Editable.load(newFile, type);
						} catch (IOException ex) {
							if (Log.ERROR) error("Unable to load file: " + item.getFile(), ex);
							UI.errorDialog(owner, "Error", //
								"An error occurred while attempting to load the file.");
							return;
						}
					}
				}
			}

			item.setDescription(descriptionText.getText());
			updateItemFromFields(item);

			if (!force && oldItem.equals(item)) return;
			try {
				item.save();
				monitor.scan(rootDir);
				owner.getStatusBar().setMessage(type.getSimpleName() + " saved.");
			} catch (IOException ex) {
				owner.getStatusBar().setMessage("Unable to save file.");
				if (Log.ERROR) error("Unable to save file: " + item.getFile(), ex);
				UI.errorDialog(owner, "Error", //
					"An error occurred while attempting to save the file.");
				return;
			}
			for (int i = 0, n = listModel.getSize(); i < n; i++) {
				T listItem = (T)listModel.getElementAt(i);
				if (listItem.getFile().equals(item.getFile())) {
					list.clearSelection();
					list.setSelectedIndex(i);
					break;
				}
			}
		}
	}

	private void initializeLayout () {
		setLayout(new GridBagLayout());
		{
			JScrollPane scroll = new JScrollPane();
			scroll.setMinimumSize(new Dimension(150, 3));
			scroll.setMaximumSize(new Dimension(150, 3));
			scroll.setPreferredSize(new Dimension(150, 3));
			add(scroll, new GridBagConstraints(0, 0, 1, 1, 0.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(
				6, 6, 0, 6), 0, 0));
			{
				list = new JList();
				scroll.setViewportView(list);
				listModel = new DefaultComboBoxModel();
				list.setModel(listModel);
				list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			}
		}
		{
			JPanel panel = new JPanel(new GridLayout(1, 1, 6, 6));
			add(panel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(6,
				6, 6, 6), 0, 0));
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
					scroll.setMinimumSize(new Dimension(3, 50));
					scroll.setMaximumSize(new Dimension(3, 50));
					scroll.setPreferredSize(new Dimension(3, 50));
					{
						descriptionText = new JTextArea();
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

		UI.enableWhenModelHasSelection(list.getSelectionModel(), deleteButton, nameText, descriptionText);
	}
}
