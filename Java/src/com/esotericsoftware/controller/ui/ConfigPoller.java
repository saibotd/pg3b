
package com.esotericsoftware.controller.ui;

import static com.esotericsoftware.minlog.Log.*;

import java.awt.EventQueue;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.esotericsoftware.controller.device.Device;
import com.esotericsoftware.controller.device.Stick;
import com.esotericsoftware.controller.ui.swing.UI;

public class ConfigPoller implements Runnable {
	private final Config config;
	private boolean hasError;
	private Thread thread;
	private volatile boolean running;

	public ConfigPoller (Config config) {
		this.config = config;
	}

	/**
	 * If true, starts a thread to check the triggers and executes their actions as needed. If false, stops checking the triggers
	 * and shuts down any running thread pool.
	 */
	public synchronized void setActive (boolean active) {
		if (active) {
			thread = new Thread(this, config.getName());
			thread.start();
			if (INFO) info("Activated config: " + config.getName());
		} else {
			running = false;
			thread = null;
		}
	}

	public void run () {
		running = true;
		hasError = false;
		Device device = UI.instance.getDevice();
		try {
			if (device != null) {
				device.setDeadzone(Stick.left, config.getLeftDeadzone());
				device.setDeadzone(Stick.right, config.getRightDeadzone());
			}
			MouseTranslation mouseTranslation = config.getMouseTranslation();
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
			ArrayList<Trigger> activeTriggers = new ArrayList();
			while (running) {
				Thread.yield();

				if (device != null) device.collectChanges();

				if (mouseTranslation != null) mouseTranslation.update(device);

				for (Poller poller : pollers)
					poller.poll();

				for (int i = 0, n = triggers.size(); i < n; i++) {
					Trigger trigger = triggers.get(i);
					boolean wasActive = activeTriggers.contains(trigger);
					if (trigger.isActive()) {
						if (!wasActive) {
							if (TRACE) debug("Trigger \"" + trigger + "\" is active with state: " + trigger.getPayload());
							activeTriggers.add(trigger);
							execute(trigger);
						}
					} else {
						if (wasActive) {
							if (TRACE) debug("Trigger \"" + trigger + "\" is inactive with state: " + trigger.getPayload());
							activeTriggers.remove(trigger);
							execute(trigger);
						}
					}
				}
				// Triggers are applied continuously in activation order so those that manipulate the same targets work correctly.
				for (Trigger trigger : activeTriggers)
					execute(trigger);

				if (device != null) device.applyChanges();
			}
		} catch (Exception ex) {
			if (ERROR) error("Error checking config triggers.", ex);
			hasError = true;
		} finally {
			if (device != null) {
				try {
					device.applyChanges();
				} catch (IOException ignored) {
				}
			}
			if (INFO) info("Deactivated config: " + config.getName());
			EventQueue.invokeLater(new Runnable() {
				public void run () {
					if (config.equals(UI.instance.getActiveConfig())) UI.instance.setActiveConfig(null);
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

	public Config getConfig () {
		return config;
	}
}
