
package pg3b.ui;

import static com.esotericsoftware.minlog.Log.*;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import net.sourceforge.yamlbeans.YamlConfig;
import net.sourceforge.yamlbeans.YamlReader;
import net.sourceforge.yamlbeans.YamlWriter;

/**
 * Stores application wide settings for the PG3B UI.
 */
public class Settings {
	public boolean showController = true;
	public String pg3bPort;
	public int controllerPort;
	public String controllerName;
	public String selectedConfig;
	public int logLevel = LEVEL_INFO;

	static private Settings instance;
	static private final String fileName = "pg3b.settings";
	static private final YamlConfig yamlConfig = new YamlConfig();
	static {
		yamlConfig.writeConfig.setWriteRootTags(false);
		yamlConfig.writeConfig.setWriteDefaultValues(true);
	}

	static public synchronized Settings get () {
		if (instance != null) return instance;
		if (new File(fileName).exists()) {
			YamlReader reader = null;
			try {
				reader = new YamlReader(new FileReader(fileName), yamlConfig);
				instance = reader.read(Settings.class);
			} catch (Exception ex) {
				if (WARN) warn("Unable to load settings file: " + fileName, ex);
				if (WARN) warn("Settings reset to defaults.");
			} finally {
				try {
					if (reader != null) reader.close();
				} catch (IOException ignored) {
				}
			}
		}
		if (instance == null) {
			instance = new Settings();
			save();
		}
		return instance;
	}

	static public void save () {
		YamlWriter writer = null;
		try {
			try {
				writer = new YamlWriter(new FileWriter(fileName), yamlConfig);
				writer.write(get());
			} finally {
				if (writer != null) writer.close();
			}
		} catch (IOException ex) {
			if (ERROR) error("Error saving file: " + fileName, ex);
		}
	}
}
