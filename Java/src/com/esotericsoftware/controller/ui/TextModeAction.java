
package com.esotericsoftware.controller.ui;

import com.esotericsoftware.controller.ui.swing.UI;

public class TextModeAction implements Action {
	public Object execute (Config config, Trigger trigger, boolean isActive, Object payload) {
		if (isActive) TextMode.block();
		return null;
	}

	public boolean isValid () {
		return UI.instance.getDevice() != null;
	}

	public void reset (Config config, Trigger trigger) {
		TextMode.setEnabled(false);
	}

	public String getType () {
		return "";
	}

	public String toString () {
		return "Text Mode";
	}
}
