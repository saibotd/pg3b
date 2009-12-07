
package pg3b.ui.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import pg3b.Axis;
import pg3b.Button;
import pg3b.PG3B;
import pg3b.Stick;
import pg3b.Target;
import pg3b.XboxController;
import pg3b.util.Listeners;
import pg3b.util.PackedImages;
import pg3b.util.Sound;
import pg3b.util.PackedImages.PackedImage;

public class XboxControllerPanel extends JPanel {
	static public final String[] imageNames = {"y", "a", "b", "back", "guide", "leftShoulder", "leftStick", "leftTrigger",
		"rightShoulder", "rightStick", "rightTrigger", "start", "x", "up", "down", "left", "right"};
	static final List<String> clickOnlyButtons = Arrays.asList("leftStick", "leftTrigger", "rightStick", "rightTrigger", "up",
		"down", "left", "right");
	static final int deadzone = 10, stickDistance = 80;
	static final int DPAD_NONE = 0, DPAD_DEADZONE = 2, DPAD_UP = 4, DPAD_DOWN = 8, DPAD_LEFT = 16, DPAD_RIGHT = 32;
	static final Timer timer = new Timer("PollController", true);

	private PG3B pg3b;
	private XboxController controller;
	private PackedImages packedImages;
	private String overImageName;
	private int dragStartX = -1, dragStartY = -1;
	private int dpadDirection;
	private float lastTriggerValue, lastValueX, lastValueY;
	private Map<Target, Boolean> nameToStatus;
	private BufferedImage checkImage, xImage;
	private Listeners<Listener> listeners = new Listeners(Listener.class);
	private TimerTask pollControllerTask;

	private XboxController.Listener controllerListener = new XboxController.Listener() {
		public void buttonChanged (Button button, boolean pressed) {
			repaint();
		}

		public void axisChanged (Axis axis, float state) {
			repaint();
		}
	};

	private PG3B.Listener pg3bListener = new PG3B.Listener() {
		public void buttonChanged (Button button, boolean pressed) {
			repaint();
		}

		public void axisChanged (Axis axis, float state) {
			repaint();
		}
	};

