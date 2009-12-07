
package pg3b.ui;

import java.awt.EventQueue;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.management.RuntimeErrorException;
import javax.swing.JToggleButton;

import static com.esotericsoftware.minlog.Log.*;

import pg3b.ui.swing.PG3BUI;
import pg3b.util.NamedThreadFactory;

/**
 * Maintains a list of triggers and polls them when activated.
 */
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

	/**
	 * If true, starts a thread to poll the triggers and executes their actions as needed using a thread pool. If false, stops
	 * polling the triggers and shuts down any running thread pool.
	 */
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

		private ExecutorService threadPool = Executors.newFixedThreadPool(24, new NamedThreadFactory("poller", false));
		private boolean hasError;

		public Poller () {
			super(Config.this.getName());
			start();
		}

		public void run () {
			try {
				while (running) {
					for (final Trigger trigger : getTriggers()) {
						final Object state = trigger.poll();
						if (state == null) continue;
						threadPool.execute(new Runnable() {
							public void run () {
								final Action action = trigger.getAction();
								try {
									action.execute(Config.this, trigger, state);
								} catch (Exception ex) {
									if (ERROR) error("Error executing action: " + action, ex);
									hasError = true;
									running = false;
								}
							}
						});
					}
					Thread.yield();
				}
			} catch (Exception ex) {
				if (ERROR) error("Error polling triggers.", ex);
				hasError = true;
			} finally {
				threadPool.shutdownNow();
				EventQueue.invokeLater(new Runnable() {
					public void run () {
						JToggleButton captureButton = PG3BUI.instance.getCaptureButton();
						if (captureButton.isSelected()) captureButton.doClick();
						if (hasError) PG3BUI.instance.getStatusBar().setMessage("Error during config processing.");
					}
				});
			}
		}
	}
}
