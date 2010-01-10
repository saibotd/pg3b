
package com.esotericsoftware.controller.ui;

import static com.esotericsoftware.minlog.Log.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sound.midi.Instrument;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Patch;
import javax.sound.midi.Synthesizer;

import pnuts.lang.Context;
import pnuts.lang.Package;
import pnuts.lang.PnutsException;
import pnuts.lang.PnutsFunction;

import com.esotericsoftware.controller.input.Keyboard;
import com.esotericsoftware.controller.ui.swing.UI;
import com.esotericsoftware.controller.util.NamedThreadFactory;
import com.esotericsoftware.controller.util.Util;

/**
 * App specific Pnuts functions.
 */
public class Functions {
	static private ArrayList<PnutsFunction> functions = new ArrayList(32);

	static public List<PnutsFunction> getAll () {
		return functions;
	}

	static public abstract class BaseFunction extends PnutsFunction {
		private final int minArgs;
		private final int maxArgs;
		private final String toString;

		public BaseFunction (String name, int minArgs, int maxArgs, String argNames) {
			super(name);
			this.minArgs = minArgs;
			this.maxArgs = maxArgs;
			toString = "function " + name + "(" + argNames + ")";
			functions.add(this);
		}

		protected Object exec (Object[] args, Context context) {
			if (!defined(args.length)) {
				PnutsException ex = new PnutsException("function.notDefined", new Object[] {name, new Integer(args.length)}, context);
				throw new PnutsException(ex.getMessage() + " \nmin/max args: " + minArgs + "/" + maxArgs, context);
			}
			return invoke(args, context);
		}

		abstract protected Object invoke (Object[] args, Context context);

		public boolean defined (int argCount) {
			return argCount >= minArgs && argCount <= maxArgs;
		}

		public String toString () {
			return toString;
		}
	}

