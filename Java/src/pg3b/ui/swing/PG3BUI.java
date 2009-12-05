
package pg3b.ui.swing;

import static com.esotericsoftware.minlog.Log.*;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import com.esotericsoftware.minlog.Log;

import net.java.games.input.Controller;
import pg3b.Axis;
import pg3b.AxisCalibration;
import pg3b.Diagnostics;
import pg3b.PG3B;
import pg3b.Target;
import pg3b.XboxController;
import pg3b.XboxController.Listener;
import pg3b.ui.Config;
import pg3b.ui.Settings;
import pg3b.util.LoaderDialog;

public class PG3BUI extends JFrame {
	static public PG3BUI instance;

	static private Settings settings = Settings.get();

	private PG3B pg3b;
	private XboxController controller;
	private Config activeConfig;

	private JMenuItem pg3bConnectMenuItem, controllerConnectMenuItem, exitMenuItem;
	private JCheckBoxMenuItem showControllerMenuItem;
	private JMenuItem roundTripMenuItem, clearMenuItem, calibrateMenuItem;

	private XboxControllerPanel controllerPanel;
	private JToggleButton captureButton;
	private StatusBar statusBar;

	private JTabbedPane tabs;
	private ConfigTab configTab;
	private ScriptEditor scriptEditor;
	private LogTab logTab;

	private Listener controllerListener = new Listener() {
		public void disconnected () {
			setController(null);
		}
	};

	public PG3BUI () {
		super("PG3B");

		Log.set(settings.logLevel);

		if (instance != null) throw new IllegalStateException();
		instance = this;

		initializeLayout();
		initializeEvents();

		controllerPanel.setPG3B(null);
		statusBar.setPG3B(null);
		controllerPanel.setController(null);
		statusBar.setController(null);
		roundTripMenuItem.setEnabled(false);
		clearMenuItem.setEnabled(false);
		calibrateMenuItem.setEnabled(false);

		controllerPanel.setVisible(settings.showController);
		showControllerMenuItem.setSelected(settings.showController);

		statusBar.setMessage("");

		new Thread("InitialConnect") {
			public void run () {
				EventQueue.invokeLater(new Runnable() {
					public void run () {
						boolean reconnectPg3b = settings.pg3bPort != null && settings.pg3bPort.length() > 0;
						if (reconnectPg3b) new LoaderDialog("Connecting to hardware") {
							public void load () throws Exception {
								setMessage("Opening PG3B...");
								try {
									setPg3b(new PG3B(settings.pg3bPort));
								} catch (IOException ex) {
									setPg3b(null);
									if (DEBUG) debug("Unable to reconnect to PG3B.", ex);
								}
							}
						}.start("Pg3bConnect");
					}
				});

				XboxController.getAllControllers(); // Always load all the controllers at startup and not on the EDT.

				boolean reconnectController = settings.controllerName != null && settings.controllerName.length() > 0;
				if (reconnectController) {
					for (Controller controller : XboxController.getAllControllers()) {
						if (settings.controllerPort == controller.getPortNumber()
							&& settings.controllerName.equals(controller.getName())) {
							setController(new XboxController(controller));
							break;
						}
					}
				}
			}
		}.start();
	}

	public void setPg3b (PG3B newPg3b) {
		if (pg3b != null) pg3b.close();

		pg3b = newPg3b;

		EventQueue.invokeLater(new Runnable() {
			public void run () {
				controllerPanel.setPG3B(pg3b);
				statusBar.setPG3B(pg3b);
				configTab.getConfigEditor().setPG3B(pg3b);

				roundTripMenuItem.setEnabled(pg3b != null && controller != null);
				clearMenuItem.setEnabled(roundTripMenuItem.isEnabled());
				calibrateMenuItem.setEnabled(roundTripMenuItem.isEnabled());
			}
		});

		settings.pg3bPort = pg3b == null ? null : pg3b.getPort();
		Settings.save();
	}

