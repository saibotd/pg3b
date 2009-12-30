
package com.esotericsoftware.controller.ui;

import static com.esotericsoftware.minlog.Log.*;

import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.esotericsoftware.controller.device.Deadzone;
import com.esotericsoftware.controller.device.Device;
import com.esotericsoftware.controller.device.Stick;
import com.esotericsoftware.controller.ui.swing.UI;

/**
 * Maintains a list of triggers and checks them when activated.
 */
public class Config extends Editable {
	private List<Trigger> triggers = new ArrayList();
	private Deadzone leftDeadzone, rightDeadzone;
	private MouseTranslation mouseTranslation;

	public Config () {
	}

	public Config (File file) {
		super(file);
	}

	public List<Trigger> getTriggers () {
		return triggers;
	}

	public void setTriggers (List<Trigger> triggers) {
		this.triggers = triggers;
	}

	public Deadzone getLeftDeadzone () {
		return leftDeadzone;
	}

	public void setLeftDeadzone (Deadzone leftDeadzone) {
		this.leftDeadzone = leftDeadzone;
	}

	public Deadzone getRightDeadzone () {
		return rightDeadzone;
	}

	public void setRightDeadzone (Deadzone rightDeadzone) {
		this.rightDeadzone = rightDeadzone;
	}

	public MouseTranslation getMouseTranslation () {
		return mouseTranslation;
	}

	public void setMouseTranslation (MouseTranslation mouseTranslation) {
		this.mouseTranslation = mouseTranslation;
	}

	public int hashCode () {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((triggers == null) ? 0 : triggers.hashCode());
		return result;
	}

	public boolean equals (Object obj) {
		if (this == obj) return true;
		if (!super.equals(obj)) return false;
		if (getClass() != obj.getClass()) return false;
		Config other = (Config)obj;
		if (triggers == null) {
			if (other.triggers != null) return false;
		} else if (!triggers.equals(other.triggers)) return false;
		return true;
	}

	public void save (Writer writer) throws IOException {
		super.save(writer);
		if (DEBUG) debug("Config saved: " + this);
	}
}
