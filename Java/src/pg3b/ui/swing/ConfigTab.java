
package pg3b.ui.swing;

import java.awt.CardLayout;

import javax.swing.JPanel;

import pg3b.ui.Config;
import pg3b.ui.ControllerTrigger;

public class ConfigTab extends JPanel {
	CardLayout cardLayout;
	ConfigPanel configPanel;
	ControllerTriggerPanel controllerTriggerPanel;

	public ConfigTab (PG3BUI owner) {
		setLayout(cardLayout = new CardLayout());
		add(configPanel = new ConfigPanel(owner), "config");
		add(controllerTriggerPanel = new ControllerTriggerPanel(owner), "controllerTrigger");
	}

	public void showConfigPanel () {
		cardLayout.show(this, "config");
	}

	public void showTriggerPanel (Config config, ControllerTrigger input) {
		controllerTriggerPanel.setTrigger(config, input);
		cardLayout.show(this, "controllerTrigger");
	}

	public ConfigPanel getConfigPanel () {
		return configPanel;
	}
}
