
package pg3b.ui.util;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;

public class Sound {
	static private final Map<String, Clip> nameToSound = new HashMap();

	static synchronized public void register (String name) {
		if (nameToSound.containsKey(name)) return;
		try {
			InputStream wavStream = Sound.class.getResourceAsStream("/" + name + ".wav");
			if (wavStream == null) throw new RuntimeException("Sound file not found: " + name + ".wav");
			AudioInputStream audioInput = AudioSystem.getAudioInputStream(wavStream);
			try {
				Clip clip = (Clip)AudioSystem.getLine(new DataLine.Info(Clip.class, audioInput.getFormat()));
				clip.open(audioInput);
				nameToSound.put(name, clip);
			} finally {
				audioInput.close();
			}
		} catch (Exception ex) {
			throw new RuntimeException("Error loading sound: " + name, ex);
		}
	}

	static private Clip getClip (String name) {
		Clip clip = nameToSound.get(name);
		if (clip == null) throw new RuntimeException("Unregistered sound: " + name);
		return clip;
	}

	static public void play (String name) {
		Clip clip = getClip(name);
		clip.stop();
		clip.setFramePosition(0);
		clip.start();
	}

	static public void stop (String name) {
		getClip(name).stop();
	}

	static public void loop (String name) {
		getClip(name).loop(Clip.LOOP_CONTINUOUSLY);
	}
}