	public PG3B getPg3b () {
		return pg3b;
	}

	public void setController (XboxController newController) {
		if (controller != null) controller.removeListener(controllerListener);
		controller = newController;
		if (controller != null) controller.addListener(controllerListener);

		EventQueue.invokeLater(new Runnable() {
			public void run () {
				controllerPanel.setController(controller);
				statusBar.setController(controller);

				roundTripMenuItem.setEnabled(controller != null && pg3b != null);
				clearMenuItem.setEnabled(roundTripMenuItem.isEnabled());
				calibrateMenuItem.setEnabled(roundTripMenuItem.isEnabled());
			}
		});

		settings.controllerName = controller == null ? null : controller.getName();
		settings.controllerPort = controller == null ? 0 : controller.getPort();
		Settings.save();
	}

	public XboxController getController () {
		return controller;
	}

	public XboxControllerPanel getControllerPanel () {
		return controllerPanel;
	}

	public ConfigTab getConfigTab () {
		return configTab;
	}

	public JToggleButton getCaptureButton () {
		return captureButton;
	}

	public ScriptEditor getScriptEditor () {
		return scriptEditor;
	}

	public StatusBar getStatusBar () {
		return statusBar;
	}

	private void initializeEvents () {
		pg3bConnectMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				setPg3b(null);
				new ConnectPG3BDialog(PG3BUI.this).setVisible(true);
			}
		});
		statusBar.setPg3bClickedListener(new Runnable() {
			public void run () {
				pg3bConnectMenuItem.doClick();
			}
		});

		controllerConnectMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				setController(null);
				new ConnectControllerDialog(PG3BUI.this).setVisible(true);
			}
		});
		statusBar.setControllerClickedListener(new Runnable() {
			public void run () {
				controllerConnectMenuItem.doClick();
			}
		});

		showControllerMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				controllerPanel.setVisible(showControllerMenuItem.isSelected());
				settings.showController = showControllerMenuItem.isSelected();
				Settings.save();
			}
		});

		exitMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				exit();
			}
		});

		captureButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				if (activeConfig != null) {
					activeConfig.setActive(false);
					activeConfig = null;
				}
				activeConfig = captureButton.isSelected() ? configTab.getConfigEditor().getSelectedItem() : null;
				if (activeConfig != null) activeConfig.setActive(true);
				statusBar.setConfig(activeConfig);
			}
		});
		statusBar.setConfigClickedListener(new Runnable() {
			public void run () {
				captureButton.doClick();
			}
		});

		Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
			public void eventDispatched (AWTEvent event) {
				// If mouse pressed when a component in an EditorPanel is focused, unfocus the component. This allows users
				// to click focus away from the component to easily save rather than have to click a different component.
				if (event.getID() != MouseEvent.MOUSE_PRESSED) return;
				Component focused = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
				Container editorPanel = SwingUtilities.getAncestorOfClass(EditorPanel.class, focused);
				if (editorPanel != null && event.getSource() != focused) PG3BUI.this.requestFocusInWindow();
			}
		}, AWTEvent.MOUSE_EVENT_MASK);

		roundTripMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				new LoaderDialog("Round trip diagnostic") {
					public void load () throws Exception {
						controllerPanel.setStatus(null);
						final Map<Target, Boolean> status = Diagnostics.roundTrip(pg3b, controller, this);
						controllerPanel.setStatus(status);
						EventQueue.invokeLater(new Runnable() {
							public void run () {
								if (status.values().contains(Boolean.FALSE))
									statusBar.setMessage("Round trip failed.");
								else
									statusBar.setMessage("Round trip successful.");
							}
						});
					}
				}.start("RoundTripTest");
			}
		});

		clearMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				controllerPanel.setStatus(null);
			}
		});

		calibrateMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				new LoaderDialog("Axes Calibration") {
					List<AxisCalibration> results;

					public void load () throws Exception {
						CalibrationResultsFrame.close();

						results = Diagnostics.calibrate(pg3b, controller, this);

						for (AxisCalibration calibration : results)
							if (INFO) info(calibration.getAxis() + " chart:\n" + calibration.getChartURL());

						EventQueue.invokeLater(new Runnable() {
							public void run () {
								if (results.size() != Axis.values().length)
									statusBar.setMessage("Calibration failed.");
								else
									statusBar.setMessage("Calibration successful.");
							}
						});
					}

					public void complete () {
						if (failed() || results.size() != Axis.values().length) return;
						CalibrationResultsFrame frame = new CalibrationResultsFrame(results);
						frame.setLocationRelativeTo(PG3BUI.this);
						frame.setVisible(true);
					}
				}.start("Calibration");
			}
		});
	}

	protected void processWindowEvent (WindowEvent event) {
		if (event.getID() == WindowEvent.WINDOW_CLOSING) {
			exit();
		}
		super.processWindowEvent(event);
	}

	void exit () {
		CalibrationResultsFrame.close();
		if (pg3b != null) pg3b.close();
		dispose();
		System.exit(0);
	}

	private void initializeLayout () {
		UIManager.put("FileChooser.readOnly", Boolean.TRUE);
		ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);

		for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
			if ("Nimbus".equals(info.getName())) {
				try {
					UIManager.setLookAndFeel(info.getClassName());
				} catch (Throwable ignored) {
				}
				break;
			}
		}

		setIconImage(new ImageIcon(getClass().getResource("/pg3b.png")).getImage());
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setSize(720, 800);
		setLocationRelativeTo(null);

		{
			JMenuBar menuBar = new JMenuBar();
			setJMenuBar(menuBar);
			{
				JMenu menu = new JMenu("File");
				menuBar.add(menu);
				{
					pg3bConnectMenuItem = menu.add(new JMenuItem("Connect to PG3B..."));
					controllerConnectMenuItem = menu.add(new JMenuItem("Connect to Controller..."));
					menu.addSeparator();
					exitMenuItem = menu.add(new JMenuItem("Exit"));
				}
			}
			{
				JMenu menu = new JMenu("View");
				menuBar.add(menu);
				{
					showControllerMenuItem = new JCheckBoxMenuItem("Show Controller");
					menu.add(showControllerMenuItem);
				}
			}
			{
				JMenu menu = new JMenu("Diagnostics");
				menuBar.add(menu);
				{
					roundTripMenuItem = new JMenuItem("Round Trip...");
					menu.add(roundTripMenuItem);
				}
				{
					clearMenuItem = new JMenuItem("Clear");
					menu.add(clearMenuItem);
				}
				menu.addSeparator();
				{
					calibrateMenuItem = new JMenuItem("Axes Calibration...");
					menu.add(calibrateMenuItem);
				}
			}
		}

		getContentPane().setLayout(new GridBagLayout());
		{
			captureButton = new JToggleButton("Capture");
			captureButton.setEnabled(false);
			getContentPane().add(
				captureButton,
				new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTH, GridBagConstraints.NONE,
					new Insets(6, 0, 0, 0), 0, 0));
		}
		{
			controllerPanel = new XboxControllerPanel();
			getContentPane().add(
				controllerPanel,
				new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.CENTER, new Insets(0, 0,
					0, 0), 0, 0));
		}
		{
			statusBar = new StatusBar();
			getContentPane().add(
				statusBar,
				new GridBagConstraints(0, 3, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0,
					0), 0, 0));
		}
		{
			tabs = new JTabbedPane();
			getContentPane().add(
				tabs,
				new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0,
					0), 0, 0));
			{
				logTab = new LogTab();
				configTab = new ConfigTab(this);
				tabs.addTab("Configuration", null, configTab, null);
				scriptEditor = new ScriptEditor(this);
				tabs.addTab("Scripts", null, scriptEditor, null);
				tabs.addTab("Log", null, logTab, null);
			}
		}
	}
}
