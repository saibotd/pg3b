
package pg3b.util;

import static com.esotericsoftware.minlog.Log.*;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

public abstract class DirectoryMonitor<T> {
	private final TreeSet<Item> items = new TreeSet();
	private final ArrayList<Item> ignoredItems = new ArrayList();
	private final String fileExtension;

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
		UI.timer.scheduleAtFixedRate(task, interval, interval);
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
				if (DEBUG) debug("Removed file: " + item);
				continue;
			}
			long lastModified = item.file.lastModified();
			if (lastModified == item.lastModified) continue;
			updated = true;
			try {
				item.lastModified = lastModified;
				item.object = load(item.file);
				if (DEBUG) debug("Updated file: " + item);
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
				if (DEBUG) debug("Added file: " + item);
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
}
