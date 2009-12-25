
package com.esotericsoftware.controller.ui;

import java.util.List;

import pnuts.compiler.CompilerPnutsImpl;
import pnuts.lang.Context;
import pnuts.lang.Package;
import pnuts.lang.Pnuts;

import com.esotericsoftware.controller.ui.swing.UI;

/**
 * An action that runs a Pnuts script when executed.
 */
public class ScriptAction implements Action {
	private String scriptName;

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

	public boolean execute (Config config, Trigger trigger, Object payload) {
		Script script = getScript();
		if (script == null) return false;
		Pnuts pnuts = script.getPnuts();
		if (pnuts == null) return false;
		pnuts.run(getContext(config, trigger, payload));
		return true;
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

	static {
		Package pkg = Package.getGlobalPackage();
		pkg.set("getPayload".intern(), Functions.getPayload);
		pkg.set("getAction".intern(), Functions.getAction);
		pkg.set("getTrigger".intern(), Functions.getTrigger);
		pkg.set("getConfig".intern(), Functions.getConfig);
		pkg.set("sleep".intern(), Functions.sleep);
		pkg.set("play".intern(), Functions.play);
		pkg.set("beep".intern(), Functions.beep);
		pkg.set("get".intern(), Functions.get);
		pkg.set("set".intern(), Functions.set);
		pkg.set("toggle".intern(), Functions.toggle);
		pkg.set("fork".intern(), Functions.fork);
		pkg.set("print".intern(), Functions.print);
		pkg.set("isCtrlDown".intern(), Functions.isCtrlDown);
		pkg.set("isAltDown".intern(), Functions.isAltDown);
		pkg.set("isShiftDown".intern(), Functions.isShiftDown);
		pkg.set("millis".intern(), Functions.millis);
		pkg.set("nanos".intern(), Functions.nanos);
	}

	/**
	 * Returns a script context for executing a Pnuts script.
	 */
	public Context getContext (Config config, Trigger trigger, Object payload) {
		Context context = new Context();
		context.set("config", config);
		context.set("trigger", trigger);
		context.set("action", this);
		context.set("payload", payload);
		context.setImplementation(new CompilerPnutsImpl());
		context.usePackage("pnuts.lib");
		return context;
	}
}