	static public BaseFunction sleep = new BaseFunction("sleep", 1, 1, "millis") {
		protected Object invoke (Object[] args, Context context) {
			long sleepMillis = (long)toDouble(args[0]);
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
	};

	static public BaseFunction millis = new BaseFunction("millis", 0, 0, "") {
		protected Object invoke (Object[] args, Context context) {
			return System.currentTimeMillis();
		}
	};

	static public BaseFunction nanos = new BaseFunction("nanos", 0, 0, "") {
		protected Object invoke (Object[] args, Context context) {
			return System.nanoTime();
		}
	};

	static private MidiChannel[] channels;
	static private int currentChannel;
	static private HashMap<String, Instrument> nameToInstrument = new HashMap();
	static {
		try {
			Synthesizer synthesizer = MidiSystem.getSynthesizer();
			synthesizer.open();
			channels = synthesizer.getChannels();
			Instrument[] instruments = synthesizer.getAvailableInstruments();
			synthesizer.loadAllInstruments(synthesizer.getDefaultSoundbank());
			for (int i = 0; i < instruments.length; i++) {
				Instrument instrument = instruments[i];
				if (synthesizer.isSoundbankSupported(instrument.getSoundbank())) {
					synthesizer.loadInstrument(instrument);
					nameToInstrument.put(instrument.getName(), instrument);
				}
			}
		} catch (Exception ex) {
			if (WARN) warn("Midi is not available.", ex);
		}
	}
	static public BaseFunction play = new BaseFunction("play", 0, 4, "[name,] note [,duration] [,pitch]") {
		protected Object invoke (Object[] args, Context context) {
			if (nameToInstrument.size() == 0) return null;

			int note = 60, duration = 500;
			String instrumentName = "Muted Guitar";
			float pitch = 1;

			switch (args.length) {
			case 0:
				System.out.println("Available instruments:");
				for (String name : nameToInstrument.keySet())
					System.out.println(name);
				return null;
			case 1:
				note = (int)toDouble(args[0]);
				break;
			case 2:
				instrumentName = (String)args[0];
				note = (int)toDouble(args[1]);
				break;
			case 3:
				instrumentName = (String)args[0];
				note = (int)toDouble(args[1]);
				duration = (int)toDouble(args[2]);
				break;
			case 4:
				instrumentName = (String)args[0];
				note = (int)toDouble(args[1]);
				duration = (int)toDouble(args[2]);
				pitch = (float)toDouble(args[3]);
				break;
			}

			MidiChannel channel = null;
			while (channel == null) {
				int channelNumber = currentChannel++;
				if (channelNumber == 9) continue; // Skip percussion channel.
				if (channelNumber >= channels.length) channelNumber = currentChannel = 0;
				channel = channels[channelNumber];
			}
			channel.setPitchBend((int)Math.max(0, Math.min(16383, (pitch + 1) * 8192)));

			Instrument instrument = nameToInstrument.get(instrumentName);
			if (instrument == null) instrument = nameToInstrument.values().iterator().next();
			Patch patch = instrument.getPatch();
			int program = patch.getProgram();
			channel.programChange(program);

			channel.noteOn(note, 45);

			final MidiChannel turnOffChannel = channel;
			final int turnOffNote = note;
			Util.timer.schedule(new TimerTask() {
				public void run () {
					turnOffChannel.noteOff(turnOffNote);
				}
			}, duration);

			return null;
		}
	};

	static public BaseFunction beep = new BaseFunction("beep", 0, 1, "on") {
		protected Object invoke (Object[] args, Context context) {
			switch (args.length) {
			case 0:
				play.invoke(new Object[] {"Muted Guitar", 60, 100, 1}, context);
				return null;
			case 1:
				boolean on = toBoolean(args[0]);
				int pitch = on ? 1 : -1;
				play.invoke(new Object[] {"Muted Guitar", 60, 100, pitch}, context);
				return on;
			}
			return null;
		}
	};

	static public BaseFunction getConfig = new BaseFunction("getConfig", 1, 1, "name") {
		protected Object invoke (Object[] args, Context context) {
			String name = (String)args[0];
			for (Config config : UI.instance.getConfigTab().getConfigEditor().getItems())
				if (config.getName().equalsIgnoreCase(name)) return config;
			return null;
		}
	};

	static public BaseFunction setConfig = new BaseFunction("setConfig", 1, 1, "config") {
		protected Object invoke (Object[] args, Context context) {
			Config config;
			if (args[0] instanceof Config)
				config = (Config)args[0];
			else
				config = (Config)getConfig.invoke(args, context);
			if (config == null) throw new IllegalArgumentException("Config not found with name: " + args[0]);
			config.setActive(true);
			return null;
		}
	};

	static public BaseFunction set = new BaseFunction("set", 2, 3, "[packageName,] name, value") {
		protected Object invoke (Object[] args, Context context) {
			String packageName = "__global";
			String valueName = null;
			Object value = null;
			switch (args.length) {
			case 2:
				valueName = (String)args[0];
				value = args[1];
				break;
			case 3:
				packageName = (String)args[0];
				valueName = (String)args[1];
				value = args[2];
				break;
			}
			Package.getPackage(packageName, context).set(valueName.intern(), value);
			return null;
		}
	};

	static public BaseFunction get = new BaseFunction("get", 1, 2, "[packageName,] name") {
		protected Object invoke (Object[] args, Context context) {
			String packageName = "__global";
			String valueName = null;
			switch (args.length) {
			case 1:
				valueName = (String)args[0];
				break;
			case 2:
				packageName = (String)args[0];
				valueName = (String)args[1];
				break;
			}
			return Package.getPackage(packageName, context).get(valueName.intern());
		}
	};

	static private HashMap<String, Long> nameToStartTime = new HashMap();
	static public BaseFunction interval = new BaseFunction("interval", 2, 2, "name, delay") {
		protected Object invoke (Object[] args, Context context) {
			String name = (String)args[0];
			long delay = (long)toDouble(args[1]);

			Long startTime = nameToStartTime.get(name);
			if (startTime == null || System.currentTimeMillis() - startTime >= delay) {
				nameToStartTime.put(name, System.currentTimeMillis());
				return true;
			}
			return false;
		}
	};

	static public BaseFunction toggle = new BaseFunction("toggle", 1, 2, "[packageName,] name") {
		protected Object invoke (Object[] args, Context context) {
			String packageName = "__global";
			String valueName = null;
			switch (args.length) {
			case 1:
				valueName = (String)args[0];
				break;
			case 2:
				packageName = (String)args[0];
				valueName = (String)args[1];
				break;
			}
			valueName = valueName.intern();
			Package pkg = Package.getPackage(packageName, context);
			Object object = pkg.get(valueName);
			object = object != null ? null : Boolean.TRUE;
			pkg.set(valueName, object);
			return object != null;
		}
	};

	static public BaseFunction isCtrlDown = new BaseFunction("isCtrlDown", 0, 0, "") {
		protected Object invoke (Object[] args, Context context) {
			return Keyboard.instance.isCtrlDown();
		}
	};

	static public BaseFunction isAltDown = new BaseFunction("isAltDown", 0, 0, "") {
		protected Object invoke (Object[] args, Context context) {
			return Keyboard.instance.isAltDown();
		}
	};

	static public BaseFunction isShiftDown = new BaseFunction("isShiftDown", 0, 0, "") {
		protected Object invoke (Object[] args, Context context) {
			return Keyboard.instance.isShiftDown();
		}
	};

	static private ExecutorService forkThreadPool = Executors.newFixedThreadPool(24, new NamedThreadFactory("fork", false));
	static public BaseFunction fork = new BaseFunction("fork", 1, 1, "function") {
		protected Object invoke (Object[] args, Context context) {
			final Context functionContext = (Context)context.clone(false, false);
			final PnutsFunction function = (PnutsFunction)args[0];
			forkThreadPool.execute(new Runnable() {
				public void run () {
					function.call(new Object[0], functionContext);
				}
			});
			return null;
		}
	};

	static double toDouble (Object value) {
		if (value instanceof Integer) return (Integer)value;
		if (value instanceof Float) return (Integer)value;
		if (value instanceof Double) return (Double)value;
		if (value instanceof Long) return (Long)value;
		if (value instanceof Short) return (Short)value;
		if (value instanceof Byte) return (Byte)value;
		if (value instanceof Character) return (Character)value;
		if (value instanceof Boolean) return (Boolean)value ? 1 : 0;
		if (value instanceof String) return Double.valueOf((String)value);
		return value != null ? 1 : 0;
	}

	static boolean toBoolean (Object value) {
		if (value instanceof Boolean) return (Boolean)value;
		if (value instanceof Integer) return (Integer)value != 0;
		if (value instanceof Float) return (Integer)value != 0;
		if (value instanceof Double) return (Double)value != 0;
		if (value instanceof Long) return (Long)value != 0;
		if (value instanceof Short) return (Short)value != 0;
		if (value instanceof Byte) return (Byte)value != 0;
		if (value instanceof Character) return (Character)value != 0;
		if (value instanceof String) return ((String)value).equalsIgnoreCase("true");
		return value != null;
	}
}
