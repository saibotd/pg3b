
package com.esotericsoftware.controller.ui;

import static com.esotericsoftware.minlog.Log.*;

public class MouseAction implements Action {
	private MouseTranslation translation;
	private transient MouseTranslation oldTranslation;

	public Object execute (Config config, Trigger trigger, boolean isActive, Object payload) {
		MouseTranslation current = config.getMouseTranslation();
		if (isActive) {
			if (current != translation) {
				oldTranslation = current;
				config.setMouseTranslation(translation);
				if (DEBUG) debug("Mouse translation changed.");
			}
		} else {
			if (oldTranslation != null) {
				config.setMouseTranslation(oldTranslation);
				if (DEBUG) debug("Mouse translation reverted.");
			}
		}
		return null;
	}

	public boolean isValid () {
		return translation != null;
	}

	public void reset (Config config, Trigger trigger) {
		oldTranslation = null;
	}

	public MouseTranslation getMouseTranslation () {
		return translation;
	}

	public void setMouseTranslation (MouseTranslation translation) {
		this.translation = translation;
	}

	public String getType () {
		return "";
	}

	public int hashCode () {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((translation == null) ? 0 : translation.hashCode());
		return result;
	}

	public boolean equals (Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		MouseAction other = (MouseAction)obj;
		if (translation == null) {
			if (other.translation != null) return false;
		} else if (!translation.equals(other.translation)) return false;
		return true;
	}

	public String toString () {
		return "Mouse Translation";
	}
}
