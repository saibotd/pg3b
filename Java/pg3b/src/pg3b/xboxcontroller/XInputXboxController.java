
package pg3b.xboxcontroller;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;

import pg3b.Axis;
import pg3b.Button;

public class XInputXboxController extends XboxController {
	static private final int BUTTON_DPAD_UP = 0x00000001;
	static private final int BUTTON_DPAD_DOWN = 0x00000002;
	static private final int BUTTON_DPAD_LEFT = 0x00000004;
	static private final int BUTTON_DPAD_RIGHT = 0x00000008;
	static private final int BUTTON_START = 0x00000010;
	static private final int BUTTON_BACK = 0x00000020;
	static private final int BUTTON_LEFT_THUMB = 0x00000040;
	static private final int BUTTON_RIGHT_THUMB = 0x00000080;
	static private final int BUTTON_LEFT_SHOULDER = 0x0100;
	static private final int BUTTON_RIGHT_SHOULDER = 0x0200;
	static private final int BUTTON_A = 0x1000;
	static private final int BUTTON_B = 0x2000;
	static private final int BUTTON_X = 0x4000;
	static private final int BUTTON_Y = 0x8000;

	static private final XInputXboxController[] controllers = {new XInputXboxController(0), new XInputXboxController(1),
		new XInputXboxController(2), new XInputXboxController(3)};

	private final int index;
	private final ByteBuffer byteBuffer;
	private final ShortBuffer shortBuffer;
	private boolean isConnected;
	private int buttons;
	private int leftTrigger, rightTrigger;
	private int thumbLX, thumbLY;
	private int thumbRX, thumbRY;

	private XInputXboxController (int index) {
		this.index = index;

		byteBuffer = ByteBuffer.allocateDirect(16);
		byteBuffer.order(ByteOrder.nativeOrder());
		shortBuffer = byteBuffer.asShortBuffer();
	}

	public boolean poll () {
		poll(index, byteBuffer);
		isConnected = shortBuffer.get(0) != 0;
		if (!isConnected) return false;
		buttons = shortBuffer.get(1);
		leftTrigger = shortBuffer.get(2);
		rightTrigger = shortBuffer.get(3);
		thumbLX = shortBuffer.get(4);
		thumbLY = shortBuffer.get(5);
		thumbRX = shortBuffer.get(6);
		thumbRY = shortBuffer.get(7);
		return true;
	}

	public float get (Axis axis) {
		switch (axis) {
		case leftStickX:
			return thumbLX / 32767f;
		case leftStickY:
			return thumbLY / 32767f;
		case rightStickX:
			return thumbRX / 32767f;
		case rightStickY:
			return thumbRY / 32767f;
		case leftTrigger:
			return leftTrigger / 255f;
		case rightTrigger:
			return rightTrigger / 255f;
		}
		throw new RuntimeException();
	}

	public boolean get (Button button) {
		switch (button) {
		case up:
			return (buttons & BUTTON_DPAD_UP) == BUTTON_DPAD_UP;
		case down:
			return (buttons & BUTTON_DPAD_DOWN) == BUTTON_DPAD_DOWN;
		case left:
			return (buttons & BUTTON_DPAD_LEFT) == BUTTON_DPAD_LEFT;
		case right:
			return (buttons & BUTTON_DPAD_RIGHT) == BUTTON_DPAD_RIGHT;
		case start:
			return (buttons & BUTTON_START) == BUTTON_START;
		case guide:
			// The Xbox controller driver doesn't expose this button!
			return false;
		case a:
			return (buttons & BUTTON_A) == BUTTON_A;
		case b:
			return (buttons & BUTTON_B) == BUTTON_B;
		case x:
			return (buttons & BUTTON_X) == BUTTON_X;
		case y:
			return (buttons & BUTTON_Y) == BUTTON_Y;
		case leftShoulder:
			return (buttons & BUTTON_LEFT_SHOULDER) == BUTTON_LEFT_SHOULDER;
		case rightShoulder:
			return (buttons & BUTTON_RIGHT_SHOULDER) == BUTTON_RIGHT_SHOULDER;
		case back:
			return (buttons & BUTTON_BACK) == BUTTON_BACK;
		case leftStick:
			return (buttons & BUTTON_LEFT_THUMB) == BUTTON_LEFT_THUMB;
		case rightStick:
			return (buttons & BUTTON_RIGHT_THUMB) == BUTTON_RIGHT_THUMB;
		}
		throw new RuntimeException();
	}

	public boolean isConnected () {
		return isConnected;
	}

	public String getName () {
		return "Xbox Controller " + index;
	}

	public int getPort () {
		return index;
	}

	static public List<XInputXboxController> getXInputControllers () {
		return Arrays.asList(controllers);
	}

	native static private void poll (int index, ByteBuffer buffer);

	native static public void setEnabled (boolean enabled);
}
