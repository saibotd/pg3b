
package com.esotericsoftware.controller.ui.swing;

import java.awt.CardLayout;

import javax.swing.JPanel;
import javax.swing.JToggleButton;

import com.esotericsoftware.controller.ui.Config;
import com.esotericsoftware.controller.ui.InputTrigger;

public class ConfigTab extends JPanel {
	private UI owner;
	private CardLayout cardLayout;
	private ConfigEditor configEditor;
	private InputTriggerPanel inputTriggerPanel;
	private Config activeConfig;

	public ConfigTab (UI owner) {
		this.owner = owner;
		setLayout(cardLayout = new CardLayout());
		add(configEditor = new ConfigEditor(owner), "config");
		add(inputTriggerPanel = new InputTriggerPanel(owner), "inputTrigger");
	}

	public void showConfigEditor () {
		JToggleButton activateButton = configEditor.getActivateButton();
		activateButton.setEnabled(true);
		if (activeConfig != null) owner.setActiveConfig(activeConfig);
		activeConfig = null;

		cardLayout.show(this, "config");
	}

	public void showInputTriggerPanel (Config config, InputTrigger input) {
		activeConfig = owner.getActiveConfig();
		owner.setActiveConfig(null);
		configEditor.getActivateButton().setEnabled(false);

		inputTriggerPanel.setTrigger(config, input);
		cardLayout.show(this, "inputTrigger");
	}

	public ConfigEditor getConfigEditor () {
		return configEditor;
	}
}
