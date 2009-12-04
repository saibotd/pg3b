
package pg3b.util;

import static com.esotericsoftware.minlog.Log.*;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.SwingUtilities;

abstract public class LoaderDialog extends Loader implements Runnable {
	ProgressDialog dialog = new ProgressDialog();
	boolean success;
	Throwable exception;
	Thread thread;

	public LoaderDialog (String title) {
		dialog.setMessage("Initializing...");
		dialog.setAlwaysOnTop(true);
		dialog.setValue(-1);
		dialog.setTitle(title);
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing (WindowEvent e) {
				if (!success) {
					thread.interrupt();
					cancel();
				}
			}
		});
	}

	public void setMessage (String message) {
		dialog.setMessage(message);
	}

	public void setPercentageComplete (float percent) {
		dialog.setValue(percent);
	}

	public void start (String threadName) {
		new Thread(this, threadName).start();
		dialog.setVisible(true);
		dialog.dispose();
	}

	public final void run () {
		thread = Thread.currentThread();
		try {
			throwCancelled();
			load();
			throwCancelled();
			success = true;
		} catch (Loader.CancelledException ex) {
			cancel();
		} catch (Throwable ex) {
			cancel();
			if (ERROR) error("", ex);
			exception = ex;
		} finally {
			SwingUtilities.invokeLater(new Runnable() {
				public void run () {
					dialog.setVisible(false);
					complete();
				}
			});
		}
	}

	public boolean failed () {
		return !success;
	}

	public Throwable getException () {
		return exception;
	}

	abstract public void load () throws Exception;

	public void complete () {
	}
}
