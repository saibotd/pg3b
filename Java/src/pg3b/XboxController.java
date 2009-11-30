
package pg3b;

import static com.esotericsoftware.minlog.Log.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Event;
import net.java.games.input.EventQueue;
import net.java.games.input.Component.Identifier;
import pg3b.PG3B.Button;
import pg3b.PG3B.Target;
import pg3b.tools.util.Listeners;

public class XboxController implements Runnable {
	static final int DPAD_UP = 4, DPAD_DOWN = 8, DPAD_LEFT = 16, DPAD_RIGHT = 32;

	static final HashMap<Identifier.Button, Button> idToButton = new HashMap();
	{
		idToButton.put(Identifier.Button._0, Button.a);
		idToButton.put(Identifier.Button._1, Button.b);
		idToButton.put(Identifier.Button._2, Button.x);
		idToButton.put(Identifier.Button._3, Button.y);
		idToButton.put(Identifier.Button._4, Button.leftShoulder);
		idToButton.put(Identifier.Button._5, Button.rightShoulder);
		idToButton.put(Identifier.Button._6, Button.back);
		idToButton.put(Identifier.Button._8, Button.leftStick);
		idToButton.put(Identifier.Button._9, Button.rightStick);
	}

	static final HashMap<Button, Identifier.Button> buttonToID = new HashMap();
	{
		buttonToID.put(Button.a, Identifier.Button._0);
		buttonToID.put(Button.b, Identifier.Button._1);
		buttonToID.put(Button.x, Identifier.Button._2);
		buttonToID.put(Button.y, Identifier.Button._3);
		buttonToID.put(Button.leftShoulder, Identifier.Button._4);
		buttonToID.put(Button.rightShoulder, Identifier.Button._5);
		buttonToID.put(Button.back, Identifier.Button._6);
		buttonToID.put(Button.leftStick, Identifier.Button._8);
		buttonToID.put(Button.rightStick, Identifier.Button._9);
	}

	private final Controller controller;
	int dpadDirection;
	private Listeners<Listener> listeners = new Listeners(Listener.class);

	public static void main (String[] args) {
		XboxController controller = new XboxController(getAllControllers().get(0));
		controller.addListener(new Listener() {
			public void button (Button button, boolean pressed) {
				System.out.println(button + " " + pressed);
			}

			public void target (Target target, float state) {
				System.out.println(target + " " + state);
			}
		});
		new Thread(controller).start();
	}

	public XboxController (Controller controller) {
		this.controller = controller;
	}

	public void run () {
		while (poll()) {
		}
	}

	public boolean get (Button button) {
		poll();
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
			// The Xbox controller driver doesn't expose these buttons.
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

	public float get (Target target) {
		poll();
		Identifier.Axis id = null;
		switch (target) {
		case leftStickX:
			id = Identifier.Axis.X;
			break;
		case leftStickY:
			id = Identifier.Axis.Y;
			break;
		case rightStickX:
			id = Identifier.Axis.RX;
			break;
		case rightStickY:
			id = Identifier.Axis.RY;
			break;
		case leftTrigger:
		case rightTrigger:
			float value = controller.getComponent(Identifier.Axis.Z).getPollData();
			if (value > 0) return target == Target.leftTrigger ? value : 0;
			return target == Target.rightTrigger ? -value : 0;
		}
		return controller.getComponent(id).getPollData();
	}

	public boolean poll () {
		if (!controller.poll()) return false;
		EventQueue eventQueue = controller.getEventQueue();
		Event event = new Event();
		while (eventQueue.getNextEvent(event)) {
			Identifier id = event.getComponent().getIdentifier();
			float value = event.getValue();

			if (id instanceof Identifier.Button) {
				notifyListeners(idToButton.get(id), value != 0);
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
				Target target = null;
				if (id == Identifier.Axis.X) target = Target.leftStickX;
				if (id == Identifier.Axis.Y) target = Target.leftStickY;
				if (id == Identifier.Axis.RX) target = Target.rightStickX;
				if (id == Identifier.Axis.RY) target = Target.rightStickY;
				if (id == Identifier.Axis.Z) target = value < 0 ? Target.leftTrigger : Target.rightTrigger;
				if (target != null) {
					// notifyListeners(target, value);
					continue;
				}
			}
		}
		return true;
	}

	private void notifyListeners (Button button, boolean pressed) {
		Listener[] listeners = this.listeners.toArray();
		for (int i = 0, n = listeners.length; i < n; i++)
			listeners[i].button(button, pressed);
	}

	private void notifyListeners (Target target, float state) {
		Listener[] listeners = this.listeners.toArray();
		for (int i = 0, n = listeners.length; i < n; i++)
			listeners[i].target(target, state);
	}

	public void addListener (Listener listener) {
		listeners.addListener(listener);
		if (TRACE) trace("pg3b", "XboxController listener added: " + listener.getClass().getName());
	}

	public void removeListener (Listener listener) {
		listeners.removeListener(listener);
		if (TRACE) trace("pg3b", "XboxController listener removed: " + listener.getClass().getName());
	}

	public int getPort () {
		return controller.getPortNumber();
	}

	public String getName () {
		return controller.getName();
	}

	static public List<Controller> getAllControllers () {
		ArrayList<Controller> list = new ArrayList();
		ControllerEnvironment ce = ControllerEnvironment.getDefaultEnvironment();
		Controller[] controllers = ce.getControllers();
		for (Controller controller : controllers)
			if (controller.getType() == Controller.Type.GAMEPAD) list.add(controller);
		return list;
	}

	static public class Listener {
		public void button (Button button, boolean pressed) {
		}

		public void target (Target target, float state) {
		}
	}
}
