
package pg3b.ui.swing;

import static com.esotericsoftware.minlog.Log.*;

import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import net.java.games.input.Controller;
import pg3b.PG3B;
import pg3b.XboxController;
import pg3b.XboxController.Listener;
import pg3b.ui.Settings;
import pg3b.util.LoaderDialog;

public class PG3BUI extends JFrame {
	static Settings settings = Settings.get();

	PG3B pg3b;
	XboxController controller;

	JMenuItem pg3bConnectMenuItem, controllerConnectMenuItem, exitMenuItem;
	JCheckBoxMenuItem showControllerMenuItem;

	XboxControllerPanel controllerPanel;
	JToggleButton captureButton;
	StatusBar statusBar;

	JTabbedPane tabs;
	ConfigTab configTab;
	ScriptEditor scriptEditor;
	DiagnosticsTab diagnosticsTab;

	Listener controllerListener = new Listener() {
		public void disconnected () {
			setController(null);
		}
	};

	public PG3BUI () {
		super("PG3B");

		initializeLayout();
		initializeEvents();

		controllerPanel.setPg3b(null);
		diagnosticsTab.setPg3b(null);
		statusBar.setPg3b(null);

		controllerPanel.setController(null);
		diagnosticsTab.setController(null);
		statusBar.setController(null);

		controllerPanel.setVisible(settings.showController);
		showControllerMenuItem.setSelected(settings.showController);

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
				controllerPanel.setPg3b(pg3b);
				diagnosticsTab.setPg3b(pg3b);
				statusBar.setPg3b(pg3b);
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
				diagnosticsTab.setController(controller);
				statusBar.setController(controller);
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
				// BOZO
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
	}

	private void initializeLayout () {
		UIManager.put("FileChooser.readOnly", Boolean.TRUE);

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
					showControllerMenuItem = new JCheckBoxMenuItem("Show controller");
					menu.add(showControllerMenuItem);
				}
			}
		}

		getContentPane().setLayout(new GridBagLayout());
		{
			captureButton = new JToggleButton("Capture");
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
				configTab = new ConfigTab(this);
				tabs.addTab("Configuration", null, configTab, null);
				scriptEditor = new ScriptEditor(this);
				tabs.addTab("Scripts", null, scriptEditor, null);
				diagnosticsTab = new DiagnosticsTab(this);
				tabs.addTab("Diagnostics", null, diagnosticsTab, null);
			}
		}
	}
}
