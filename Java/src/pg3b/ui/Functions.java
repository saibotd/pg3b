
package pg3b.ui;

import static com.esotericsoftware.minlog.Log.*;

import java.util.HashMap;
import java.util.TimerTask;

import javax.sound.midi.Instrument;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Patch;
import javax.sound.midi.Synthesizer;

import pg3b.util.UI;
import pnuts.lang.Context;
import pnuts.lang.Package;
import pnuts.lang.PnutsFunction;

public class Functions {
	static public class sleep extends PnutsFunction {
		public sleep () {
			super("sleep");
		}

		public boolean defined (int nargs) {
			return nargs == 1;
		}

		protected Object exec (Object[] args, Context context) {
			if (args.length != 1) {
				undefined(args, context);
				return null;
			}
			long sleepMillis = (Integer)args[0];
			long endTime = System.nanoTime() + sleepMillis * 1000000;
			if (sleepMillis > 100) {
				try {
					Thread.sleep(sleepMillis - 100);
				} catch (InterruptedException ex) {
				}
			}
			while (endTime > System.nanoTime()) {
			}
			return null;
		}

		public String toString () {
			return "function sleep(millis)";
		}
	}

	static public class play extends PnutsFunction {
		static private MidiChannel[] channels;
		static private int currentChannel;
		static private HashMap<String, Instrument> nameToInstrument = new HashMap();
		static {
			try {
				Synthesizer synthesizer = MidiSystem.getSynthesizer();
				synthesizer.open();
				channels = synthesizer.getChannels();
				Instrument[] instruments = synthesizer.getDefaultSoundbank().getInstruments();
				synthesizer.loadAllInstruments(synthesizer.getDefaultSoundbank());
				for (int i = 0; i < instruments.length; i++)
					nameToInstrument.put(instruments[i].getName(), instruments[i]);
			} catch (MidiUnavailableException ex) {
				if (WARN) warn("Midi is not available.", ex);
			}
		}

		public play () {
			super("play");
		}

		play (String name) {
			super(name);
		}

		public boolean defined (int nargs) {
			return nargs <= 2;
		}

		protected Object exec (Object[] args, Context context) {
			if (nameToInstrument.size() == 0) return null;

			int note;
			String instrumentName;
			int duration;

			switch (args.length) {
			case 0:
				for (String name : nameToInstrument.keySet())
					System.out.println(name);
				return null;
			case 1:
				instrumentName = "Muted Guitar";
				note = (Integer)args[0];
				duration = 500;
				break;
			case 2:
				instrumentName = (String)args[0];
				note = (Integer)args[1];
				duration = 500;
				break;
			case 3:
				instrumentName = (String)args[0];
				note = (Integer)args[1];
				duration = (Integer)args[2];
				break;
			default:
				undefined(args, context);
				return null;
			}

			MidiChannel channel = null;
			while (channel == null) {
				int channelNumber = currentChannel++;
				if (channelNumber == 9) continue; // Skip percussion channel.
				if (channelNumber >= channels.length) channelNumber = currentChannel = 0;
				channel = channels[channelNumber];
			}

			Instrument instrument = nameToInstrument.get(instrumentName);
			if (instrument == null) instrument = nameToInstrument.values().iterator().next();
			Patch patch = instrument.getPatch();
			int program = patch.getProgram();
			channel.programChange(program);

			channel.noteOn(note, 50);

			final MidiChannel turnOffChannel = channel;
			final int turnOffNote = note;
			UI.timer.schedule(new TimerTask() {
				public void run () {
					turnOffChannel.noteOff(turnOffNote);
				}
			}, duration);

			return null;
		}

		public String toString () {
			return "function sleep([name, ] note [, duration])";
		}
	}

	static public class beep extends play {
		public beep () {
			super("beep");
		}

		public boolean defined (int nargs) {
			return nargs == 0;
		}

		protected Object exec (Object[] args, Context context) {
			if (args.length != 0) {
				undefined(args, context);
				return null;
			}
			super.exec(new Object[] {"Muted Guitar", 68, 100}, context);
			return null;
		}

		public String toString () {
			return "function beep()";
		}
	}

	static public class set extends PnutsFunction {
		public set () {
			super("set");
		}

		public boolean defined (int nargs) {
			return nargs == 3;
		}

		protected Object exec (Object[] args, Context context) {
			if (args.length != 3) {
				undefined(args, context);
				return null;
			}
			String packageName = (String)args[0];
			String valueName = (String)args[1];
			Object value = args[2];

			Package.getPackage(packageName).set(valueName.intern(), value);

			return null;
		}

		public String toString () {
			return "function set(packageName, name, value)";
		}
	}

	static public class get extends PnutsFunction {
		public get () {
			super("get");
		}

		public boolean defined (int nargs) {
			return nargs == 2;
		}

		protected Object exec (Object[] args, Context context) {
			if (args.length != 2) {
				undefined(args, context);
				return null;
			}
			String packageName = (String)args[0];
			String valueName = (String)args[1];

			return Package.getPackage(packageName).get(valueName.intern());
		}

		public String toString () {
			return "function get(packageName, name)";
		}
	}

	static public class getPayload extends PnutsFunction {
		public getPayload () {
			super("getPayload");
		}

		public boolean defined (int nargs) {
			return nargs == 0;
		}

		protected Object exec (Object[] args, Context context) {
			if (args.length != 0) {
				undefined(args, context);
				return null;
			}
			return context.get("payload");
		}

		public String toString () {
			return "function getPayload()";
		}
	}

	static public class print extends PnutsFunction {
		public print () {
			super("print");
		}

		public boolean defined (int nargs) {
			return nargs == 1;
		}

		protected Object exec (Object[] args, Context context) {
			if (args.length != 1) {
				undefined(args, context);
				return null;
			}
			System.out.println(args[0]);
			return null;
		}

		public String toString () {
			return "function print(text)";
		}
	}
}
