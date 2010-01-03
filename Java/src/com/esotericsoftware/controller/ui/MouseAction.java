
package com.esotericsoftware.controller.ui;

public class MouseAction implements Action {
	private MouseTranslation translation;
	private transient MouseTranslation oldTranslation;

	public Object execute (Config config, Trigger trigger, boolean isActive, Object payload) {
		MouseTranslation current = config.getMouseTranslation();
		if (isActive) {
			if (current != translation) {
				oldTranslation = current;
				config.setMouseTranslation(translation);
			}
		} else {
			if (oldTranslation != null) config.setMouseTranslation(oldTranslation);
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

	public String toString () {
		return "Mouse Translation";
	}
}
