
package com.esotericsoftware.controller.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

public class WindowsRegistry {
	static private final boolean isWindows = System.getProperty("os.name").toLowerCase().contains("windows");

	static public String get (String path, String name) {
		if (!isWindows) return null;
		try {
			Process process = Runtime.getRuntime().exec("reg query \"" + path.replace('/', '\\') + "\" /v \"" + name + "\"");
			final InputStream input = process.getInputStream();
			final StringWriter buffer = new StringWriter();
			Thread thread = new Thread("RegistryReader") {
				public void run () {
					try {
						while (true) {
							int b = input.read();
							if (b == -1) break;
							buffer.write(b);
						}
					} catch (IOException ignored) {
					}
				}
			};
			thread.start();
			process.waitFor();
			thread.join();
			String result = buffer.toString();
			String type = "REG_DWORD";
			int index = result.indexOf(type);
			if (index == -1) {
				type = "REG_SZ";
				index = result.indexOf(type);
				if (index == -1) return null;
			}
			return result.substring(index + type.length()).trim();
		} catch (Exception ex) {
			return null;
		}
	}
}
