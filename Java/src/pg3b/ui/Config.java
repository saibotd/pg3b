
package pg3b.ui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Config extends Editable {
	private List<Trigger> triggers = new ArrayList();

	public Config () {
	}

	public Config (File file) {
		super(file);
	}

	public List<Trigger> getTriggers () {
		return triggers;
	}

	public void setTriggers (List<Trigger> triggers) {
		this.triggers = triggers;
	}

	public int hashCode () {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((triggers == null) ? 0 : triggers.hashCode());
		return result;
	}

	public boolean equals (Object obj) {
		if (this == obj) return true;
		if (!super.equals(obj)) return false;
		if (getClass() != obj.getClass()) return false;
		Config other = (Config)obj;
		if (triggers == null) {
			if (other.triggers != null) return false;
		} else if (!triggers.equals(other.triggers)) return false;
		return true;
	}
}
