
package pg3b.ui.swing;

import java.awt.CardLayout;

import javax.swing.JPanel;
import javax.swing.JToggleButton;

import pg3b.ui.Config;
import pg3b.ui.InputTrigger;

public class ConfigTab extends JPanel {
	private PG3BUI owner;
	private CardLayout cardLayout;
	private ConfigEditor configEditor;
	private InputTriggerPanel inputTriggerPanel;
	private Boolean wasCaptureButtonDown;

	public ConfigTab (PG3BUI owner) {
		this.owner = owner;
		setLayout(cardLayout = new CardLayout());
		add(configEditor = new ConfigEditor(owner), "config");
		add(inputTriggerPanel = new InputTriggerPanel(owner), "inputTrigger");
	}

	public void showConfigEditor () {
		JToggleButton captureButton = owner.getCaptureButton();
		captureButton.setEnabled(true);
		if (wasCaptureButtonDown != null && wasCaptureButtonDown) owner.setCapture(true);
		wasCaptureButtonDown = null;

		cardLayout.show(this, "config");
	}

	public void showInputTriggerPanel (Config config, InputTrigger input) {
		wasCaptureButtonDown = owner.getCaptureButton().isSelected();
		owner.setCapture(false);
		owner.getCaptureButton().setEnabled(false);

		inputTriggerPanel.setTrigger(config, input);
		cardLayout.show(this, "inputTrigger");
	}

	public ConfigEditor getConfigEditor () {
		return configEditor;
	}
}
