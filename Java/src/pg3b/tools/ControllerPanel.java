
package pg3b.tools;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import pg3b.PG3B;
import pg3b.XboxController;
import pg3b.PG3B.Button;
import pg3b.PG3B.Target;
import pg3b.tools.util.PackedImages;
import pg3b.tools.util.Sound;
import pg3b.tools.util.PackedImages.PackedImage;

public class ControllerPanel extends JPanel {
	static public final String[] imageNames = {"y", "a", "b", "back", "guide", "leftShoulder", "leftStick", "leftTrigger",
		"rightShoulder", "rightStick", "rightTrigger", "start", "x", "up", "down", "left", "right"};
	static final List<String> clickOnlyButtons = Arrays.asList("leftStick", "leftTrigger", "rightStick", "rightTrigger", "up",
		"down", "left", "right");
	static final int deadzone = 10, stickDistance = 80;

	static final int DPAD_NONE = 0, DPAD_DEADZONE = 2, DPAD_UP = 4, DPAD_DOWN = 8, DPAD_LEFT = 16, DPAD_RIGHT = 32;

	PG3B pg3b;
	XboxController controller;
	PackedImages packedImages;
	String overImageName;
	int dragStartX = -1, dragStartY = -1;
	int dpadDirection;
	float lastTriggerValue, lastValueX, lastValueY;
	Map<String, Boolean> nameToStatus;
	BufferedImage checkImage, xImage;

	public ControllerPanel () {
		Sound.register("click");

		setMinimumSize(new Dimension(497, 337));
		setMaximumSize(new Dimension(497, 337));
		setPreferredSize(new Dimension(497, 337));
		setOpaque(false);

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
					if (dragObject == Target.leftTrigger || dragObject == Target.rightTrigger) {
						dragStartX = x;
						dragStartY = y;
					} else if (dragObject == Target.leftStickX) {
						dragStartX = 63;
						dragStartY = 191;
					} else if (dragObject == Target.rightStickX) {
						dragStartX = 331;
						dragStartY = 275;
					} else if (dragObject instanceof Button) {
						dragStartX = 165;
						dragStartY = 261;
					}
					repaint();
				}

