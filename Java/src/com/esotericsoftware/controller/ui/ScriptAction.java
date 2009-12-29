
package com.esotericsoftware.controller.ui;

import java.io.PrintWriter;
import java.util.List;

import pnuts.compiler.CompilerPnutsImpl;
import pnuts.lang.Context;
import pnuts.lang.Package;
import pnuts.lang.Pnuts;
import pnuts.lang.PnutsFunction;

import com.esotericsoftware.controller.ui.swing.UI;
import static com.esotericsoftware.minlog.Log.*;

/**
 * An action that runs a Pnuts script when executed.
 */
public class ScriptAction implements Action {
	static public final String CONSTANT_CONFIG = "config".intern();
	static public final String CONSTANT_TRIGGER = "trigger".intern();
	static public final String CONSTANT_ACTION = "action".intern();
	static public final String CONSTANT_PAYLOAD = "payload".intern();

	static public final String FUNCTION_INIT = "init".intern();
	static public final String FUNCTION_ACTIVATE = "activate".intern();
	static public final String FUNCTION_DEACTIVATE = "deactivate".intern();
	static public final String FUNCTION_CONTINUOUS = "continuous".intern();

	private String scriptName;
	private transient Context context;
	private transient boolean wasActive;

	public ScriptAction () {
	}

	public ScriptAction (String scriptName) {
		setScriptName(scriptName);
	}

	public String getScriptName () {
		return scriptName;
	}

	public void setScriptName (String scriptName) {
		this.scriptName = scriptName;
	}

	public void reset (Config config, Trigger trigger) {
		Script script = getScript();
		if (script == null) return;

		Pnuts pnuts = script.getPnuts();
		if (pnuts == null) return;

		context = getContext(config, trigger, this);
		pnuts.run(context);
		execute(pnuts, context, "init", 0);

		if (TRACE) trace("Script action reset: " + this);
	}

	public Object execute (Config config, Trigger trigger) {
		Script script = getScript();
		if (script == null) return null;

		Pnuts pnuts = script.getPnuts();
		if (pnuts == null) return null;

		Object payload = trigger.getPayload();
		if (trigger.isActive()) {
			if (!wasActive) {
				wasActive = true;
				return execute(pnuts, context, "activate", payload);
			} else {
				return execute(pnuts, context, "continuous", payload);
			}
		} else {
			if (wasActive) {
				wasActive = false;
				return execute(pnuts, context, "deactivate", payload);
			}
		}
		return null;
	}

	public boolean isValid () {
		Script script = getScript();
		if (script == null) return false;
		return script.getPnuts() != null;
	}

	public Script getScript () {
		List<Script> scripts = UI.instance.getScriptEditor().getItems();
		for (int i = 0, n = scripts.size(); i < n; i++) {
			Script script = scripts.get(i);
			if (script.getName().equals(scriptName)) return script;
		}
		return null;
	}

	public String toString () {
		return "Script: " + scriptName.toString();
	}

	/**
	 * Returns a script context for executing a Pnuts script.
	 */
	static public Context getContext (Config config, Trigger trigger, Action action) {
		Package pkg = new Package(null);
		pkg.set(CONSTANT_CONFIG, config);
		pkg.set(CONSTANT_TRIGGER, trigger);
		pkg.set(CONSTANT_ACTION, action);
		pkg.set(CONSTANT_PAYLOAD, 0);

		pkg.set("sleep".intern(), Functions.sleep);
		pkg.set("play".intern(), Functions.play);
		pkg.set("beep".intern(), Functions.beep);
		pkg.set("get".intern(), Functions.get);
		pkg.set("set".intern(), Functions.set);
		pkg.set("toggle".intern(), Functions.toggle);
		pkg.set("fork".intern(), Functions.fork);
		pkg.set("isCtrlDown".intern(), Functions.isCtrlDown);
		pkg.set("isAltDown".intern(), Functions.isAltDown);
		pkg.set("isShiftDown".intern(), Functions.isShiftDown);
		pkg.set("millis".intern(), Functions.millis);
		pkg.set("nanos".intern(), Functions.nanos);
		pkg.set("interval".intern(), new Functions.interval());

		Context context = new Context(pkg);
		context.setWriter(new PrintWriter(System.out, true));
		context.setImplementation(new CompilerPnutsImpl());
		context.usePackage("pnuts.lib");
		return context;
	}

	/**
	 * Executes the specified function, if it exists. If the function is "activate" or "deactivate" and does not exist, the entire
	 * script is executed.
	 */
	static public Object execute (Pnuts pnuts, Context context, String functionName, Object payload) {
		context.getCurrentPackage().set(CONSTANT_PAYLOAD, payload);
		Object function = context.resolveSymbol(functionName);
		if (function instanceof PnutsFunction) return ((PnutsFunction)function).call(new Object[0], context);
		if (functionName == FUNCTION_ACTIVATE || functionName == FUNCTION_DEACTIVATE) return pnuts.run(context);
		return null;
	}
}
