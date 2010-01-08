
package com.esotericsoftware.controller.ui;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;

import net.sourceforge.yamlbeans.YamlConfig;
import net.sourceforge.yamlbeans.YamlException;
import net.sourceforge.yamlbeans.YamlReader;
import net.sourceforge.yamlbeans.YamlWriter;
import net.sourceforge.yamlbeans.scalar.ScalarSerializer;

import com.esotericsoftware.controller.device.Deadzone;
import com.esotericsoftware.controller.device.Device;
import com.esotericsoftware.controller.device.Target;
import com.esotericsoftware.controller.input.JInputJoystick;
import com.esotericsoftware.controller.input.JInputXboxController;
import com.esotericsoftware.controller.input.Keyboard;
import com.esotericsoftware.controller.input.Mouse;
import com.esotericsoftware.controller.input.XInputXboxController;
import com.esotericsoftware.controller.input.Keyboard.KeyboardInput;
import com.esotericsoftware.controller.ui.DeviceAction.Direction;
import com.esotericsoftware.controller.ui.swing.EditorPanel;
import com.esotericsoftware.controller.xim.XIMMouseTranslation;

/**
 * Base class for a file-based object that can be edited and saved by an {@link EditorPanel}.
 */
public class Editable implements Cloneable {
	static public final YamlConfig yamlConfig = new YamlConfig();
	static {
		yamlConfig.setPrivateFields(true);
		yamlConfig.setBeanProperties(false);
		yamlConfig.writeConfig.setAutoAnchor(false);
		yamlConfig.writeConfig.setWriteRootTags(false);
		// yamlConfig.writeConfig.setWriteDefaultValues(true);

		yamlConfig.setScalarSerializer(KeyboardInput.class, new ScalarSerializer<KeyboardInput>() {
			public KeyboardInput read (String value) {
				KeyboardInput input = new KeyboardInput();
				input.setKeyCode(Keyboard.getKeyCode(value));
				return input;
			}

			public String write (KeyboardInput input) {
				return Keyboard.getName(input.getKeyCode()).toLowerCase();
			}
		});

		yamlConfig.setScalarSerializer(DeviceAction.class, new ScalarSerializer<DeviceAction>() {
			public DeviceAction read (String value) {
				DeviceAction action = new DeviceAction();
				int spaceIndex = value.indexOf(' ');
				if (spaceIndex != -1) {
					action.setTarget(Device.getTarget(value.substring(0, spaceIndex)));
					action.setDirection(Direction.valueOf(value.substring(spaceIndex + 1)));
				} else
					action.setTarget(Device.getTarget(value));
				return action;
			}

			public String write (DeviceAction action) {
				String value = action.getTarget().name();
				if (action.getDirection() != Direction.both) value += " " + action.getDirection();
				return value;
			}
		});

		yamlConfig.setScalarSerializer(ScriptAction.class, new ScalarSerializer<ScriptAction>() {
			public ScriptAction read (String value) {
				ScriptAction action = new ScriptAction();
				action.setScriptName(value);
				return action;
			}

			public String write (ScriptAction action) {
				return action.getScriptName();
			}
		});

		yamlConfig.setScalarSerializer(ScriptAction.class, new ScalarSerializer<ScriptAction>() {
			public ScriptAction read (String value) {
				ScriptAction action = new ScriptAction();
				action.setScriptName(value);
				return action;
			}

			public String write (ScriptAction action) {
				return action.getScriptName();
			}
		});

		yamlConfig.setClassTag("device", DeviceAction.class);
		yamlConfig.setClassTag("script", ScriptAction.class);
		yamlConfig.setClassTag("mouse", Mouse.MouseInput.class);
		yamlConfig.setClassTag("joystick", JInputJoystick.JoystickInput.class);
		yamlConfig.setClassTag("xboxcontroller", XInputXboxController.ControllerInput.class);
		yamlConfig.setClassTag("xboxcontroller-jinput", JInputXboxController.ControllerInput.class);
		yamlConfig.setClassTag("round", Deadzone.Round.class);
		yamlConfig.setClassTag("square", Deadzone.Square.class);
		yamlConfig.setClassTag("mouse-settings", MouseAction.class);
		yamlConfig.setClassTag("xim-translation", XIMMouseTranslation.class);
		yamlConfig.setClassTag("text-mode", TextModeAction.class);

		yamlConfig.setPropertyElementType(Config.class, "triggers", InputTrigger.class);
		yamlConfig.setPropertyDefaultType(Config.class, "targetNames", LinkedHashMap.class);
		yamlConfig.setPropertyDefaultType(Trigger.class, "action", DeviceAction.class);
		yamlConfig.setPropertyDefaultType(InputTrigger.class, "input", Keyboard.KeyboardInput.class);
	}

	protected transient File file;
	private String description = "";

	public Editable () {
	}

	public Editable (File file) {
		this.file = file.getAbsoluteFile();
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

	public void save (Writer writer) throws IOException {
		if (writer == null) throw new IllegalArgumentException("writer cannot be null.");
		YamlWriter yamlWriter = new YamlWriter(writer, yamlConfig);
		try {
			yamlWriter.write(this);
			yamlWriter.close();
		} finally {
			writer.close();
		}
	}

	public void load (File file) throws IOException {
		this.file = file.getAbsoluteFile();
		YamlReader yamlReader = getYamlReader(new FileReader(file));
		try {
			yamlReader.read(getClass());
		} catch (Exception ex) {
			IOException ioEx = new IOException();
			ioEx.initCause(ex);
			throw ioEx;
		} finally {
			try {
				if (yamlReader != null) yamlReader.close();
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