	public XboxControllerPanel () {
		setMinimumSize(new Dimension(497, 337));
		setMaximumSize(new Dimension(497, 337));
		setPreferredSize(new Dimension(497, 337));
		setOpaque(false);

		Sound.register("click");

		try {
			checkImage = ImageIO.read(getClass().getResource("/check.png"));
			xImage = ImageIO.read(getClass().getResource("/x.png"));
		} catch (IOException ex) {
			throw new RuntimeException("Image resources not found.", ex);
		}

		try {
			packedImages = new PackedImages("/controller.pack");
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}

		MouseAdapter mouseListener = new MouseAdapter() {
			private int buttonsDown;

			public void mouseMoved (MouseEvent event) {
				// Highlight buttons when moused over.
				int x = event.getX(), y = event.getY();
				String newOverButtonName = null;
				int closest = Integer.MAX_VALUE;
				for (String imageName : imageNames) {
					PackedImage packedImage = packedImages.get(imageName);
					if (x > packedImage.offsetX && x < packedImage.offsetX + packedImage.image.getWidth()) {
						if (y > packedImage.offsetY && y < packedImage.offsetY + packedImage.image.getHeight()) {
							int dx = Math.abs(x - (packedImage.offsetX + packedImage.image.getWidth() / 2));
							int dy = Math.abs(y - (packedImage.offsetY + packedImage.image.getHeight() / 2));
							int distance = dx * dx + dy * dy;
							if (distance < closest) {
								closest = distance;
								newOverButtonName = imageName;
							}
						}
					}
				}
				if (newOverButtonName != overImageName) {
					overImageName = newOverButtonName;
					repaint();
				}
			}

			public void mouseDragged (MouseEvent event) {
				// Drag to manipulate stick and trigger axes.
				if (overImageName == null) return;

				Object dragObject = getDragObject();
				if (dragObject == null) return;

				int x = event.getX(), y = event.getY();
				if (dragStartX == -1) {
					if (dragObject == Axis.leftTrigger || dragObject == Axis.rightTrigger) {
						dragStartX = x;
						dragStartY = y;
					} else if (dragObject == Axis.leftStickX) {
						dragStartX = 63;
						dragStartY = 191;
					} else if (dragObject == Axis.rightStickX) {
						dragStartX = 331;
						dragStartY = 275;
					} else if (dragObject instanceof Button) {
						dragStartX = 165;
						dragStartY = 261;
					}
					repaint();
				}

				if (dragObject == Axis.leftTrigger || dragObject == Axis.rightTrigger) {
					float value = Math.max(0, Math.min(stickDistance, y - dragStartY)) / (float)stickDistance;
					if (value != lastTriggerValue) {
						triggerDragged((Axis)dragObject, value);
						lastTriggerValue = value;
					}

				} else if (dragObject == Axis.leftStickX || dragObject == Axis.rightStickX) {
					float valueX = 0;
					if (Math.abs(x - dragStartX) > deadzone) {
						valueX = x - dragStartX;
						valueX -= valueX < 0 ? -deadzone : deadzone;
						valueX = Math.max(-stickDistance, Math.min(stickDistance, valueX)) / (float)stickDistance;
					}
					if (valueX != lastValueX) {
						stickDragged((Axis)dragObject, valueX);
						lastValueX = valueX;
					}

					float valueY = 0;
					if (Math.abs(y - dragStartY) > deadzone) {
						valueY = y - dragStartY;
						valueY -= valueY < 0 ? -deadzone : deadzone;
						valueY = Math.max(-stickDistance, Math.min(stickDistance, valueY)) / (float)stickDistance;
					}
					if (valueY != lastValueY) {
						Axis axisY = dragObject == Axis.leftStickX ? Axis.leftStickY : Axis.rightStickY;
						stickDragged(axisY, valueY);
						lastValueY = valueY;
					}

				} else if (dragObject instanceof Button) {
					int newDirection = 0;
					if (x > dragStartX + deadzone) newDirection |= DPAD_RIGHT;
					if (x < dragStartX - deadzone) newDirection |= DPAD_LEFT;
					if (y > dragStartY + deadzone) newDirection |= DPAD_DOWN;
					if (y < dragStartY - deadzone) newDirection |= DPAD_UP;
					if (newDirection == 0) newDirection = DPAD_DEADZONE;
					// If the direction has changed, press or release the dpad buttons.
					int diff = dpadDirection ^ newDirection;
					if ((diff & DPAD_RIGHT) != 0) dpadDragged(Button.right, (newDirection & DPAD_RIGHT) == DPAD_RIGHT);
					if ((diff & DPAD_LEFT) != 0) dpadDragged(Button.left, (newDirection & DPAD_LEFT) == DPAD_LEFT);
					if ((diff & DPAD_DOWN) != 0) dpadDragged(Button.down, (newDirection & DPAD_DOWN) == DPAD_DOWN);
					if ((diff & DPAD_UP) != 0) dpadDragged(Button.up, (newDirection & DPAD_UP) == DPAD_UP);
					dpadDirection = newDirection;
					repaint();
				}
			}

			public void mousePressed (MouseEvent event) {
				buttonsDown++;
				if (overImageName != null && !clickOnlyButtons.contains(overImageName))
					buttonClicked(Button.valueOf(overImageName), true);
			}

			public void mouseReleased (MouseEvent event) {
				buttonsDown--;
				if (dragStartX != -1) {
					dragStartX = dragStartY = -1;
					Object dragObject = getDragObject();
					if (dragObject == Axis.leftTrigger || dragObject == Axis.rightTrigger) {
						triggerDragged((Axis)dragObject, 0);
					} else if (dragObject == Axis.leftStickX || dragObject == Axis.rightStickX) {
						stickDragged((Axis)dragObject, 0);
						Axis axisY = dragObject == Axis.leftStickX ? Axis.leftStickY : Axis.rightStickY;
						stickDragged(axisY, 0);
					} else if (dragObject instanceof Button) {
						if ((dpadDirection & DPAD_RIGHT) == DPAD_RIGHT) buttonClicked(Button.right, false);
						if ((dpadDirection & DPAD_LEFT) == DPAD_LEFT) buttonClicked(Button.left, false);
						if ((dpadDirection & DPAD_DOWN) == DPAD_DOWN) buttonClicked(Button.down, false);
						if ((dpadDirection & DPAD_UP) == DPAD_UP) buttonClicked(Button.up, false);
						dpadDirection = DPAD_NONE;
					}
					repaint();
				}

				if (overImageName != null && !clickOnlyButtons.contains(overImageName))
					buttonClicked(Button.valueOf(overImageName), false);

				mouseMoved(event);
			}

			public void mouseClicked (MouseEvent event) {
				if (overImageName != null && clickOnlyButtons.contains(overImageName)) {
					if (overImageName.endsWith("Trigger")) {
						triggerDragged(Axis.valueOf(overImageName), 1);
						triggerDragged(Axis.valueOf(overImageName), 0);
						if (pg3b != null) Sound.play("click");
					} else {
						Button stickButton = Button.valueOf(overImageName);
						buttonClicked(stickButton, true);
						buttonClicked(stickButton, false);
					}
				}
			}

			public void mouseExited (MouseEvent event) {
				if (buttonsDown == 0) {
					overImageName = null;
					repaint();
				}
			}
		};
		addMouseMotionListener(mouseListener);
		addMouseListener(mouseListener);
	}

