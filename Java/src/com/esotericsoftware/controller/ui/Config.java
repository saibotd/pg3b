
package com.esotericsoftware.controller.ui;

import static com.esotericsoftware.minlog.Log.*;

import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.esotericsoftware.controller.device.Deadzone;
import com.esotericsoftware.controller.device.Device;
import com.esotericsoftware.controller.device.Stick;
import com.esotericsoftware.controller.device.Target;
import com.esotericsoftware.controller.input.Mouse;
import com.esotericsoftware.controller.ui.swing.UI;

/**
 * Maintains a list of triggers and checks them when activated.
 */
public class Config extends Editable {
	static private PollerThread thread;
	static private Object lock = new Object();

	private List<Trigger> triggers = new ArrayList();
	private Deadzone leftDeadzone, rightDeadzone;
	private MouseTranslation mouseTranslation;
	private Map<String, String> targetNames = new HashMap();

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
		System.out.println(mouseTranslation);
		this.mouseTranslation = mouseTranslation;
	}

	public Map<String, String> getTargetNames () {
		return targetNames;
	}

	/**
	 * @param targetNames Map from target name to alternate name.
	 */
	public void setTargetNames (Map<String, String> targetNames) {
		this.targetNames = targetNames;
	}

	public String getTargetName (Target target) {
		if (target == null) throw new IllegalArgumentException("target cannot be null.");
		String name = targetNames.get(target.name());
		if (name == null || name.length() == 0) name = target.toString();
		return name;
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

	/**
	 * If true, starts a thread to check the triggers and executes their actions as needed. If false, stops checking the triggers
	 * and shuts down any running thread pool.
	 */
	public void setActive (boolean active) {
		synchronized (lock) {
			if (active) {
				if (thread != null && thread.running && thread.config.equals(this)) return;
				stop();
				thread = new PollerThread(this);
				thread.start();
				if (INFO) info("Activated config: " + this);
			} else {
				if (thread != null && thread.running && thread.config.equals(this)) stop();
			}
		}
	}

	static private void stop () {
		if (thread != null && thread.running) {
			thread.running = false;
			thread = null;
		}
	}

	static public Config getActive () {
		PollerThread thread = Config.thread;
		if (thread == null || !thread.running) return null;
		return thread.config;
	}

	static private class PollerThread extends Thread {
		final Config config;
		volatile boolean running = true;
		private boolean hasError;

		public PollerThread (Config config) {
			super(config.getName());
			this.config = config;
		}

		public void run () {
			EventQueue.invokeLater(new Runnable() {
				public void run () {
					UI.instance.updateActiveConfig();
				}
			});
			hasError = false;
			Device device = UI.instance.getDevice();
			MouseTranslation originalMouseTranslation = config.getMouseTranslation();
			try {
				if (device != null) {
					device.setDeadzone(Stick.left, config.getLeftDeadzone());
					device.setDeadzone(Stick.right, config.getRightDeadzone());
					device.setTargetNames(config.getTargetNames());
				}
				List<Trigger> triggers = config.getTriggers();
				// Multiple triggers may use the same poller. Obtain a distinct list to avoid polling the same one twice.
				HashSet<Poller> pollers = new HashSet();
				for (int i = 0, n = triggers.size(); i < n; i++) {
					Trigger trigger = triggers.get(i);
					Poller poller = trigger.getPoller();
					if (poller != null) pollers.add(poller);

					trigger.getAction().reset(config, trigger);
				}
				// Poll initially to clear any values.
				for (Poller poller : pollers)
					poller.poll();
				// Triggers are applied in activation order so those that manipulate the same targets work correctly.
				ArrayList<Trigger> activeTriggers = new ArrayList();
				ArrayList<Trigger> deactivateTriggers = new ArrayList();
				while (running) {
					Thread.sleep(1);

					for (Poller poller : pollers)
						poller.poll();

					for (int i = 0, n = triggers.size(); i < n; i++) {
						Trigger trigger = triggers.get(i);
						boolean wasActive = activeTriggers.contains(trigger);
						if (trigger.isActive()) {
							if (!wasActive) {
								if (TRACE) debug("Trigger \"" + trigger + "\" is active with state: " + trigger.getPayload());
								activeTriggers.add(trigger);
								// Execute "activate" function outside apply/collect.
								execute(trigger);
							}
						} else {
							if (wasActive) {
								if (TRACE) debug("Trigger \"" + trigger + "\" is inactive with state: " + trigger.getPayload());
								activeTriggers.remove(trigger);
								// Execute "deactivate" function inside apply/collect.
								deactivateTriggers.add(trigger);
							}
						}
					}

					if (device != null) device.collect();

					for (int i = 0, n = deactivateTriggers.size(); i < n; i++)
						execute(deactivateTriggers.get(i));
					deactivateTriggers.clear();

					for (int i = 0, n = activeTriggers.size(); i < n; i++)
						execute(activeTriggers.get(i));

					MouseTranslation mouseTranslation = config.getMouseTranslation();
					if (mouseTranslation != null) mouseTranslation.update(device);

					if (device != null) device.apply();
				}
			} catch (Exception ex) {
				if (ERROR) error("Error checking config triggers.", ex);
				hasError = true;
			} finally {
				running = false;
				if (device != null) {
					try {
						device.apply();
					} catch (IOException ignored) {
					}
					device.setDeadzone(Stick.left, null);
					device.setDeadzone(Stick.right, null);
				}
				config.setMouseTranslation(originalMouseTranslation);
				if (INFO) info("Deactivated config: " + config.getName());
				EventQueue.invokeLater(new Runnable() {
					public void run () {
						UI.instance.updateActiveConfig();
						if (hasError) UI.instance.getStatusBar().setMessage("Error during config processing.");
					}
				});
			}
		}

		private void execute (Trigger trigger) {
			try {
				trigger.execute(config);
			} catch (Exception ex) {
				if (ERROR) error("Error executing action \"" + trigger.getAction() + "\" for trigger \"" + trigger + "\".", ex);
				hasError = true;
				running = false;
			}
		}
	}
}
