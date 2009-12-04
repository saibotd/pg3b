
package pg3b;

import static com.esotericsoftware.minlog.Log.*;

import java.util.ArrayList;
import java.util.List;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Event;
import net.java.games.input.EventQueue;
import net.java.games.input.Component.Identifier;
import pg3b.util.Listeners;

public class XboxController {
	static final int DPAD_UP = 4, DPAD_DOWN = 8, DPAD_LEFT = 16, DPAD_RIGHT = 32;

	private final Controller controller;
	int dpadDirection;
	private Listeners<Listener> listeners = new Listeners(Listener.class);

	public static void main (String[] args) throws Exception {
		XboxController controller = new XboxController(getAllControllers().get(0));
		controller.addListener(new Listener() {
			public void buttonChanged (Button button, boolean pressed) {
				System.out.println(button + " " + pressed);
			}

			public void axisChanged (Axis axis, float state) {
				System.out.println(axis + " " + state);
			}
		});
		while (true) {
			controller.poll();
			Thread.sleep(100);
		}
	}

	public XboxController (Controller controller) {
		this.controller = controller;
	}

	public boolean get (Button button) {
		if (!poll()) return false;
		Identifier.Button id = null;
		switch (button) {
		case up:
			return (dpadDirection & DPAD_UP) == DPAD_UP;
		case down:
			return (dpadDirection & DPAD_DOWN) == DPAD_DOWN;
		case left:
			return (dpadDirection & DPAD_LEFT) == DPAD_LEFT;
		case right:
			return (dpadDirection & DPAD_RIGHT) == DPAD_RIGHT;
		case start:
		case guide:
			// The Xbox controller driver doesn't expose these buttons!
			return false;
		case a:
			id = Identifier.Button._0;
			break;
		case b:
			id = Identifier.Button._1;
			break;
		case x:
			id = Identifier.Button._2;
			break;
		case y:
			id = Identifier.Button._3;
			break;
		case leftShoulder:
			id = Identifier.Button._4;
			break;
		case rightShoulder:
			id = Identifier.Button._5;
			break;
		case back:
			id = Identifier.Button._6;
			break;
		case leftStick:
			id = Identifier.Button._8;
			break;
		case rightStick:
			id = Identifier.Button._9;
			break;
		}
		return controller.getComponent(id).getPollData() != 0;
	}

	public float get (Axis axis) {
		if (!poll()) return 0;
		Identifier.Axis id = null;
		boolean invert = false;
		switch (axis) {
		case leftTrigger:
		case rightTrigger:
			float value = controller.getComponent(Identifier.Axis.Z).getPollData();
			if (value > 0)
				value = axis == Axis.leftTrigger ? value : 0;
			else
				value = axis == Axis.rightTrigger ? -value : 0;
			return value;
		case leftStickX:
			id = Identifier.Axis.X;
			break;
		case leftStickY:
			id = Identifier.Axis.Y;
			invert = true;
			break;
		case rightStickX:
			id = Identifier.Axis.RX;
			break;
		case rightStickY:
			id = Identifier.Axis.RY;
			invert = true;
			break;
		}
		float value = controller.getComponent(id).getPollData();
		if (invert) value = -value;
		return value;
	}

