
package com.esotericsoftware.controller.util;

public class Loader {
	public volatile boolean cancelled;

	public void cancel () {
		cancelled = true;
	}

	public void throwCancelled () {
		if (cancelled) throw new CancelledException();
	}

	public void setPercentageComplete (float percent) {
	}

	public void setMessage (String message) {
	}

	static public class CancelledException extends RuntimeException {
	}
}
