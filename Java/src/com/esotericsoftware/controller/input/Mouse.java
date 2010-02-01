
package com.esotericsoftware.controller.input;

import static com.esotericsoftware.minlog.Log.*;

import java.awt.AWTEvent;
import java.awt.AWTException;
import java.awt.Component;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.esotericsoftware.controller.util.Listeners;

/**
 * The input device for the system wide mouse.
 */
public class Mouse implements InputDevice {
	static public final Mouse instance = new Mouse();

	private int x, y;
	private boolean[] buttons = new boolean[4];
	private Listeners<Listener> listeners = new Listeners(Listener.class);
	private int lastButton, lastDeltaX, lastDeltaY, lastMouseWheel;
	private int currentDeltaX, currentDeltaY, currentMouseWheel;
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
					Point screenPoint = event.getPoint();
					SwingUtilities.convertPointToScreen(screenPoint, (Component)event.getSource());
					x = screenPoint.x;
					y = screenPoint.y;
					lastDeltaX += x - lastX;
					lastDeltaY += y - lastY;
					for (int i = 0, n = listeners.length; i < n; i++)
						listeners[i].mouseMoved(x - lastX, y - lastY);
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
						listeners[i].mouseDown(lastButton);
					break;
				case MouseEvent.MOUSE_RELEASED:
					int button = event.getButton();
					buttons[button] = false;
					for (int i = 0, n = listeners.length; i < n; i++)
						listeners[i].mouseUp(button);
					break;
				case MouseEvent.MOUSE_WHEEL:
					lastMouseWheel = ((MouseWheelEvent)event).getWheelRotation();
					for (int i = 0, n = listeners.length; i < n; i++)
						listeners[i].mouseWheel(lastMouseWheel);
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
		if (grabbedFrame != null && robot != null) {
			x = grabbedFrame.getX() + grabbedFrame.getWidth() / 2;
			y = grabbedFrame.getY() + grabbedFrame.getHeight() / 2;
			robot.mouseMove(x, y);
		}
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
		if (currentMouseWheel != 0) return new MouseInput(true);
		if (currentDeltaX != 0) return new MouseInput("x");
		if (currentDeltaY != 0) return new MouseInput("y");
		return null;
	}

	public boolean poll () {
		currentDeltaX = lastDeltaX;
		currentDeltaY = lastDeltaY;
		lastDeltaX = 0;
		lastDeltaY = 0;
		currentMouseWheel = lastMouseWheel;
		lastMouseWheel = 0;
		return true;
	}

	public String toString () {
		return "Mouse";
	}

	static public class MouseInput implements Input {
		static private final long MOUSEWHEEL_CHANGE_TIME = 32;

		private String axis;
		private int button;
		private boolean mouseWheel;

		private transient long mouseWheelTime;
		private transient int lastMoustWheel;

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

		public float getState () {
			if (button > 0) return instance.isPressed(button) ? 1 : 0;
			if ("x".equals(axis)) return instance.currentDeltaX;
			if ("y".equals(axis)) return instance.currentDeltaY;
			if (mouseWheel) {
				if (mouseWheelTime > 0) {
					if (System.currentTimeMillis() < mouseWheelTime) return lastMoustWheel;
					mouseWheelTime = 0;
				}
				lastMoustWheel = instance.currentMouseWheel;
				if (lastMoustWheel != 0) 
					mouseWheelTime = System.currentTimeMillis() + MOUSEWHEEL_CHANGE_TIME;
				return lastMoustWheel;
			}
			return 0;
		}

		public float getOtherState () {
			return 0;
		}

		public Mouse getInputDevice () {
			return instance;
		}

		public boolean isValid () {
			return true;
		}

		public boolean isAxis () {
			return axis != null;
		}

		public boolean isAxisX () {
			return axis != null && axis.equals("x");
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

		public int hashCode () {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((axis == null) ? 0 : axis.hashCode());
			result = prime * result + button;
			result = prime * result + (mouseWheel ? 1231 : 1237);
			return result;
		}

		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			MouseInput other = (MouseInput)obj;
			if (axis == null) {
				if (other.axis != null) return false;
			} else if (!axis.equals(other.axis)) return false;
			if (button != other.button) return false;
			if (mouseWheel != other.mouseWheel) return false;
			return true;
		}
	}

	static public class Listener {
		public void mouseDown (int button) {
		}

		public void mouseUp (int button) {
		}

		public void mouseMoved (int deltaX, int deltaY) {
		}

		public void mouseWheel (int delta) {
		}
	}
}