	void triggerDragged (Axis axis, float value) {
		repaint();
		try {
			if (pg3b != null) pg3b.set(axis, value);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		notifyListeners(axis, value);
	}

	void stickDragged (Axis axis, float value) {
		repaint();
		try {
			if (pg3b != null) pg3b.set(axis, value);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		notifyListeners(axis, value);
	}

	void buttonClicked (Button button, boolean pressed) {
		if (pressed && pg3b != null) Sound.play("click");
		repaint();
		try {
			if (pg3b != null) pg3b.set(button, pressed);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		notifyListeners(button, pressed);
	}

	void dpadDragged (Button button, boolean pressed) {
		repaint();
		try {
			if (pg3b != null) pg3b.set(button, pressed);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		notifyListeners(button, pressed);
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
	}

	public void removeListener (Listener listener) {
		listeners.removeListener(listener);
	}

	Object getDragObject () {
		if (overImageName == null) return null;
		if (overImageName.equals("leftStick")) return Axis.leftStickX;
		if (overImageName.equals("rightStick")) return Axis.rightStickX;
		if (overImageName.equals("leftTrigger")) return Axis.leftTrigger;
		if (overImageName.equals("rightTrigger")) return Axis.rightTrigger;
		if (overImageName.equals("up")) return Button.up;
		if (overImageName.equals("down")) return Button.down;
		if (overImageName.equals("left")) return Button.left;
		if (overImageName.equals("right")) return Button.right;
		return null;
	}

	protected void paintComponent (Graphics g) {
		g.setFont(g.getFont().deriveFont(10f));

		packedImages.get("controller").draw(g, 0, 0);

		for (Button button : Button.values())
			if (getTargetState(button) != 0) packedImages.get(button.name()).draw(g, 0, 0);

		if (pg3b != null && dpadDirection != DPAD_NONE) {
			if ((dpadDirection & DPAD_RIGHT) == DPAD_RIGHT) packedImages.get("right").draw(g, 0, 0);
			if ((dpadDirection & DPAD_LEFT) == DPAD_LEFT) packedImages.get("left").draw(g, 0, 0);
			if ((dpadDirection & DPAD_UP) == DPAD_UP) packedImages.get("up").draw(g, 0, 0);
			if ((dpadDirection & DPAD_DOWN) == DPAD_DOWN) packedImages.get("down").draw(g, 0, 0);
		} else {
			if (overImageName != null) packedImages.get(overImageName).draw(g, 0, 0);
		}

		float leftTrigger = getTargetState(Axis.leftTrigger);
		float rightTrigger = getTargetState(Axis.rightTrigger);
		float leftStickX = getTargetState(Axis.leftStickX);
		float leftStickY = getTargetState(Axis.leftStickY);
		float rightStickX = getTargetState(Axis.rightStickX);
		float rightStickY = getTargetState(Axis.rightStickY);
		if (pg3b != null && dragStartX != -1) {
			Object dragObject = getDragObject();
			if (dragObject == Axis.leftTrigger)
				leftTrigger = lastTriggerValue;
			else if (dragObject == Axis.rightTrigger)
				rightTrigger = lastTriggerValue;
			else if (dragObject == Axis.leftStickX) {
				leftStickX = lastValueX;
				leftStickY = lastValueY;
			} else if (dragObject == Axis.rightStickX) {
				rightStickX = lastValueX;
				rightStickY = lastValueY;
			}
		}
		g.setColor(Color.black);
		drawTrigger(g, Axis.leftTrigger, leftTrigger);
		drawTrigger(g, Axis.rightTrigger, rightTrigger);
		drawStickArrows(g, Stick.left, leftStickX, leftStickY);
		drawStickArrows(g, Stick.right, rightStickX, rightStickY);

		g.setFont(g.getFont().deriveFont(Font.BOLD, 12f));
		if (controller != null)
			drawString(g, "Controller", 250, 38);
		else if (pg3b != null) {
			drawString(g, "PG3B", 250, 38);
		}

		if (nameToStatus != null) {
			// Show button status.
			for (Entry<Target, Boolean> entry : nameToStatus.entrySet()) {
				PackedImage packedImage = packedImages.get(entry.getKey().name());
				if (packedImage == null) continue;
				int x = packedImage.offsetX + packedImage.image.getWidth() / 2;
				int y = packedImage.offsetY + packedImage.image.getHeight() / 2;
				BufferedImage image = entry.getValue() ? checkImage : xImage;
				g.drawImage(image, x - (entry.getValue() ? 13 : 16), y - (entry.getValue() ? 24 : 16), null);
			}
			// Show axes status.
			drawStatusText(g, 25, 245, "X Axis", nameToStatus.get(Axis.leftStickX));
			drawStatusText(g, 25, 245 + 31, "Y Axis", nameToStatus.get(Axis.leftStickY));
			drawStatusText(g, 388, 245, "X Axis", nameToStatus.get(Axis.rightStickX));
			drawStatusText(g, 388, 245 + 31, "Y Axis", nameToStatus.get(Axis.rightStickY));
		}

		if (pg3b != null && dragStartX != -1 && !overImageName.endsWith("Trigger"))
			packedImages.get("crosshair").draw(g, dragStartX - 11, dragStartY - 11);
	}

	private float getTargetState (Target target) {
		if (controller != null) return controller.get(target);
		if (pg3b != null) return pg3b.get(target);
		return 0;
	}

	private void drawStatusText (Graphics g, int x, int y, String text, Boolean status) {
		if (status == null) return;
		g.drawString(text, x + 34, y + 21);
		g.drawImage(status ? checkImage : xImage, x, y, null);
	}

	private void drawTrigger (Graphics g, Axis axis, float value) {
		if ((int)(value * 100) == 0) return;
		packedImages.get(axis.name()).draw(g, 0, 0);
		drawString(g, toPercent(value), axis == Axis.leftTrigger ? 104 : 392, 32);
	}

	private void drawStickArrows (Graphics g, Stick stick, float valueX, float valueY) {
		int x = stick == Stick.left ? 0 : 268;
		int y = stick == Stick.left ? 129 : 213;
		if ((int)(valueY * 100) != 0) {
			if (valueY < 0) {
				packedImages.get("upArrow").draw(g, x, y);
				drawString(g, toPercent(valueY), x + 62, y + 27);
			} else if (valueY > 0) {
				packedImages.get("downArrow").draw(g, x, y);
				drawString(g, toPercent(valueY), x + 62, y + 27 + 77);
			}
		}
		if ((int)(valueX * 100) != 0) {
			if (valueX < 0) {
				packedImages.get("leftArrow").draw(g, x, y);
				drawString(g, toPercent(valueX), x + 62 - 44, y + 27 + 39);
			} else if (valueX > 0) {
				packedImages.get("rightArrow").draw(g, x, y);
				drawString(g, toPercent(valueX), x + 62 + 43, y + 27 + 39);
			}
		}
	}

	private String toPercent (float value) {
		return String.valueOf((int)(value * 100));
	}

	private void drawString (Graphics g, String text, int x, int y) {
		int width = g.getFontMetrics().stringWidth(text);
		g.drawString(text, x - width / 2, y);
	}

	public void setPG3B (PG3B pg3b) {
		if (this.pg3b != null) this.pg3b.removeListener(pg3bListener);
		this.pg3b = pg3b;
		if (pg3b != null) pg3b.addListener(pg3bListener);
		repaint();
	}

	public void setController (final XboxController controller) {
		if (pollControllerTask != null) pollControllerTask.cancel();
		if (controller != null) {
			timer.scheduleAtFixedRate(pollControllerTask = new TimerTask() {
				public void run () {
					controller.poll();
				}
			}, 0, 64);
		}

		if (this.controller != null) this.controller.removeListener(controllerListener);
		this.controller = controller;
		if (controller != null) controller.addListener(controllerListener);
		repaint();
	}

	public void setStatus (Map<Target, Boolean> nameToStatus) {
		this.nameToStatus = nameToStatus;
		repaint();
	}

	static public class Listener {
		public void buttonChanged (Button button, boolean pressed) {
		}

		public void axisChanged (Axis axis, float state) {
		}
	}
}
