
package com.esotericsoftware.controller.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamedThreadFactory implements ThreadFactory {
	private final AtomicInteger threadNumber = new AtomicInteger(1);
	private final String namePrefix;
	private final boolean daemon;

	public NamedThreadFactory (String name, boolean daemon) {
		this.daemon = daemon;
		namePrefix = name + '-';
	}

	public Thread newThread (Runnable runnable) {
		Thread thread = new Thread(runnable, namePrefix + threadNumber.getAndIncrement());
		thread.setDaemon(daemon);
		return thread;
	}
}
