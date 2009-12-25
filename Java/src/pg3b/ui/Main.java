
package pg3b.ui;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.java.games.input.ControllerEnvironment;
import pg3b.ui.swing.PG3BUI;
import pg3b.util.MultiplexOutputStream;

import com.esotericsoftware.minlog.LogHandler;

/**
 * Main class for the PG3B UI.
 */
public class Main {
	public static void main (String[] args) throws Exception {
		FileOutputStream logOutput = new FileOutputStream("pg3b.log");
		System.setOut(new PrintStream(new MultiplexOutputStream(System.out, logOutput), true));
		System.setErr(new PrintStream(new MultiplexOutputStream(System.err, logOutput), true));

		Logger jinputLogger = Logger.getLogger(ControllerEnvironment.class.getName());
		jinputLogger.setUseParentHandlers(false);
		jinputLogger.setLevel(Level.ALL);
		jinputLogger.addHandler(new LogHandler("jinput"));

		new PG3BUI();
	}
}
