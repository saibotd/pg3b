
package pg3b.ui.swing;

import static com.esotericsoftware.minlog.Log.*;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import pg3b.ControllerType;
import pg3b.PG3B;
import pg3b.PG3BConfig;
import pg3b.Target;
import pg3b.input.Input;
import pg3b.input.Keyboard;
import pg3b.input.Mouse;
import pg3b.input.XboxController;
import pg3b.input.XboxController.Listener;
import pg3b.ui.Config;
import pg3b.ui.Diagnostics;
import pg3b.ui.InputTrigger;
import pg3b.ui.Settings;
import pg3b.ui.Trigger;
import pg3b.util.LoaderDialog;
import pnuts.lang.Package;

import com.esotericsoftware.minlog.Log;

public class PG3BUI extends JFrame {
	static public PG3BUI instance;

	static private Settings settings = Settings.get();

	private PG3B pg3b;
	private XboxController controller;
	private Config activeConfig;

	private JMenuItem pg3bConnectMenuItem, controllerConnectMenuItem, exitMenuItem;
	private JCheckBoxMenuItem showControllerMenuItem;
	private JMenuItem roundTripMenuItem, clearMenuItem, calibrateMenuItem, setControllerTypeMenuItem;

	private XboxControllerPanel controllerPanel;
	private JToggleButton captureButton;
	private StatusBar statusBar;

	private JTabbedPane tabs;
	private ConfigTab configTab;
	private ScriptEditor scriptEditor;
	private LogTab logTab;

