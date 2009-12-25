
package pg3b.ui;

import static com.esotericsoftware.minlog.Log.*;

import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import pg3b.Deadzone;
import pg3b.PG3B;
import pg3b.Stick;
import pg3b.ui.swing.PG3BUI;

/**
 * Maintains a list of triggers and checks them when activated.
 */
public class Config extends Editable {
	private List<Trigger> triggers = new ArrayList();
	private transient PollerThread pollerThread;
	private Deadzone leftDeadzone, rightDeadzone;

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

	/**
	 * If true, starts a thread to check the triggers and executes their actions as needed. If false, stops checking the triggers
	 * and shuts down any running thread pool.
	 */
	public synchronized void setActive (boolean active) {
		if (active) {
			if (pollerThread != null) return;
			pollerThread = new PollerThread();
			if (INFO) info("Activated config: " + getName());
		} else {
			if (pollerThread != null) pollerThread.running = false;
			pollerThread = null;
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

	public void save (Writer writer) throws IOException {
		super.save(writer);
		if (DEBUG) debug("Config saved: " + this);
	}

	private class PollerThread extends Thread {
		public volatile boolean running = true;

		private boolean hasError;

		public PollerThread () {
			super(Config.this.getName());
			start();
		}

		public void run () {
			try {
				PG3B pg3b = PG3BUI.instance.getPG3B();
				if (pg3b != null) {
					pg3b.setDeadzone(Stick.left, leftDeadzone);
					pg3b.setDeadzone(Stick.right, rightDeadzone);
				}
				// Multiple triggers may use the same poller. Obtain a distinct list of pollers to avoid polling the same one twice.
				HashSet<Poller> pollers = new HashSet();
				for (Trigger trigger : getTriggers())
					pollers.add(trigger.getPoller());
				while (running) {
					for (Poller poller : pollers)
						poller.poll();
					for (final Trigger trigger : getTriggers()) {
						final Object state = trigger.check();
						if (state == null) continue;
						final Action action = trigger.getAction();
						try {
							if (action.execute(Config.this, trigger, state)) {
								if (DEBUG) debug("Trigger \"" + trigger + "\" executed action: " + action);
							}
						} catch (Exception ex) {
							if (ERROR) error("Error executing action \"" + action + "\" for trigger: " + trigger, ex);
							hasError = true;
							running = false;
						}
					}
					Thread.yield();
				}
			} catch (Exception ex) {
				if (ERROR) error("Error checking config triggers.", ex);
				hasError = true;
			} finally {
				if (INFO) info("Deactivated config: " + getName());
				EventQueue.invokeLater(new Runnable() {
					public void run () {
						PG3BUI.instance.setActivated(false);
						if (hasError) PG3BUI.instance.getStatusBar().setMessage("Error during config processing.");
					}
				});
			}
		}
	}
}
