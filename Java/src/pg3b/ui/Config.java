
package pg3b.ui;

import java.awt.EventQueue;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JToggleButton;

import pg3b.ui.swing.PG3BUI;

public class Config extends Editable {
	private List<Trigger> triggers = new ArrayList();
	private transient Poller poller;

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

	public synchronized void setActive (boolean active) {
		if (active) {
			if (poller != null) return;
			poller = new Poller();
		} else {
			if (poller != null) poller.running = false;
			poller = null;
		}
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

	private class Poller extends Thread {
		public volatile boolean running = true;

		public Poller () {
			super(Config.this.getName());
			start();
		}

		public void run () {
			try {
				while (running) {
					for (Trigger trigger : getTriggers()) {
						if (trigger.poll()) PG3BUI.instance.getControllerPanel().repaint();
					}
				}
			} catch (RuntimeException ex) {
				EventQueue.invokeLater(new Runnable() {
					public void run () {
						JToggleButton captureButton = PG3BUI.instance.getCaptureButton();
						if (captureButton.isSelected()) captureButton.doClick();
						PG3BUI.instance.getStatusBar().setMessage("Error during config processing.");
					}
				});
				throw ex;
			}
		}
	}
}
