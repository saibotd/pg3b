
package pg3b.ui;

import java.io.FileOutputStream;
import java.io.PrintStream;

import pg3b.ui.swing.PG3BUI;
import pg3b.util.MultiplexOutputStream;

public class Main {
	public static void main (String[] args) throws Exception {
		FileOutputStream logOutput = new FileOutputStream("pg3b.log");
		System.setOut(new PrintStream(new MultiplexOutputStream(System.out, logOutput), true));
		System.setErr(new PrintStream(new MultiplexOutputStream(System.err, logOutput), true));
		new PG3BUI().setVisible(true);
	}
}
