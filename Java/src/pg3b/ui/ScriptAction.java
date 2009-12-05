
package pg3b.ui;

import java.util.List;

import pg3b.ui.swing.PG3BUI;
import pnuts.lang.Context;
import pnuts.lang.Package;
import pnuts.lang.Pnuts;

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

	public boolean execute (Object payload) {
		Script script = getScript();
		if (script == null) return false;
		Pnuts pnuts = script.getPnuts();
		if (pnuts == null) return false;
		Package pkg = new Package();
		pkg.setConstant("payload", payload);
		pkg.setConstant("pg3b", PG3BUI.instance.getPg3b());
		// BOZO - Finish setting up script context.
		Context context = new Context(pkg);
		// context.setPnutsImpl();
		pnuts.run(context);
		return true;
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
}