				if (dragObject == Target.leftTrigger || dragObject == Target.rightTrigger) {
					float value = Math.max(0, Math.min(stickDistance, y - dragStartY)) / (float)stickDistance;
					if (value != lastTriggerValue) {
						triggerDragged((Target)dragObject, value);
						lastTriggerValue = value;
					}

				} else if (dragObject == Target.leftStickX || dragObject == Target.rightStickX) {
					float valueX = 0;
					if (Math.abs(x - dragStartX) > deadzone) {
						valueX = x - dragStartX;
						valueX -= valueX < 0 ? -deadzone : deadzone;
						valueX = Math.max(-stickDistance, Math.min(stickDistance, valueX)) / (float)stickDistance;
					}
					if (valueX != lastValueX) {
						stickDragged((Target)dragObject, valueX);
						lastValueX = valueX;
					}

					float valueY = 0;
					if (Math.abs(y - dragStartY) > deadzone) {
						valueY = y - dragStartY;
						valueY -= valueY < 0 ? -deadzone : deadzone;
						valueY = Math.max(-stickDistance, Math.min(stickDistance, valueY)) / (float)stickDistance;
					}
					if (valueY != lastValueY) {
						Target targetY = dragObject == Target.leftStickX ? Target.leftStickY : Target.rightStickY;
						stickDragged(targetY, valueY);
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
				if (overImageName != null && !clickOnlyButtons.contains(overImageName))
					buttonClicked(Button.valueOf(overImageName), true);
			}

			public void mouseReleased (MouseEvent event) {
				if (dragStartX != -1) {
					dragStartX = dragStartY = -1;
					Object dragObject = getDragObject();
					if (dragObject == Target.leftTrigger || dragObject == Target.rightTrigger) {
						triggerDragged((Target)dragObject, 0);
					} else if (dragObject == Target.leftStickX || dragObject == Target.rightStickX) {
						stickDragged((Target)dragObject, 0);
						Target targetY = dragObject == Target.leftStickX ? Target.leftStickY : Target.rightStickY;
						stickDragged(targetY, 0);
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
						triggerDragged(Target.valueOf(overImageName), 1);
						triggerDragged(Target.valueOf(overImageName), 0);
						Sound.play("click");
					} else {
						Button stickButton = Button.valueOf(overImageName);
						buttonClicked(stickButton, true);
						buttonClicked(stickButton, false);
					}
				}
			}
		};
		addMouseMotionListener(mouseListener);
		addMouseListener(mouseListener);

	}

	protected void triggerDragged (Target target, float value) {
		repaint();
		try {
			if (pg3b != null) pg3b.set(target, value);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	protected void stickDragged (Target target, float value) {
		repaint();
		try {
			if (pg3b != null) pg3b.set(target, value);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	protected void buttonClicked (Button button, boolean pressed) {
		if (pressed) Sound.play("click");
		repaint();
		try {
			if (pg3b != null) pg3b.set(button, pressed);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	protected void dpadDragged (Button button, boolean pressed) {
		repaint();
		try {
			if (pg3b != null) pg3b.set(button, pressed);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	Object getDragObject () {
		if (overImageName == null) return null;
		if (overImageName.equals("leftStick")) return Target.leftStickX;
		if (overImageName.equals("rightStick")) return Target.rightStickX;
		if (overImageName.equals("leftTrigger")) return Target.leftTrigger;
		if (overImageName.equals("rightTrigger")) return Target.rightTrigger;
		if (overImageName.equals("up")) return Button.up;
		if (overImageName.equals("down")) return Button.down;
		if (overImageName.equals("left")) return Button.left;
		if (overImageName.equals("right")) return Button.right;
		return null;
	}

	protected void paintComponent (Graphics g) {
		g.setFont(g.getFont().deriveFont(10f));

		packedImages.get("controller").draw(g, 0, 0);

		if (dpadDirection != DPAD_NONE) {
			if ((dpadDirection & DPAD_RIGHT) == DPAD_RIGHT) packedImages.get("right").draw(g, 0, 0);
			if ((dpadDirection & DPAD_LEFT) == DPAD_LEFT) packedImages.get("left").draw(g, 0, 0);
			if ((dpadDirection & DPAD_UP) == DPAD_UP) packedImages.get("up").draw(g, 0, 0);
			if ((dpadDirection & DPAD_DOWN) == DPAD_DOWN) packedImages.get("down").draw(g, 0, 0);
		} else {
			if (overImageName != null) packedImages.get(overImageName).draw(g, 0, 0);
		}

		if (dragStartX != -1) {
			// Drag in progress, don't show controller input.
			g.setColor(Color.black);
			Object dragObject = getDragObject();
			if (dragObject == Target.leftTrigger) {
				drawString(g, toPercent(lastTriggerValue), 104, 32);
			} else if (dragObject == Target.rightTrigger) {
				drawString(g, toPercent(lastTriggerValue), 392, 32);
			} else if (dragObject == Target.leftStickX) {
				int x = 0;
				int y = 129;
				if (lastValueY < 0) {
					packedImages.get("upArrow").draw(g, x, y);
					drawString(g, toPercent(lastValueY), x + 62, y + 27);
				} else if (lastValueY > 0) {
					packedImages.get("downArrow").draw(g, x, y);
					drawString(g, toPercent(lastValueY), x + 62, y + 27 + 77);
				}
				if (lastValueX < 0) {
					packedImages.get("leftArrow").draw(g, x, y);
					drawString(g, toPercent(lastValueX), x + 62 - 44, y + 27 + 39);
				} else if (lastValueX > 0) {
					packedImages.get("rightArrow").draw(g, x, y);
					drawString(g, toPercent(lastValueX), x + 62 + 43, y + 27 + 39);
				}
			} else if (dragObject == Target.rightStickX) {
				int x = 268;
				int y = 213;
				if (lastValueY < 0) {
					packedImages.get("upArrow").draw(g, x, y);
					drawString(g, toPercent(lastValueY), x + 62, y + 27);
				} else if (lastValueY > 0) {
					packedImages.get("downArrow").draw(g, x, y);
					drawString(g, toPercent(lastValueY), x + 62, y + 27 + 77);
				}
				if (lastValueX < 0) {
					packedImages.get("leftArrow").draw(g, x, y);
					drawString(g, toPercent(lastValueX), x + 62 - 44, y + 27 + 39);
				} else if (lastValueX > 0) {
					packedImages.get("rightArrow").draw(g, x, y);
					drawString(g, toPercent(lastValueX), x + 62 + 43, y + 27 + 39);
				}
			}
		} else if (controller != null) {
			// No drag in progress, show controller input.
			if (controller.get(Button.a)) packedImages.get("a").draw(g, 0, 0);
		}

		if (nameToStatus != null) {
			for (Entry<String, Boolean> entry : nameToStatus.entrySet()) {
				PackedImage packedImage = packedImages.get(entry.getKey());
				if (packedImage == null) continue;
				int x = packedImage.offsetX + packedImage.image.getWidth() / 2;
				int y = packedImage.offsetY + packedImage.image.getHeight() / 2;
				BufferedImage image = entry.getValue() ? checkImage : xImage;
				g.drawImage(image, x - (entry.getValue() ? 13 : 16), y - (entry.getValue() ? 24 : 16), null);
			}
			// BOZO - Need to show marks for stick axes.
		}

		if (dragStartX != -1 && !overImageName.endsWith("Trigger"))
			packedImages.get("crosshair").draw(g, dragStartX - 11, dragStartY - 11);
	}

	private String toPercent (float value) {
		return String.valueOf((int)(value * 100));
	}

	private void drawString (Graphics g, String text, int x, int y) {
		int width = g.getFontMetrics().stringWidth(text);
		g.drawString(text, x - width / 2, y);
	}

	public void setPg3b (PG3B pg3b) {
		this.pg3b = pg3b;
	}

	public void setController (XboxController controller) {
		this.controller = controller;
	}

	public void setStatus (Map<String, Boolean> nameToStatus) {
		this.nameToStatus = nameToStatus;
		repaint();
	}
}
