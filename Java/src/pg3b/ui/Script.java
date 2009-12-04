
package pg3b.ui;

import java.io.File;

public class Script extends Editable {
	private String code;

	public Script () {
	}

	public Script (File file) {
		super(file);
	}

	public String getCode () {
		return code;
	}

	public void setCode (String code) {
		this.code = code;
	}
}
