
package pg3b.tools;

import static com.esotericsoftware.minlog.Log.*;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;

import javax.swing.JButton;
import javax.swing.JPanel;

import pg3b.PG3B;
import pg3b.XboxController;
import pg3b.PG3B.Button;
import pg3b.PG3B.Target;
import pg3b.tools.util.LoaderDialog;

import com.esotericsoftware.minlog.Log;

public class DiagnosticsTab extends JPanel {
	static private final int TIMEOUT = 250;

	PG3BTool owner;
	private JButton roundTripTestButton;
	private JButton clearButton;
	private JButton calibrateButton;

	public DiagnosticsTab (PG3BTool owner) {
		this.owner = owner;
		initializeLayout();
		initializeEvents();
	}

	private void initializeEvents () {
		roundTripTestButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				new LoaderDialog("Round trip diagnostic") {
					public void load () throws Exception {
						owner.getControllerPanel().setStatus(null);

						PG3B pg3b = owner.getPg3b();
						XboxController controller = owner.getController();
						HashMap<String, Boolean> status = new HashMap();

						for (Button button : Button.values()) {
							if (button == Button.start || button == Button.guide) continue;
							setMessage("Testing " + button + "...");
							throwCancelled();
							boolean success = waitForButton(pg3b, controller, button, false);
							throwCancelled();
							if (success) success = waitForButton(pg3b, controller, button, true);
							throwCancelled();
							if (success) success = waitForButton(pg3b, controller, button, false);
							status.put(button.toString(), success);
						}

						for (Target target : Target.values()) {
							setMessage("Testing " + target + "...");
							throwCancelled();
							boolean success = waitForTarget(pg3b, controller, target, 0);
							throwCancelled();
							if (success) success = waitForTarget(pg3b, controller, target, 1);
							throwCancelled();
							if (success) success = waitForTarget(pg3b, controller, target, 0);
							status.put(target.toString(), success);
						}

						owner.getControllerPanel().setStatus(status);
					}
				}.start("RoundTripTest");
			}
		});

		clearButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				owner.getControllerPanel().setStatus(null);
			}
		});

		calibrateButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				new LoaderDialog("Calibration") {
					LinkedHashMap<String, String> nameToURL = new LinkedHashMap();

					public void load () throws Exception {
						PG3B pg3b = owner.getPg3b();
						XboxController controller = owner.getController();
						if (false) {
							int i = 0;
							Target[] values = Target.values();
							for (Target target : values) {
								setMessage("Calibrating " + target + "...");
								setPercentageComplete(i++ / (float)values.length);
								throwCancelled();
								String url = pg3b.calibrate(target, controller);
								nameToURL.put(target.toString(), url);
							}
						} else {
							String url = pg3b.calibrate(Target.leftStickY, controller);
							nameToURL.put(Target.leftStickY.toString(), url);
						}
					}

					public void complete () {
						CalibrationResultsFrame frame = new CalibrationResultsFrame(nameToURL);
						frame.setLocationRelativeTo(owner);
						frame.setVisible(true);
					}
				}.start("Calibrate");
			}
		});
	}

	boolean waitForButton (PG3B pg3b, XboxController controller, Button button, boolean pressed) {
		try {
			pg3b.set(button, pressed);
			long startTime = System.currentTimeMillis();
			while (controller.get(button) != pressed) {
				if (System.currentTimeMillis() - startTime > TIMEOUT) {
					if (WARN) warn("Timed out setting button: " + button);
					return false;
				}
				Thread.yield();
			}
			return true;
		} catch (IOException ex) {
			if (Log.ERROR) error("Error setting button: " + button, ex);
			return false;
		}
	}

	boolean waitForTarget (PG3B pg3b, XboxController controller, Target target, float value) {
		try {
			pg3b.set(target, value);
			long startTime = System.currentTimeMillis();
			while (controller.get(target) != value) {
				controller.poll();
				if (System.currentTimeMillis() - startTime > TIMEOUT) {
					if (WARN) warn("Timed out setting target: " + target);
					return false;
				}
				Thread.yield();
			}
			return true;
		} catch (IOException ex) {
			if (Log.ERROR) error("Error setting target: " + target, ex);
			return false;
		}
	}

	private void initializeLayout () {
		setLayout(new GridBagLayout());
		{
			roundTripTestButton = new JButton("Round Trip");
			add(roundTripTestButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST,
				GridBagConstraints.NONE, new Insets(6, 6, 6, 6), 0, 0));
		}
		{
			clearButton = new JButton("Clear");
			add(clearButton, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
				new Insets(6, 0, 6, 6), 0, 0));
		}
		{
			calibrateButton = new JButton("Calibrate");
			this.add(calibrateButton, new GridBagConstraints(0, 1, 2, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST,
				GridBagConstraints.NONE, new Insets(0, 6, 6, 6), 0, 0));
		}
	}

	public void setPg3b (PG3B pg3b) {
		roundTripTestButton.setEnabled(pg3b != null && owner.getController() != null);
		clearButton.setEnabled(roundTripTestButton.isEnabled());
	}

	public void setController (XboxController controller) {
		roundTripTestButton.setEnabled(controller != null && owner.getPg3b() != null);
		clearButton.setEnabled(roundTripTestButton.isEnabled());
	}
}
