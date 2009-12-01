
package pg3b.tools;

import static java.awt.GridBagConstraints.*;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import pg3b.tools.util.DirectoryMonitor;
import pg3b.tools.util.UI;

public class ScriptsTab extends JPanel {
	private JList scriptsList;
	DefaultComboBoxModel scriptsListModel;
	private JTextArea scriptDescriptionText, scriptText;
	private JTextField scriptNameText;
	private JButton newScriptButton, deleteScriptButton, recordScriptButton;

	public ScriptsTab () {
		initializeLayout();

		new DirectoryMonitor<Script>(".script") {
			protected Script load (File file) {
				return new Script(file);
			}

			protected void updated () {
				scriptsListModel.removeAllElements();
				for (Script script : getItems())
					scriptsListModel.addElement(script);
			}
		}.scan(new File("scripts"), 3000);
	}

	private void initializeLayout () {
		setLayout(new GridBagLayout());
		{
			JScrollPane scroll = new JScrollPane();
			add(scroll, new GridBagConstraints(1, 1, 1, 2, 0.0, 1.0, CENTER, BOTH, new Insets(6, 6, 0, 6), 0, 0));
			scroll.setMinimumSize(new Dimension(150, 3));
			scroll.setPreferredSize(new Dimension(150, 3));
			scroll.setMaximumSize(new Dimension(150, 3));
			{
				scroll.setViewportView(scriptsList = new JList());
				scriptsList.setModel(scriptsListModel = new DefaultComboBoxModel());
			}
		}
		{
			JScrollPane scroll = new JScrollPane();
			add(scroll, new GridBagConstraints(2, 2, 1, 1, 1.0, 1.0, CENTER, BOTH, new Insets(6, 0, 0, 6), 0, 0));
			{
				scroll.setViewportView(scriptText = new JTextArea());
			}
		}
		{
			JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 6));
			add(panel, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0, EAST, NONE, new Insets(0, 0, 0, 0), 0, 0));
			{
				panel.add(newScriptButton = new JButton("New"));
			}
			{
				panel.add(deleteScriptButton = new JButton("Delete"));
			}
		}
		{
			JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 6));
			add(panel, new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0, EAST, NONE, new Insets(0, 0, 0, 0), 0, 0));
			{
				panel.add(recordScriptButton = new JButton("Record"));
				recordScriptButton.setEnabled(false);
			}
		}
		{
			JPanel panel = new JPanel(new GridBagLayout());
			add(panel, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, CENTER, BOTH, new Insets(0, 0, 0, 0), 0, 0));
			{
				panel
					.add(new JLabel("Name:"), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, EAST, NONE, new Insets(6, 6, 0, 6), 0, 0));
			}
			{
				panel.add(scriptNameText = new JTextField(), new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, CENTER, HORIZONTAL,
					new Insets(6, 0, 0, 6), 0, 0));
			}
			{
				panel.add(new JLabel("Description:"), new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, NORTHEAST, NONE, new Insets(6, 6,
					0, 6), 0, 0));
			}
			{
				JScrollPane scroll = new JScrollPane();
				panel.add(scroll, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, CENTER, HORIZONTAL, new Insets(6, 0, 0, 6), 0, 0));
				scroll.setMinimumSize(new Dimension(3, 50));
				scroll.setPreferredSize(new Dimension(3, 50));
				scroll.setMaximumSize(new Dimension(3, 50));
				{
					scroll.setViewportView(scriptDescriptionText = new JTextArea());
				}
			}
		}

		UI.enableWhenListHasSelection(scriptsList, deleteScriptButton, scriptText, recordScriptButton, scriptNameText,
			scriptDescriptionText);
	}
}
