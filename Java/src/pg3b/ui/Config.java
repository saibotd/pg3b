
package pg3b.ui;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import pg3b.PG3B.Axis;
import pg3b.PG3B.Button;

import net.sourceforge.yamlbeans.YamlConfig;
import net.sourceforge.yamlbeans.YamlException;
import net.sourceforge.yamlbeans.YamlReader;
import net.sourceforge.yamlbeans.YamlWriter;

public class Config implements Cloneable {
	static private final YamlConfig yamlConfig = new YamlConfig();
	static {
		yamlConfig.writeConfig.setWriteRootTags(false);
		yamlConfig.writeConfig.setWriteDefaultValues(true);
		yamlConfig.setPropertyElementType(Config.class, "inputs", Input.class);
	}

	private transient File file;
	private String description;
	private List<Input> inputs = new ArrayList();

	public Config () {
	}

	public Config (File file) {
		this.file = file;
	}

	public File getFile () {
		return file;
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

	public Config clone () {
		try {
			return (Config)super.clone();
		} catch (CloneNotSupportedException ex) {
			throw new RuntimeException(ex);
		}
	}

	public int hashCode () {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((file == null) ? 0 : file.hashCode());
		result = prime * result + ((inputs == null) ? 0 : inputs.hashCode());
		return result;
	}

	public boolean equals (Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Config other = (Config)obj;
		if (description == null) {
			if (other.description != null) return false;
		} else if (!description.equals(other.description)) return false;
		if (file == null) {
			if (other.file != null) return false;
		} else if (!file.equals(other.file)) return false;
		if (inputs == null) {
			if (other.inputs != null) return false;
		} else if (!inputs.equals(other.inputs)) return false;
		return true;
	}

	public String toString () {
		return getName();
	}

	public void save () throws IOException {
		YamlWriter writer = null;
		try {
			try {
				writer = new YamlWriter(new FileWriter(file), yamlConfig);
				writer.write(this);
			} finally {
				if (writer != null) writer.close();
			}
		} catch (YamlException ex) {
			IOException ioEx = new IOException();
			ioEx.initCause(ex);
			throw ioEx;
		}
	}

	static public Config load (File file) throws IOException {
		YamlReader reader = null;
		try {
			reader = new YamlReader(new FileReader(file), yamlConfig);
			Config config = reader.read(Config.class);
			if (config == null) config = new Config();
			config.file = file;
			return config;
		} catch (YamlException ex) {
			IOException ioEx = new IOException();
			ioEx.initCause(ex);
			throw ioEx;
		} finally {
			try {
				if (reader != null) reader.close();
			} catch (IOException ignored) {
			}
		}
	}

	static public class Input {
		private String description;
		private Script script;
		private Enum target;

		public String getDescription () {
			return description;
		}

		public void setDescription (String description) {
			this.description = description;
		}

		public Script getScript () {
			return script;
		}

		public void setScript (Script script) {
			this.script = script;
		}

		public Enum getTarget () {
			return target;
		}

		public void setTarget (Enum target) {
			if (!(target instanceof Button) && !(target instanceof Axis))
				throw new IllegalArgumentException("target must be a button or axis.");
			this.target = target;
		}
		
		
	}
}
