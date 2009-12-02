
package pg3b.ui;

import pg3b.Axis;
import pg3b.Button;
import pg3b.Target;

public class Input {
	private String description;
	private InputTrigger trigger;
	private Script script;
	private Target target;

	public String getDescription () {
		return description;
	}

	public void setDescription (String description) {
		this.description = description;
	}

	public InputTrigger getTrigger () {
		return trigger;
	}

	public void setTrigger (InputTrigger trigger) {
		this.trigger = trigger;
	}

	public Script getScript () {
		return script;
	}

	public void setScript (Script script) {
		this.script = script;
	}

	public Target getTarget () {
		return target;
	}

	public void setTarget (Target target) {
		if (!(target instanceof Button) && !(target instanceof Axis))
			throw new IllegalArgumentException("target must be a button or axis.");
		this.target = target;
	}
}
