
package pg3b.ui;

import static com.esotericsoftware.minlog.Log.*;

import java.io.IOException;

import pg3b.Axis;
import pg3b.Button;
import pg3b.PG3B;
import pg3b.Target;
import pg3b.ui.swing.PG3BUI;

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

	public boolean isValid () {
		return PG3BUI.instance.getPg3b() != null;
	}

	public boolean execute (Object payload) {
		PG3B pg3b = PG3BUI.instance.getPg3b();
		if (pg3b == null) return false;
		float state = payload instanceof Float ? (Float)payload : 1;
		try {
			pg3b.set(target, state);
			return true;
		} catch (IOException ex) {
			if (ERROR) error("Error executing action: " + this, ex);
			return false;
		}
	}

	public String toString () {
		if (target == null) return "";
		return "PG3B: " + target.toString();
	}
}
