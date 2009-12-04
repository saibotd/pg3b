
package pg3b.ui.swing;

import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import pg3b.ui.Script;
import pg3b.util.UI;

public class ScriptsTab extends EditorPanel<Script> {
	private int lastSelectedTriggerIndex;

	private JTextArea scriptText;
	private JButton recordButton;

	public ScriptsTab (PG3BUI owner) {
		super(owner, Script.class, new File("scripts"), ".script");

		initializeLayout();
		initializeEvents();
	}

	protected void itemSelected (Script script) {
		if (script == null)
			scriptText.setText("");
		else
			scriptText.setText(script.getCode());
	}

	private void initializeEvents () {
		recordButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				UI.errorDialog(ScriptsTab.this, "Error", "Not implemented yet.");
			}
		});
	}

	private void initializeLayout () {
		{
			JScrollPane scroll = new JScrollPane();
			getContentPanel().add(
				scroll,
				new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0,
					0), 0, 0));
			{
				scriptText = new JTextArea();
				scroll.setViewportView(scriptText);
			}
		}
		{
			JPanel panel = new JPanel(new GridLayout(1, 1, 6, 6));
			getContentPanel().add(
				panel,
				new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(6, 6, 6, 0), 0, 0));
			{
				recordButton = new JButton("Record");
				panel.add(recordButton);
			}
		}

		UI.enableWhenModelHasSelection(getSelectionModel(), recordButton, scriptText);
	}
}
