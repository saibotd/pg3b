
package pg3b.ui.util;

import static com.esotericsoftware.minlog.Log.ERROR;
import static com.esotericsoftware.minlog.Log.INFO;
import static com.esotericsoftware.minlog.Log.LEVEL_INFO;
import static com.esotericsoftware.minlog.Log.error;
import static com.esotericsoftware.minlog.Log.info;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
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
	private final ArrayList<Item> ignoredItems = new ArrayList();
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
		scan(dir);
		TimerTask task = new TimerTask() {
			public void run () {
				scan(dir);
			}
		};
		timer.scheduleAtFixedRate(task, interval, interval);
		return task;
	}

	public synchronized void scan (File dir) {
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
				if (INFO) info("Removed file: " + item);
				continue;
			}
			long lastModified = item.file.lastModified();
			if (lastModified == item.lastModified) continue;
			updated = true;
			try {
				item.lastModified = lastModified;
				item.object = load(item.file);
				if (INFO) info("Updated file: " + item);
			} catch (Exception ex) {
				if (ERROR) error("File ignored: " + item, ex);
				ignoredItems.add(item);
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

			int ignoredIndex = ignoredItems.indexOf(item);
			if (ignoredIndex != -1) {
				if (ignoredItems.get(ignoredIndex).lastModified == item.lastModified) continue;
				ignoredItems.remove(ignoredIndex);
			}

			if (!items.add(item)) continue;
			try {
				item.object = load(file);
				updated = true;
				if (INFO) info("Added file: " + item);
			} catch (Exception ex) {
				if (ERROR) error("File ignored: " + item, ex);
				ignoredItems.add(item);
				items.remove(item);
			}
		}
		if (updated) updated();
	}

	protected void updated () {
	}

	abstract protected T load (File file) throws IOException;

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
		Log.set(LEVEL_INFO);
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
