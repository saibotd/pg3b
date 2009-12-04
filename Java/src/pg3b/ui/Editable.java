
package pg3b.ui;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import net.sourceforge.yamlbeans.YamlConfig;
import net.sourceforge.yamlbeans.YamlException;
import net.sourceforge.yamlbeans.YamlReader;
import net.sourceforge.yamlbeans.YamlWriter;
import net.sourceforge.yamlbeans.scalar.ScalarSerializer;
import pg3b.PG3B;
import pg3b.Target;

public class Editable implements Cloneable {
	static public final YamlConfig yamlConfig = new YamlConfig();
	static {
		yamlConfig.writeConfig.setWriteRootTags(false);
		yamlConfig.writeConfig.setWriteDefaultValues(true);
		yamlConfig.setScalarSerializer(Target.class, new ScalarSerializer<Target>() {
			public Target read (String value) throws YamlException {
				return PG3B.getTarget(value);
			}

			public String write (Target target) throws YamlException {
				return target.toString();
			}
		});
		yamlConfig.setClassTag("controller", ControllerTrigger.class);
		yamlConfig.setClassTag("PG3B", PG3BAction.class);
		yamlConfig.setClassTag("script", ScriptAction.class);
	}

	private transient File file;
	private String description;

	public Editable () {
	}

	public Editable (File file) {
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

	public void save () throws IOException {
		if (file == null) throw new IllegalStateException("A file has not been set.");
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

	static public <T extends Editable> T load (File file, Class<T> c) throws IOException {
		YamlReader reader = null;
		try {
			reader = new YamlReader(new FileReader(file), yamlConfig);
			T object = reader.read(c);
			if (object == null) object = c.newInstance();
			object.file = file;
			return object;
		} catch (Exception ex) {
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

	public Editable clone () {
		try {
			return (Editable)super.clone();
		} catch (CloneNotSupportedException ex) {
			throw new RuntimeException(ex);
		}
	}

	public String toString () {
		return getName();
	}

	public int hashCode () {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((file == null) ? 0 : file.hashCode());
		return result;
	}

	public boolean equals (Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Editable other = (Editable)obj;
		if (description == null) {
			if (other.description != null) return false;
		} else if (!description.equals(other.description)) return false;
		if (file == null) {
			if (other.file != null) return false;
		} else if (!file.equals(other.file)) return false;
		return true;
	}
}
