
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