	public boolean poll () {
		if (!controller.poll()) {
			Listener[] listeners = this.listeners.toArray();
			for (int i = 0, n = listeners.length; i < n; i++)
				listeners[i].disconnected();
			return false;
		}
		EventQueue eventQueue = controller.getEventQueue();
		Event event = new Event();
		while (eventQueue.getNextEvent(event)) {
			Identifier id = event.getComponent().getIdentifier();
			float value = event.getValue();

			if (id instanceof Identifier.Button) {
				notifyListeners(getButton((Identifier.Button)id), value != 0);
				continue;
			}

			if (id == Identifier.Axis.POV) {
				int newDirection = 0;
				if (value == Component.POV.RIGHT || value == Component.POV.UP_RIGHT || value == Component.POV.DOWN_RIGHT)
					newDirection |= DPAD_RIGHT;
				if (value == Component.POV.LEFT || value == Component.POV.UP_LEFT || value == Component.POV.DOWN_LEFT)
					newDirection |= DPAD_LEFT;
				if (value == Component.POV.DOWN || value == Component.POV.DOWN_LEFT || value == Component.POV.DOWN_RIGHT)
					newDirection |= DPAD_DOWN;
				if (value == Component.POV.UP || value == Component.POV.UP_LEFT || value == Component.POV.UP_RIGHT)
					newDirection |= DPAD_UP;
				// If the direction has changed, press or release the dpad buttons.
				int diff = dpadDirection ^ newDirection;
				if ((diff & DPAD_RIGHT) != 0) notifyListeners(Button.right, (newDirection & DPAD_RIGHT) == DPAD_RIGHT);
				if ((diff & DPAD_LEFT) != 0) notifyListeners(Button.left, (newDirection & DPAD_LEFT) == DPAD_LEFT);
				if ((diff & DPAD_DOWN) != 0) notifyListeners(Button.down, (newDirection & DPAD_DOWN) == DPAD_DOWN);
				if ((diff & DPAD_UP) != 0) notifyListeners(Button.up, (newDirection & DPAD_UP) == DPAD_UP);
				dpadDirection = newDirection;
				continue;
			}

			if (id instanceof Identifier.Axis) {
				Axis axis = null;
				if (id == Identifier.Axis.X) axis = Axis.leftStickX;
				if (id == Identifier.Axis.Y) axis = Axis.leftStickY;
				if (id == Identifier.Axis.RX) axis = Axis.rightStickX;
				if (id == Identifier.Axis.RY) axis = Axis.rightStickY;
				if (id == Identifier.Axis.Z) axis = value < 0 ? Axis.leftTrigger : Axis.rightTrigger;
				if (axis != null) {
					notifyListeners(axis, value);
					continue;
				}
			}
		}
		return true;
	}

	private void notifyListeners (Button button, boolean pressed) {
		Listener[] listeners = this.listeners.toArray();
		for (int i = 0, n = listeners.length; i < n; i++)
			listeners[i].buttonChanged(button, pressed);
	}

	private void notifyListeners (Axis axis, float state) {
		Listener[] listeners = this.listeners.toArray();
		for (int i = 0, n = listeners.length; i < n; i++)
			listeners[i].axisChanged(axis, state);
	}

	public void addListener (Listener listener) {
		listeners.addListener(listener);
		if (TRACE) trace("pg3b", "XboxController listener added: " + listener.getClass().getName());
	}

	public void removeListener (Listener listener) {
		listeners.removeListener(listener);
		if (TRACE) trace("pg3b", "XboxController listener removed: " + listener.getClass().getName());
	}

	private Button getButton (Identifier.Button id) {
		if (id == Identifier.Button._0) return Button.a;
		if (id == Identifier.Button._1) return Button.b;
		if (id == Identifier.Button._2) return Button.x;
		if (id == Identifier.Button._3) return Button.y;
		if (id == Identifier.Button._4) return Button.leftShoulder;
		if (id == Identifier.Button._5) return Button.rightShoulder;
		if (id == Identifier.Button._6) return Button.back;
		if (id == Identifier.Button._8) return Button.leftStick;
		if (id == Identifier.Button._9) return Button.rightStick;
		throw new IllegalArgumentException("Unknown button ID: " + id);
	}

	public int getPort () {
		return controller.getPortNumber();
	}

	public String getName () {
		return controller.getName();
	}

	static public List<Controller> getAllControllers () {
		ArrayList<Controller> list = new ArrayList();
		for (Controller controller : ControllerEnvironment.getDefaultEnvironment().getControllers())
			if (controller.getType() == Controller.Type.GAMEPAD) list.add(controller);
		return list;
	}

	static public class Listener {
		public void buttonChanged (Button button, boolean pressed) {
		}

		public void disconnected () {
		}

		public void axisChanged (Axis axis, float state) {
		}
	}
}
