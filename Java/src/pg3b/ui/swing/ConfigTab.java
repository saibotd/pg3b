
package pg3b.ui.swing;

import java.awt.CardLayout;

import javax.swing.JPanel;
import javax.swing.JToggleButton;

import pg3b.ui.Config;
import pg3b.ui.ControllerTrigger;

public class ConfigTab extends JPanel {
	private PG3BUI owner;
	private CardLayout cardLayout;
	private ConfigEditor configEditor;
	private ControllerTriggerPanel controllerTriggerPanel;
	private Boolean wasCaptureButtonDown;

	public ConfigTab (PG3BUI owner) {
		this.owner = owner;
		setLayout(cardLayout = new CardLayout());
		add(configEditor = new ConfigEditor(owner), "config");
		add(controllerTriggerPanel = new ControllerTriggerPanel(owner), "controllerTrigger");
	}

	public void showConfigEditor () {
		JToggleButton captureButton = owner.getCaptureButton();
		captureButton.setEnabled(true);
		if (wasCaptureButtonDown != null) captureButton.setSelected(wasCaptureButtonDown);
		wasCaptureButtonDown = null;

		cardLayout.show(this, "config");
	}

	public void showTriggerPanel (Config config, ControllerTrigger input) {
		JToggleButton captureButton = owner.getCaptureButton();
		captureButton.setEnabled(false);
		wasCaptureButtonDown = captureButton.isSelected();
		captureButton.setSelected(false);

		controllerTriggerPanel.setTrigger(config, input);
		cardLayout.show(this, "controllerTrigger");
	}

	public ConfigEditor getConfigEditor () {
		return configEditor;
	}
}
