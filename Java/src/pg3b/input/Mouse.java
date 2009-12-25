
package pg3b.input;

import static com.esotericsoftware.minlog.Log.*;

import java.awt.AWTEvent;
import java.awt.AWTException;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import javax.swing.JFrame;

import pg3b.ui.InputTrigger;
import pg3b.util.Listeners;

/**
 * The input device for the system wide mouse.
 */
public class Mouse implements InputDevice {
	static public final Mouse instance = new Mouse();

	private int x, y;
	private boolean[] buttons = new boolean[4];
	private Listeners<Listener> listeners = new Listeners(Listener.class);
	private int lastButton, lastDeltaX, lastDeltaY, lastMouseWheel;
	private int currentDeltaX, currentDeltaY;
	private JFrame grabbedFrame;
	private boolean usingRobot;
	private Robot robot;

	private Mouse () {
		try {
			robot = new Robot();
		} catch (AWTException ex) {
			if (WARN) warn("Error creating robot.", ex);
		}
		Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
			public void eventDispatched (AWTEvent awtEvent) {
				MouseEvent event = (MouseEvent)awtEvent;
				Listener[] listeners = Mouse.this.listeners.toArray();
				switch (awtEvent.getID()) {
				case MouseEvent.MOUSE_MOVED:
				case MouseEvent.MOUSE_DRAGGED:
					int lastX = x;
					int lastY = y;
					x = event.getXOnScreen();
					y = event.getYOnScreen();
					lastDeltaX += x - lastX;
					lastDeltaY += y - lastY;
					for (int i = 0, n = listeners.length; i < n; i++)
						listeners[i].mouseMoved(Mouse.this, x - lastX, y - lastY);
					if (grabbedFrame != null && robot != null) {
						// Note this is not perfect. Another window can be focused if the mouse is moved and clicked extremely fast.
						x = grabbedFrame.getX() + grabbedFrame.getWidth() / 2;
						y = grabbedFrame.getY() + grabbedFrame.getHeight() / 2;
						robot.mouseMove(x, y);
					}
					break;
				case MouseEvent.MOUSE_PRESSED:
					lastButton = event.getButton();
					buttons[lastButton] = true;
					for (int i = 0, n = listeners.length; i < n; i++)
						listeners[i].mouseDown(Mouse.this, lastButton);
					break;
				case MouseEvent.MOUSE_RELEASED:
					int button = event.getButton();
					buttons[button] = false;
					for (int i = 0, n = listeners.length; i < n; i++)
						listeners[i].mouseUp(Mouse.this, button);
					break;
				case MouseEvent.MOUSE_WHEEL:
					lastMouseWheel = ((MouseWheelEvent)event).getWheelRotation();
					for (int i = 0, n = listeners.length; i < n; i++)
						listeners[i].mouseWheel(Mouse.this, lastMouseWheel);
					break;
				}
			}
		}, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_WHEEL_EVENT_MASK);
	}

	public int getX () {
		return x;
	}

	public int getY () {
		return y;
	}

	public boolean isPressed (int button) {
		if (button < 1 || button > 3) throw new IllegalArgumentException("button must be => 1 and <= 3.");
		return buttons[button];
	}

	public boolean isPressed () {
		return buttons[1] || buttons[2] || buttons[3];
	}

	public void reset () {
		buttons = new boolean[4];
	}

	public void grab (JFrame grabbedFrame) {
		this.grabbedFrame = grabbedFrame;
	}

	public void release () {
		grabbedFrame = null;
	}

	public void addListener (Listener listener) {
		listeners.addListener(listener);
	}

	public void removeListener (Listener listener) {
		listeners.removeListener(listener);
	}

	public boolean resetLastInput () {
		lastButton = 0;
		lastMouseWheel = 0;
		lastDeltaX = 0;
		lastDeltaY = 0;
		return true;
	}

	public Input getLastInput () {
		if (lastButton != 0) return new MouseInput(lastButton);
		if (lastMouseWheel != 0) return new MouseInput(true);
		if (currentDeltaX != 0) return new MouseInput("x");
		if (currentDeltaY != 0) return new MouseInput("y");
		return null;
	}

	public boolean poll () {
		currentDeltaX = lastDeltaX;
		currentDeltaY = lastDeltaY;
		lastDeltaX = 0;
		lastDeltaY = 0;
		return true;
	}

	public String toString () {
		return "Mouse";
	}

	static public class MouseInput implements Input {
		private String axis;
		private int button;
		private boolean mouseWheel;
		private transient float lastState = Float.NaN;

		public MouseInput () {
		}

		public MouseInput (String axis) {
			this.axis = axis;
		}

		public MouseInput (int button) {
			this.button = button;
		}

		public MouseInput (boolean mouseWheel) {
			this.mouseWheel = mouseWheel;
		}

		public Float getState (InputTrigger trigger) {
			if (button > 0) {
				float state = instance.isPressed(button) ? 1 : 0;
				if (state != 0 && !trigger.checkModifiers()) return null;
				if (state == lastState) return null;
				lastState = state;
				return state;
			}
			if ("x".equals(axis)) {
				if (!trigger.checkModifiers()) return null;
				float state = instance.currentDeltaX;
				if (state == 0 && lastState == 0) return null;
				lastState = state;
				return state;
			}
			if ("y".equals(axis)) {
				if (!trigger.checkModifiers()) return null;
				float state = instance.currentDeltaY;
				if (state == 0 && lastState == 0) return null;
				lastState = state;
				return state;
			}
			if (mouseWheel) {
				if (!trigger.checkModifiers()) return null;
				float state = (float)instance.lastMouseWheel;
				if (state == 0) return null;
				instance.lastMouseWheel = 0;
				return state;
			}
			return null;
		}

		public Mouse getInputDevice () {
			return instance;
		}

		public boolean isValid () {
			return true;
		}

		public String toString () {
			if (button == 1) return "Left button";
			if (button == 2) return "Middle button";
			if (button == 3) return "Right button";
			if ("x".equals(axis)) return "X axis";
			if ("y".equals(axis)) return "Y axis";
			if (mouseWheel) return "Wheel";
			return "<none>";
		}
	}

	static public class Listener {
		public void mouseDown (Mouse mouse, int button) {
		}

		public void mouseUp (Mouse mouse, int button) {
		}

		public void mouseMoved (Mouse mouse, int deltaX, int deltaY) {
		}

		public void mouseWheel (Mouse mouse, int delta) {
		}
	}
}