	private boolean disableKeyboard = false;

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
		setControllerTypeMenuItem.setEnabled(false);

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
									setPG3B(new PG3B(settings.pg3bPort));
								} catch (IOException ex) {
									setPG3B(null);
									if (DEBUG) debug("Unable to reconnect to PG3B.", ex);
								}
							}
						}.start("Pg3bConnect");
					}
				});

				boolean reconnectController = settings.controllerName != null && settings.controllerName.length() > 0;
				if (reconnectController) {
					for (XboxController controller : XboxController.getAll()) {
						if (settings.controllerPort == controller.getPort() && settings.controllerName.equals(controller.getName())) {
							setController(controller);
							break;
						}
					}
				}
			}
		}.start();
	}

	public void setPG3B (PG3B newPg3b) {
		if (pg3b != null) pg3b.close();

		pg3b = newPg3b;
		Package.getGlobalPackage().set("pg3b".intern(), pg3b);

		EventQueue.invokeLater(new Runnable() {
			public void run () {
				controllerPanel.setPG3B(pg3b);
				statusBar.setPG3B(pg3b);
				configTab.getConfigEditor().setPG3B(pg3b);

				roundTripMenuItem.setEnabled(pg3b != null && controller != null);
				calibrateMenuItem.setEnabled(roundTripMenuItem.isEnabled());
				setControllerTypeMenuItem.setEnabled(pg3b != null);
			}
		});

		settings.pg3bPort = pg3b == null ? null : pg3b.getPort();
		Settings.save();
	}

	public PG3B getPG3B () {
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

	public ScriptEditor getScriptEditor () {
		return scriptEditor;
	}

	public StatusBar getStatusBar () {
		return statusBar;
	}

	public JToggleButton getCaptureButton () {
		return captureButton;
	}

	private void initializeEvents () {
		pg3bConnectMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				setPG3B(null);
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
				setCapture(captureButton.isSelected());
			}
		});
		statusBar.setConfigClickedListener(new Runnable() {
			public void run () {
				captureButton.doClick();
			}
		});

		Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
			public void eventDispatched (AWTEvent event) {
				// If mouse pressed when a component in an EditorPanel is focused, save the editor.
				if (event.getID() != MouseEvent.MOUSE_PRESSED) return;
				Component focused = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
				EditorPanel editorPanel = (EditorPanel)SwingUtilities.getAncestorOfClass(EditorPanel.class, focused);
				if (editorPanel != null && event.getSource() != focused) editorPanel.saveItem(false);
			}
		}, AWTEvent.MOUSE_EVENT_MASK);

		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
			public boolean dispatchKeyEvent (KeyEvent event) {
				if (disableKeyboard) {
					if (event.isControlDown() && event.getKeyCode() == KeyEvent.VK_F4) setCapture(false);
					event.consume();
				}
				return false;
			}
		});

		roundTripMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				new LoaderDialog("Round trip diagnostic") {
					private Map<Target, Boolean> status;

					public void load () throws Exception {
						controllerPanel.setStatus(null);
						status = Diagnostics.roundTrip(pg3b, controller, this);
						controllerPanel.setStatus(status);
						clearMenuItem.setEnabled(true);
					}

					public void complete () {
						if (status.values().contains(Boolean.FALSE))
							statusBar.setMessage("Round trip unsuccessful.");
						else
							statusBar.setMessage("Round trip successful.");
					}
				}.start("RoundTripTest");
			}
		});

		clearMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				controllerPanel.setStatus(null);
				clearMenuItem.setEnabled(false);
			}
		});

		calibrateMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				new CalibrationDialog(PG3BUI.this, pg3b, controller);
			}
		});

		setControllerTypeMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				PG3BConfig config = pg3b.getConfig();
				int result = JOptionPane.showOptionDialog(PG3BUI.this, "Select the type of controller to which the PG3B is wired:",
					"PG3B Controller Type", JOptionPane.OK_OPTION, JOptionPane.PLAIN_MESSAGE, null, ControllerType.values(), config
						.getControllerType());
				if (result == JOptionPane.CLOSED_OPTION) return;
				try {
					config.setControllerType(ControllerType.values()[result]);
					config.save();
				} catch (IOException ex) {
					if (Log.ERROR) error("Error setting PG3B controller type.", ex);
				}
			}
		});

		addWindowFocusListener(new WindowFocusListener() {
			public void windowLostFocus (WindowEvent event) {
				// Prevent buttons from being stuck down when the app loses focus.
				Mouse.instance.reset();
				Keyboard.instance.reset();
			}

			public void windowGainedFocus (WindowEvent event) {
			}
		});
	}

	public void setCapture (boolean enabled) {
		captureButton.setSelected(enabled);
		if (enabled && activeConfig != null) return;
		if (!enabled && activeConfig == null) return;

		if (activeConfig != null) activeConfig.setActive(false);

		try {
			if (pg3b != null) pg3b.reset();
		} catch (IOException ex) {
			if (WARN) warn("Unable to reset PG3B.", ex);
		}

		activeConfig = enabled ? configTab.getConfigEditor().getSelectedItem() : null;
		statusBar.setConfig(activeConfig);

		if (activeConfig == null) {
			getGlassPane().setVisible(false);
			disableKeyboard = false;
			Mouse.instance.release();
			return;
		}

		activeConfig.setActive(true);

		// Disable mouse and/or keyboard.
		for (Trigger trigger : activeConfig.getTriggers()) {
			if (trigger instanceof InputTrigger) {
				Input input = ((InputTrigger)trigger).getInput();
				if (input instanceof Mouse.MouseInput || input instanceof Keyboard.KeyboardInput) {
					getGlassPane().setVisible(true);
					Mouse.instance.grab(PG3BUI.this);
					disableKeyboard = true;
					return;
				}
			}
		}
	}

	protected void processWindowEvent (WindowEvent event) {
		if (event.getID() == WindowEvent.WINDOW_CLOSING) {
			exit();
		}
		super.processWindowEvent(event);
	}

	void exit () {
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
				JMenu menu = new JMenu("Setup");
				menuBar.add(menu);
				{
					setControllerTypeMenuItem = new JMenuItem("PG3B Controller Type...");
					menu.add(setControllerTypeMenuItem);
				}
				{
					calibrateMenuItem = new JMenuItem("Axes Calibration...");
					menu.add(calibrateMenuItem);
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
		{
			JPanel glassPane = new JPanel(new GridBagLayout()) {
				{
					enableEvents(AWTEvent.MOUSE_WHEEL_EVENT_MASK);
				}

				public void paintComponent (Graphics g) {
					g.setColor(new Color(0, 0, 0, 70));
					g.fillRect(0, 0, getWidth(), getHeight());
				}
			};
			glassPane.addMouseListener(new MouseAdapter() {
				public void mousePressed (MouseEvent event) {
					event.consume();
				}
			});
			glassPane.setOpaque(false);
			glassPane.setCursor(getToolkit().createCustomCursor(new BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB),
				new Point(0, 0), "null"));
			setGlassPane(glassPane);
			{
				JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 6));
				panel.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.black));
				glassPane.add(panel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
					new Insets(0, 0, 0, 0), 0, 0));
				{
					JLabel label = new JLabel("Press ctrl+F4 to stop capture.");
					panel.add(label);
				}
			}
		}
	}
}
