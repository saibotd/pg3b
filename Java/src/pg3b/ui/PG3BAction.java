
package pg3b.ui;

import static com.esotericsoftware.minlog.Log.*;

import java.io.IOException;

import pg3b.Axis;
import pg3b.Button;
import pg3b.PG3B;
import pg3b.Target;
import pg3b.ui.swing.PG3BUI;

/**
 * An action that sets the state of a PG3B button or axis when executed.
 */
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

	public void execute (Config config, Trigger trigger, Object payload) {
		PG3B pg3b = PG3BUI.instance.getPg3b();
		if (pg3b == null) return;
		float state = payload instanceof Float ? (Float)payload : 1;
		try {
			pg3b.set(target, state);
		} catch (IOException ex) {
			if (ERROR) error("Error executing action: " + this, ex);
		}
	}

	public String toString () {
		if (target == null) return "";
		return "PG3B: " + target.toString();
	}
}
