
package pg3b.ui;

import static com.esotericsoftware.minlog.Log.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;

import net.sourceforge.yamlbeans.YamlWriter;
import pnuts.lang.ParseException;
import pnuts.lang.Pnuts;

/**
 * Stores the code for a Pnuts script.
 */
public class Script extends Editable {
	private transient String code;
	private transient Pnuts pnuts;

	public Script () {
	}

	public Script (File file) {
		super(file);
	}

	public String getCode () {
		return code;
	}

	public void setCode (String code) {
		if (code != null) {
			// Normalize EOL characters.
			code = code.replaceAll("\r\n", "\n").replaceAll("\r", "\n");
			// Prevent whitespace before EOL, allows YAML block style output.
			code = code.replaceAll("[ \t]+\n", "\n");
		}
		this.code = code;

		pnuts = null;
		if (code != null) {
			try {
				pnuts = Pnuts.parse(code);
			} catch (ParseException ex) {
				if (TRACE) trace("Error compiling script.", ex);
			}
		}
	}

	public Pnuts getPnuts () {
		return pnuts;
	}

	public void save (Writer writer) throws IOException {
		if (writer == null) throw new IllegalArgumentException("writer cannot be null.");
		YamlWriter yamlWriter = new YamlWriter(writer, yamlConfig);
		try {
			yamlWriter.write(this);
			yamlWriter.clearAnchors();
			writer.write("---\r\n");
			writer.write(code.replaceAll("\n", "\r\n"));
		} finally {
			yamlWriter.close();
		}
	}

	public void load (File file) throws IOException {
		this.file = file;
		BufferedReader reader = new BufferedReader(new FileReader(file));
		try {
			StringBuilder buffer = new StringBuilder(4096);
			while (true) {
				String line = reader.readLine();
				if (line == null) break;
				if (line.equals("---")) {
					getYamlReader(new StringReader(buffer.toString())).read(Script.class);
					buffer.setLength(0);
				} else {
					buffer.append(line);
					buffer.append("\n");
				}
			}
			setCode(buffer.toString());
		} finally {
			reader.close();
		}
	}

	public int hashCode () {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((code == null) ? 0 : code.hashCode());
		return result;
	}

	public boolean equals (Object obj) {
		if (this == obj) return true;
		if (!super.equals(obj)) return false;
		if (getClass() != obj.getClass()) return false;
		Script other = (Script)obj;
		if (code == null) {
			if (other.code != null) return false;
		} else if (!code.equals(other.code)) return false;
		return true;
	}
}
