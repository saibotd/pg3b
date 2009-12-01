
package pg3b.tools;

import java.io.File;

public class Script {
	private final File file;

	public Script (File file) {
		this.file = file;
	}

	public String toString () {
		return file.getName();
	}
}
