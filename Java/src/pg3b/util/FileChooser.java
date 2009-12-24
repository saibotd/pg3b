
package pg3b.util;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class FileChooser extends FileDialog {
	static private final Map<String, FileChooser> fileChoosers = new HashMap();

	public FileChooser (Frame frame) {
		super(frame);
	}

	public boolean show (String title, boolean save) {
		setTitle(title);
		setMode(save ? FileChooser.SAVE : FileChooser.LOAD);
		setLocationRelativeTo(null);
		setVisible(true);
		return getFile() != null;
	}

	public File getSelectedFile () {
		String path = getFile();
		if (path == null) return null;
		return new File(getDirectory(), path);
	}

	static public FileChooser get (Frame frame, String name, String dir) {
		FileChooser fileChooser = fileChoosers.get(name);
		if (fileChooser == null) {
			fileChooser = new FileChooser(frame);
			fileChooser.setDirectory(new File(dir).getAbsolutePath());
			fileChoosers.put(name, fileChooser);
		}
		return fileChooser;
	}
}
