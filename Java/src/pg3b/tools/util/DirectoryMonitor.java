
package pg3b.tools.util;

import static com.esotericsoftware.minlog.Log.*;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import com.esotericsoftware.minlog.Log;

public abstract class DirectoryMonitor<T> {
	static private final Timer timer = new Timer("DirectoryMonitor", true);

	private final TreeSet<Item> items = new TreeSet();
	final String fileExtension;

	private final FileFilter fileFilter = new FileFilter() {
		public boolean accept (File file) {
			return file.isDirectory() || file.getName().endsWith(fileExtension);
		}
	};

	public DirectoryMonitor (String fileExtension) {
		if (fileExtension == null) throw new IllegalArgumentException("fileExtension cannot be null.");

		this.fileExtension = fileExtension;
	}

	public TimerTask scan (final File dir, int interval) {
		TimerTask task = new TimerTask() {
			public void run () {
				scan(dir);
			}
		};
		timer.scheduleAtFixedRate(task, 0, interval);
		return task;
	}

	public void scan (File dir) {
		if (dir == null) throw new IllegalArgumentException("dir cannot be null.");
		if (!dir.exists()) dir.mkdirs();
		if (!dir.exists() || !dir.isDirectory()) throw new IllegalArgumentException("Directory does not exist: " + dir);
		boolean updated = false;
		// Remove deleted and update existing items.
		for (Iterator iter = items.iterator(); iter.hasNext();) {
			Item item = (Item)iter.next();
			if (!item.file.exists()) {
				updated = true;
				iter.remove();
				if (TRACE) trace("Removed item: " + item);
				continue;
			}
			long lastModified = item.file.lastModified();
			if (lastModified == item.lastModified) continue;
			updated = true;
			try {
				item.object = load(item.file);
				item.lastModified = lastModified;
				if (TRACE) trace("Updated item: " + item);
			} catch (Exception ex) {
				if (ERROR) error("Item ignored: " + item, ex);
				iter.remove();
			}
		}
		// Add new items.
		for (File file : dir.listFiles(fileFilter)) {
			if (file.isDirectory()) {
				scan(file);
				continue;
			}
			Item item = new Item();
			item.file = file;
			item.lastModified = file.lastModified();
			if (!items.add(item)) continue;
			try {
				item.object = load(file);
				updated = true;
				if (TRACE) trace("Added item: " + item);
			} catch (Exception ex) {
				if (ERROR) error("Item ignored: " + item, ex);
				items.remove(item);
			}
		}
		if (updated) updated();
	}

	protected void updated () {
	}

	abstract protected T load (File file);

	public List<T> getItems () {
		ArrayList<T> list = new ArrayList();
		for (Item item : items)
			list.add(item.object);
		return list;
	}

	class Item implements Comparable<Item> {
		File file;
		long lastModified;
		T object;

		public int compareTo (Item other) {
			return file.compareTo(other.file);
		}

		public String toString () {
			return file.getAbsolutePath();
		}

		public int hashCode () {
			return 31 + file.hashCode();
		}

		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (obj == null || getClass() != obj.getClass()) return false;
			if (!file.equals(((Item)obj).file)) return false;
			return true;
		}
	}

	public static void main (String[] args) throws Exception {
		Log.set(LEVEL_TRACE);
		DirectoryMonitor monitor = new DirectoryMonitor(".config") {
			protected Object load (File file) {
				return file.getName();
			}
		};
		while (true) {
			Thread.sleep(1000);
			monitor.scan(new File("test"));
		}
	}
}
