
package com.esotericsoftware.controller.ui.swing;

import static com.esotericsoftware.minlog.Log.*;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
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
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import pnuts.lang.Package;

import com.esotericsoftware.controller.device.Axis;
import com.esotericsoftware.controller.device.Button;
import com.esotericsoftware.controller.device.Deadzone;
import com.esotericsoftware.controller.device.Device;
import com.esotericsoftware.controller.device.Stick;
import com.esotericsoftware.controller.device.Target;
import com.esotericsoftware.controller.input.Input;
import com.esotericsoftware.controller.input.Keyboard;
import com.esotericsoftware.controller.input.Mouse;
import com.esotericsoftware.controller.input.XboxController;
import com.esotericsoftware.controller.input.XboxController.Listener;
import com.esotericsoftware.controller.pg3b.ControllerType;
import com.esotericsoftware.controller.pg3b.PG3B;
import com.esotericsoftware.controller.pg3b.PG3BConfig;
import com.esotericsoftware.controller.ui.Config;
import com.esotericsoftware.controller.ui.Diagnostics;
import com.esotericsoftware.controller.ui.InputTrigger;
import com.esotericsoftware.controller.ui.Settings;
import com.esotericsoftware.controller.ui.Trigger;
import com.esotericsoftware.controller.util.LoaderDialog;
import com.esotericsoftware.controller.util.Util;
import com.esotericsoftware.controller.xim.XIM;
import com.esotericsoftware.minlog.Log;

public class UI extends JFrame {
	static public final String version = "0.1.3";
	static public UI instance;

	static private Settings settings = Settings.get();

	private Device device;
	private XboxController controller;
	private Config activeConfig;

	private JMenuItem pg3bConnectMenuItem, ximConnectMenuItem, disconnectMenuItem, controllerConnectMenuItem, exitMenuItem;
	private JCheckBoxMenuItem showControllerMenuItem, showLogMenuItem, debugEnabledMenuItem, calibrationEnabledMenuItem;
	private JMenuItem roundTripMenuItem, clearMenuItem, calibrateMenuItem, setControllerTypeMenuItem;

	private XboxControllerPanel controllerPanel;
	private StatusBar statusBar;

	private JTabbedPane tabs;
	private ConfigTab configTab;
	private ScriptEditor scriptEditor;
	private LogPanel logPanel;
	private JSplitPane splitPane;

	private boolean disableKeyboard = false;

	private Listener controllerListener = new Listener() {
		public void disconnected () {
			setController(null);
		}
	};

