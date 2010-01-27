
package com.esotericsoftware.controller.ui;

import java.io.PrintWriter;
import java.util.List;

import pnuts.compiler.CompilerPnutsImpl;
import pnuts.lang.Context;
import pnuts.lang.Package;
import pnuts.lang.Pnuts;
import pnuts.lang.PnutsFunction;

import com.esotericsoftware.controller.ui.swing.UI;

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

	public synchronized void reset (Config config, Trigger trigger) {
		context = null;
		wasActive = false;

		Script script = getScript();
		if (script == null) return;

		Pnuts pnuts = script.getPnuts();
		if (pnuts == null) return;

		context = getContext(config, trigger, this);
		pnuts.run(context);
		execute(pnuts, context, "init", 0);
	}

	public synchronized Object execute (Config config, Trigger trigger, boolean isActive, Object payload) {
		if (context == null) return null;

		Script script = getScript();
		if (script == null) return null;

		Pnuts pnuts = script.getPnuts();
		if (pnuts == null) return null;

		if (isActive) {
			if (!wasActive) {
				wasActive = true;
				return execute(pnuts, context, "activate", payload);
			} else {
				return execute(pnuts, context, "continuous", payload);
			}
		} else {
			if (wasActive) {
				wasActive = false;
				execute(pnuts, context, "continuous", payload);
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

	public String getType () {
		return "Script";
	}

	public int hashCode () {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((scriptName == null) ? 0 : scriptName.hashCode());
		return result;
	}

	public boolean equals (Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		ScriptAction other = (ScriptAction)obj;
		if (scriptName == null) {
			if (other.scriptName != null) return false;
		} else if (!scriptName.equals(other.scriptName)) return false;
		return true;
	}

	public String toString () {
		return scriptName;
	}

	/**
	 * Returns a script context for executing a Pnuts script.
	 */
	static public Context getContext (Config config, Trigger trigger, Action action) {
		Package pkg = new Package(null);
		for (PnutsFunction function : Functions.getAll())
			pkg.set(function.getName(), function);
		pkg.set(CONSTANT_CONFIG, config);
		pkg.set(CONSTANT_TRIGGER, trigger);
		pkg.set(CONSTANT_ACTION, action);
		pkg.set(CONSTANT_PAYLOAD, 0);

		Context context = new Context(pkg);
		context.setWriter(new PrintWriter(System.out, true));
		context.setImplementation(new CompilerPnutsImpl());
		context.usePackage("pnuts.lib");
		return context;
	}

	/**
	 * Executes the specified function, if it exists.
	 */
	static public Object execute (Pnuts pnuts, Context context, String functionName, Object payload) {
		synchronized (context) {
			context.getCurrentPackage().set(CONSTANT_PAYLOAD, payload);
			Object function = context.resolveSymbol(functionName);
			if (function instanceof PnutsFunction) return ((PnutsFunction)function).call(new Object[0], context);
			return null;
		}
	}
}
