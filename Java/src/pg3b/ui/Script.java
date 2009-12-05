
package pg3b.ui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;

import net.sourceforge.yamlbeans.YamlWriter;
import pnuts.lang.Context;
import pnuts.lang.Pnuts;

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
		try {
			pnuts = Pnuts.parse(code);
		} catch (Exception ex) {
			// BOZO - Add error markers to the code textarea or gutter. Also indicate compilation failure elsewhere in UI.
			ex.printStackTrace();
		}
	}

	public Pnuts getPnuts () {
		return pnuts;
	}

	public void save () throws IOException {
		if (getFile() == null) throw new IllegalStateException("A file has not been set.");

		FileWriter writer = new FileWriter(getFile());
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
		BufferedReader reader = new BufferedReader(new FileReader(getFile()));
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