	public UI () {
		super("Controller v" + version);

		Log.set(settings.logLevel);

		if (instance != null) throw new IllegalStateException();
		instance = this;

		Package pkg = Package.getGlobalPackage();
		pkg.set("ui".intern(), this);
		pkg.set("device".intern(), null);
		pkg.set("controller".intern(), null);
		pkg.set("Axis".intern(), Axis.class);
		pkg.set("Button".intern(), Button.class);
		pkg.set("ControllerType".intern(), ControllerType.class);
		pkg.set("Deadzone".intern(), Deadzone.class);
		pkg.set("Device".intern(), PG3B.class);
		pkg.set("Stick".intern(), Stick.class);

		initializeLayout();
		initializeEvents();

		if (INFO) info("Controller v" + version);

		controllerPanel.setDevice(null);
		statusBar.setDevice(null);
		controllerPanel.setController(null);
		statusBar.setController(null);

		roundTripMenuItem.setEnabled(false);
		disconnectMenuItem.setEnabled(false);
		clearMenuItem.setEnabled(false);
		calibrateMenuItem.setEnabled(false);
		setControllerTypeMenuItem.setEnabled(false);
		debugEnabledMenuItem.setEnabled(false);
		calibrationEnabledMenuItem.setEnabled(false);

		controllerPanel.setVisible(settings.showController);
		showControllerMenuItem.setSelected(settings.showController);
		showLog(settings.showLog);
		showLogMenuItem.setSelected(settings.showLog);

		statusBar.setMessage("");

		new Thread("InitialConnect") {
			public void run () {
				EventQueue.invokeLater(new Runnable() {
					public void run () {
						boolean reconnectPg3b = settings.pg3bPort != null && settings.pg3bPort.length() > 0;
						if (reconnectPg3b) new LoaderDialog("Connecting to PG3B") {
							public void load () throws Exception {
								setMessage("Opening PG3B...");
								try {
									setDevice(new PG3B(settings.pg3bPort));
								} catch (IOException ex) {
									setDevice(null);
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

		setSize(settings.windowSize.width, settings.windowSize.height);
		if (settings.windowSize.x == -1 || settings.windowSize.y == -1)
			setLocationRelativeTo(null);
		else
			setLocation(settings.windowSize.x, settings.windowSize.y);
		setExtendedState(settings.windowState);

		setVisible(true);

		int range = splitPane.getMaximumDividerLocation() - splitPane.getMinimumDividerLocation() - splitPane.getDividerSize();
		splitPane.setDividerLocation(splitPane.getMinimumDividerLocation() + (int)(range * settings.dividerLocation));
	}

	public void setDevice (Device newDevice) {
		if (device != null) device.close();

		device = newDevice;
		Package.getGlobalPackage().set("device".intern(), device);

		EventQueue.invokeLater(new Runnable() {
			public void run () {
				controllerPanel.setDevice(device);
				statusBar.setDevice(device);
				configTab.getConfigEditor().setDevice(device);

				disconnectMenuItem.setEnabled(device != null);
				roundTripMenuItem.setEnabled(device != null && controller != null);

				boolean isPG3B = device instanceof PG3B;
				calibrateMenuItem.setEnabled(isPG3B && controller != null);
				setControllerTypeMenuItem.setEnabled(isPG3B);
				debugEnabledMenuItem.setEnabled(isPG3B);
				calibrationEnabledMenuItem.setEnabled(isPG3B);
			}
		});

		settings.pg3bPort = device instanceof PG3B ? ((PG3B)device).getPort() : null;
		Settings.save();
	}

	public Device getDevice () {
		return device;
	}

	public void setController (XboxController newController) {
		if (controller != null) controller.removeListener(controllerListener);
		controller = newController;
		Package.getGlobalPackage().set("controller".intern(), controller);
		if (controller != null) controller.addListener(controllerListener);

		EventQueue.invokeLater(new Runnable() {
			public void run () {
				controllerPanel.setController(controller);
				statusBar.setController(controller);

				roundTripMenuItem.setEnabled(controller != null && device != null);
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

	public JTabbedPane getTabs () {
		return tabs;
	}

	private void initializeEvents () {
		disconnectMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				setDevice(null);
			}
		});

		ximConnectMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				setDevice(null);
				try {
					setDevice(new XIM());
					statusBar.setMessage("XIM connected.");
				} catch (IOException ex) {
					if (Log.ERROR) error("Error connecting to XIM.", ex);
					statusBar.setMessage("XIM connection failed.");
					Util.errorDialog(UI.this, "Connect Error", "An error occurred while attempting to connect to the XIM.");
				}
			}
		});

		pg3bConnectMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				setDevice(null);
				new ConnectPG3BDialog(UI.this).setVisible(true);
			}
		});

		statusBar.setDeviceClickedListener(new Runnable() {
			public void run () {
				pg3bConnectMenuItem.doClick();
			}
		});

		controllerConnectMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				setController(null);
				new ConnectControllerDialog(UI.this).setVisible(true);
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

		showLogMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				showLog(showLogMenuItem.isSelected());
				settings.showLog = showLogMenuItem.isSelected();
				Settings.save();
			}
		});

		exitMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				exit();
			}
		});

		statusBar.setConfigClickedListener(new Runnable() {
			public void run () {
				configTab.getConfigEditor().getActivateButton().doClick();
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
					if (event.isControlDown() && event.getKeyCode() == KeyEvent.VK_F4) setActivated(false);
					event.consume();
				}
				return false;
			}
		});

		roundTripMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				new LoaderDialog("Round Trip") {
					private Map<Target, Boolean> status;

					public void load () throws Exception {
						controllerPanel.setStatus(null);
						status = Diagnostics.roundTrip(device, controller, this);
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
				new PG3BCalibrationDialog(UI.this, (PG3B)device, controller);
			}
		});

		setControllerTypeMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				PG3BConfig config = ((PG3B)device).getConfig();
				int result = JOptionPane.showOptionDialog(UI.this, "Select the type of controller to which the PG3B is wired:",
					"PG3B Controller Type", JOptionPane.OK_OPTION, JOptionPane.PLAIN_MESSAGE, null, ControllerType.values(), config
						.getControllerType());
				if (result == JOptionPane.CLOSED_OPTION) return;
				try {
					config.setControllerType(ControllerType.values()[result]);
					config.save();
					device.reset();
				} catch (IOException ex) {
					if (Log.ERROR) error("Error setting PG3B controller type.", ex);
				}
			}
		});

		debugEnabledMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				try {
					((PG3B)device).setDebugEnabled(debugEnabledMenuItem.isSelected());
				} catch (IOException ex) {
					if (Log.ERROR) error("Error setting PG3B calibration.", ex);
				}
			}
		});

		calibrationEnabledMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				try {
					((PG3B)device).setCalibrationEnabled(calibrationEnabledMenuItem.isSelected());
					device.reset();
				} catch (IOException ex) {
					if (Log.ERROR) error("Error setting PG3B calibration.", ex);
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

		addComponentListener(new ComponentAdapter() {
			public void componentResized (ComponentEvent event) {
				saveFrameState();
			}
		});

		splitPane.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange (PropertyChangeEvent event) {
				if (!event.getPropertyName().equals(JSplitPane.DIVIDER_LOCATION_PROPERTY)) return;
				saveFrameState();
			}
		});
	}

	public void setActivated (boolean enabled) {
		configTab.getConfigEditor().getActivateButton().setSelected(enabled);
		if (enabled && activeConfig != null) return;
		if (!enabled && activeConfig == null) return;

		if (activeConfig != null) activeConfig.setActive(false);

		try {
			if (device != null) device.reset();
		} catch (IOException ex) {
			if (WARN) warn("Unable to reset device.", ex);
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
					Mouse.instance.grab(UI.this);
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
		if (device != null) device.close();
		dispose();
		System.exit(0);
	}

	private void showLog (boolean showLog) {
		getContentPane().remove(splitPane);
		getContentPane().remove(tabs);
		Component component;
		if (showLog) {
			component = splitPane;
			splitPane.setTopComponent(tabs);
		} else {
			component = tabs;
		}
		getContentPane().add(
			component,
			new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0),
				0, 0));
		getContentPane().validate();
		splitPane.setDividerLocation(settings.dividerLocation);
	}

	private void saveFrameState () {
		if (!isVisible()) return;

		int extendedState = getExtendedState();
		if (extendedState == JFrame.ICONIFIED) extendedState = JFrame.NORMAL;

		Rectangle windowSize = settings.windowSize;
		if ((extendedState & JFrame.MAXIMIZED_BOTH) == 0) windowSize = getBounds();

		float dividerLocation = settings.dividerLocation;
		if (splitPane.getParent() != null) {
			dividerLocation = (splitPane.getDividerLocation() - splitPane.getMinimumDividerLocation())
				/ (float)(splitPane.getMaximumDividerLocation() - splitPane.getMinimumDividerLocation());
			if (dividerLocation < 0 || dividerLocation > 1) dividerLocation = 0.66f;
		}

		if (settings.windowState == extendedState && settings.windowSize.equals(windowSize)
			&& settings.dividerLocation == dividerLocation) return;
		settings.windowState = extendedState;
		settings.windowSize = windowSize;
		settings.dividerLocation = dividerLocation;
		Settings.save();
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

		setIconImage(new ImageIcon(getClass().getResource("/app.png")).getImage());
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		{
			JMenuBar menuBar = new JMenuBar();
			setJMenuBar(menuBar);
			{
				JMenu menu = new JMenu("Device");
				menuBar.add(menu);
				{
					pg3bConnectMenuItem = menu.add(new JMenuItem("Connect to PG3B..."));
					ximConnectMenuItem = menu.add(new JMenuItem("Connect to XIM..."));
					disconnectMenuItem = menu.add(new JMenuItem("Disconnect"));
				}
				menu.addSeparator();
				{
					controllerConnectMenuItem = menu.add(new JMenuItem("Connect to Controller..."));
				}
				menu.addSeparator();
				{
					setControllerTypeMenuItem = new JMenuItem("PG3B Controller Type...");
					menu.add(setControllerTypeMenuItem);
				}
				{
					calibrateMenuItem = new JMenuItem("PG3B Axes Calibration...");
					menu.add(calibrateMenuItem);
				}
				menu.addSeparator();
				{
					exitMenuItem = new JMenuItem("Exit");
					menu.add(exitMenuItem);
				}
			}
			{
				JMenu menu = new JMenu("View");
				menuBar.add(menu);
				{
					showControllerMenuItem = new JCheckBoxMenuItem("Show Controller");
					menu.add(showControllerMenuItem);
				}
				{
					showLogMenuItem = new JCheckBoxMenuItem("Show Log");
					menu.add(showLogMenuItem);
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
					debugEnabledMenuItem = new JCheckBoxMenuItem("PG3B Debug");
					menu.add(debugEnabledMenuItem);
				}
				{
					calibrationEnabledMenuItem = new JCheckBoxMenuItem("PG3B Calibration");
					menu.add(calibrationEnabledMenuItem);
					calibrationEnabledMenuItem.setSelected(true);
				}
			}
		}

		getContentPane().setLayout(new GridBagLayout());
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
			splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
			splitPane.setBorder(BorderFactory.createEmptyBorder());
			{
				splitPane.setTopComponent(tabs = new JTabbedPane());
				{
					configTab = new ConfigTab(this);
					tabs.addTab("Configuration", null, configTab, null);
					scriptEditor = new ScriptEditor(this);
					tabs.addTab("Scripts", null, scriptEditor, null);
				}
			}
			{
				splitPane.setBottomComponent(logPanel = new LogPanel());
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
					JLabel label = new JLabel("Press ctrl+F4 to deactivate.");
					panel.add(label);
				}
			}
		}
	}
}
