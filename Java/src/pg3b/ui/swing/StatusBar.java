
package pg3b.ui.swing;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import pg3b.PG3B;
import pg3b.input.XboxController;
import pg3b.ui.Config;
import pg3b.util.UI;

public class StatusBar extends JPanel {
	private TimerTask clearMessageTask;

	private JLabel pg3bLabel, controllerLabel, configLabel, messageLabel;
	private ImageIcon greenImage, redImage;

	private XboxController lastController;

	private PG3B lastPG3B;

	private Config lastConfig;

	public StatusBar () {
		greenImage = new ImageIcon(getClass().getResource("/green.png"));
		redImage = new ImageIcon(getClass().getResource("/red.png"));

		setLayout(new GridBagLayout());
		setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0, 0, 0)));
		{
			pg3bLabel = new JLabel("PG3B");
			add(pg3bLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(3, 6, 3, 0), 0, 0));
			pg3bLabel.setIcon(redImage);
		}
		{
			controllerLabel = new JLabel("Controller");
			add(controllerLabel, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(3, 6, 3, 0), 0, 0));
			controllerLabel.setIcon(redImage);
		}
		{
			configLabel = new JLabel("Config");
			add(configLabel, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(3, 6, 3, 0), 0, 0));
			configLabel.setIcon(redImage);
		}
		{
			messageLabel = new JLabel();
			messageLabel.setFont(messageLabel.getFont().deriveFont(Font.BOLD));
			this.add(messageLabel, new GridBagConstraints(3, 0, 1, 1, 1.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(0, 0, 0, 6), 0, 0));
		}
	}

	public void setPg3bClickedListener (final Runnable listener) {
		pg3bLabel.addMouseListener(new MouseAdapter() {
			public void mouseClicked (MouseEvent e) {
				listener.run();
			}
		});
	}

	public void setControllerClickedListener (final Runnable listener) {
		controllerLabel.addMouseListener(new MouseAdapter() {
			public void mouseClicked (MouseEvent e) {
				listener.run();
			}
		});
	}

	public void setConfigClickedListener (final Runnable listener) {
		configLabel.addMouseListener(new MouseAdapter() {
			public void mouseClicked (MouseEvent e) {
				listener.run();
			}
		});
	}

	public void setPG3B (final PG3B pg3b) {
		EventQueue.invokeLater(new Runnable() {
			public void run () {
				pg3bLabel.setIcon(pg3b == null ? redImage : greenImage);
				pg3bLabel.setText(pg3b == null ? "PG3B" : "PG3B: " + pg3b.getPort());
				if (lastPG3B != null && pg3b == null)
					setMessage("PG3B disconnected.");
				else if (pg3b != null) {
					setMessage("PG3B connected.");
				}
				lastPG3B = pg3b;
			}
		});
	}

	public void setController (final XboxController controller) {
		controllerLabel.setIcon(controller == null ? redImage : greenImage);
		controllerLabel.setText(controller == null ? "Controller" : "Controller: " + controller.getPort());
		if (lastController != null && controller == null)
			setMessage("Controller disconnected.");
		else if (controller != null) {
			setMessage("Controller connected.");
		}
		lastController = controller;
	}

	public void setConfig (Config config) {
		configLabel.setIcon(config == null ? redImage : greenImage);
		configLabel.setText(config == null ? "Config" : "Config: " + config.getName());
		if (lastConfig != null && config == null)
			setMessage("Config deactivated.");
		else if (config != null) {
			setMessage("Config activated.");
		}
		lastConfig = config;
	}

	public synchronized void setMessage (String message) {
		if (clearMessageTask != null) clearMessageTask.cancel();
		if (message != null && message.length() > 0) {
			clearMessageTask = new TimerTask() {
				float alpha = 1;

				public void run () {
					try {
						EventQueue.invokeAndWait(new Runnable() {
							public void run () {
								alpha -= 0.03f;
								if (alpha > 0)
									messageLabel.setForeground(new Color(0, 0, 0, alpha));
								else {
									cancel();
									clearMessageTask = null;
									setMessage("");
								}
							}
						});
					} catch (Exception ignored) {
					}
				}
			};
			UI.timer.scheduleAtFixedRate(clearMessageTask, 83, 166);
		}
		messageLabel.setText(message);
		messageLabel.setForeground(Color.black);
	}
}
