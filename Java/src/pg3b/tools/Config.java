
package pg3b.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Config {
	private File file;
	private String description;
	private List<Input> inputs = new ArrayList();

	public File getFile () {
		return file;
	}

	public void setFile (File file) {
		this.file = file;
	}

	public String getName () {
		if (file == null) return "";
		String name = file.getName();
		int index = name.lastIndexOf('.');
		if (index == -1) return name;
		return name.substring(0, index);
	}

	public String getDescription () {
		return description;
	}

	public void setDescription (String description) {
		this.description = description;
	}

	public List<Input> getInputs () {
		return inputs;
	}

	public void setInputs (List<Input> inputs) {
		this.inputs = inputs;
	}

	public String toString () {
		return getName();
	}

	static public class Input {
		private String description;

		public String getDescription () {
			return description;
		}

		public void setDescription (String description) {
			this.description = description;
		}
	}
}
