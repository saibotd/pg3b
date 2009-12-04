
package pg3b.ui.swing;

import java.awt.CardLayout;

import javax.swing.JPanel;

import pg3b.ui.Config;
import pg3b.ui.ControllerTrigger;

public class ConfigTab extends JPanel {
	CardLayout cardLayout;
	ConfigEditor configEditor;
	ControllerTriggerPanel controllerTriggerPanel;

	public ConfigTab (PG3BUI owner) {
		setLayout(cardLayout = new CardLayout());
		add(configEditor = new ConfigEditor(owner), "config");
		add(controllerTriggerPanel = new ControllerTriggerPanel(owner), "controllerTrigger");
	}

	public void showConfigEditor () {
		cardLayout.show(this, "config");
	}

	public void showTriggerPanel (Config config, ControllerTrigger input) {
		controllerTriggerPanel.setTrigger(config, input);
		cardLayout.show(this, "controllerTrigger");
	}

	public ConfigEditor getConfigEditor () {
		return configEditor;
	}
}
