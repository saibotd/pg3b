
package pg3b.ui;

import pg3b.Axis;
import pg3b.Button;
import pg3b.Target;

public class PG3BAction implements Action {
	private Target target;

	public PG3BAction () {
	}

	public PG3BAction (Target target) {
		setTarget(target);
	}

	public Target getTarget () {
		return target;
	}

	public void setTarget (Target target) {
		if (target != null && !(target instanceof Button) && !(target instanceof Axis))
			throw new IllegalArgumentException("target must be a button or axis.");
		this.target = target;
	}

	public String toString () {
		if (target == null) return "";
		return "PG3B: " + target.toString();
	}

	public void execute () {
		// BOZO
	}
}
