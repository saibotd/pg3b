
package pg3b.ui;

import java.util.List;

import pg3b.ui.swing.PG3BUI;
import pnuts.compiler.CompilerPnutsImpl;
import pnuts.lang.Context;
import pnuts.lang.Package;
import pnuts.lang.Pnuts;

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

	public void execute (Trigger trigger, Object payload) {
		Script script = getScript();
		if (script == null) return;
		Pnuts pnuts = script.getPnuts();
		if (pnuts == null) return;
		pnuts.run(getContext(trigger, payload));
	}

	public boolean isValid () {
		Script script = getScript();
		if (script == null) return false;
		return script.getPnuts() != null;
	}

	private Script getScript () {
		List<Script> scripts = PG3BUI.instance.getScriptEditor().getItems();
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
		pkg.set("getPayload".intern(), new Functions.getPayload());
		pkg.set("getAction".intern(), new Functions.getAction());
		pkg.set("getTrigger".intern(), new Functions.getTrigger());
		pkg.set("pg3bui".intern(), PG3BUI.instance);
		pkg.set("sleep".intern(), new Functions.sleep());
		pkg.set("play".intern(), new Functions.play());
		pkg.set("beep".intern(), new Functions.beep());
		pkg.set("get".intern(), new Functions.get());
		pkg.set("set".intern(), new Functions.set());
		pkg.set("toggle".intern(), new Functions.toggle());
		pkg.set("print".intern(), new Functions.print());
		pkg.set("isCtrlDown".intern(), new Functions.isCtrlDown());
		pkg.set("isAltDown".intern(), new Functions.isAltDown());
		pkg.set("isShiftDown".intern(), new Functions.isShiftDown());
	}

	/**
	 * Returns a script context for executing a Pnuts script.
	 */
	public Context getContext (Trigger trigger, Object payload) {
		Context context = new Context();
		context.set("trigger", trigger);
		context.set("action", this);
		context.set("payload", payload);
		context.setImplementation(new CompilerPnutsImpl());
		context.usePackage("pnuts.lib");
		return context;
	}
}
