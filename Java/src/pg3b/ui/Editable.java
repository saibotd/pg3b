
package pg3b.ui;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;

import net.sourceforge.yamlbeans.YamlConfig;
import net.sourceforge.yamlbeans.YamlReader;
import net.sourceforge.yamlbeans.YamlWriter;
import net.sourceforge.yamlbeans.scalar.ScalarSerializer;
import pg3b.PG3B;
import pg3b.Target;
import pg3b.ui.swing.EditorPanel;

/**
 * Base class for a file-based object that can be edited and saved by an {@link EditorPanel}.
 */
public class Editable implements Cloneable {
	static public final YamlConfig yamlConfig = new YamlConfig();
	static {
		yamlConfig.setPrivateFields(true);
		yamlConfig.setBeanProperties(false);
		yamlConfig.setScalarSerializer(Target.class, new ScalarSerializer<Target>() {
			public Target read (String value) {
				return PG3B.getTarget(value);
			}

			public String write (Target target) {
				return target.toString();
			}
		});
		yamlConfig.writeConfig.setAutoAnchor(false);
		yamlConfig.writeConfig.setWriteRootTags(false);
		// yamlConfig.writeConfig.setWriteDefaultValues(true);

		yamlConfig.setClassTag("input", InputTrigger.class);
		yamlConfig.setClassTag("PG3B", PG3BAction.class);
		yamlConfig.setClassTag("script", ScriptAction.class);
	}

	protected transient File file;
	private String description = "";

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
		if (description != null) description = description.trim();
		this.description = description;
	}

	public void save () throws IOException {
		if (file == null) throw new IllegalStateException("A file has not been set.");
		YamlWriter writer = new YamlWriter(new FileWriter(file), yamlConfig);
		try {
			writer.write(this);
		} finally {
			writer.close();
		}
	}

	public void load (File file) throws IOException {
		this.file = file;
		YamlReader reader = getYamlReader(new FileReader(file));
		try {
			reader.read(getClass());
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

	protected YamlReader getYamlReader (Reader reader) throws IOException {
		return new YamlReader(reader, yamlConfig) {
			protected Object createObject (Class type) throws InvocationTargetException {
				if (type == Editable.this.getClass()) return Editable.this;
				return super.createObject(type);
			}
		};
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
